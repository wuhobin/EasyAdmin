package com.nexora.monitor.biz;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexora.monitor.constants.ServerConstants;
import com.nexora.monitor.domain.form.ManagedServerForm;
import com.nexora.monitor.domain.form.ManagedServerQueryForm;
import com.nexora.monitor.domain.form.ServerPasswordForm;
import com.nexora.monitor.domain.form.TerminalTicketForm;
import com.nexora.monitor.domain.query.ManagedServerQuery;
import com.nexora.monitor.entity.ManagedServer;
import com.nexora.monitor.infrastructure.serverssh.SshConnectionService;
import com.nexora.monitor.infrastructure.serverssh.SshTargetValidator;
import com.nexora.monitor.infrastructure.serverssh.SshTerminalSessionManager;
import com.nexora.monitor.infrastructure.serverssh.TerminalTicketStore;
import com.nexora.monitor.service.ManagedServerService;
import com.nexora.security.PlatformCredentialCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.net.InetAddress;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedServerBizServiceTest {

    @Test
    void alwaysScopesListsToTheCurrentOwner() {
        Fixture fixture = new Fixture();
        when(fixture.serverService.listOwned(any(), any()))
                .thenReturn(new Page<ManagedServer>(1, 10));

        withOwner(7, () -> fixture.service.list(
                new ManagedServerQueryForm(), new PageParam()));

        ArgumentCaptor<ManagedServerQuery> query =
                ArgumentCaptor.forClass(ManagedServerQuery.class);
        ArgumentCaptor<PageParam> page = ArgumentCaptor.forClass(PageParam.class);
        verify(fixture.serverService).listOwned(query.capture(), page.capture());
        assertThat(query.getValue().getOwnerId()).isEqualTo(7);
        assertThat(page.getValue().getOrderBy()).isEqualTo("sort asc,id desc");
    }

    @Test
    void keepsCallerProvidedServerOrder() {
        Fixture fixture = new Fixture();
        when(fixture.serverService.listOwned(any(), any()))
                .thenReturn(new Page<ManagedServer>(1, 10));
        PageParam pageParam = new PageParam(1, 10, "name desc");

        withOwner(7, () -> fixture.service.list(new ManagedServerQueryForm(), pageParam));

        ArgumentCaptor<PageParam> page = ArgumentCaptor.forClass(PageParam.class);
        verify(fixture.serverService).listOwned(any(), page.capture());
        assertThat(page.getValue().getOrderBy()).isEqualTo("name desc");
    }

    @Test
    void savesPasswordsOnlyAfterExplicitOptIn() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.targetValidator.resolveAllowedAddress("203.0.113.20"))
                .thenReturn(InetAddress.getByName("203.0.113.20"));
        when(fixture.cipher.encrypt(
                ServerConstants.SSH_PASSWORD_CREDENTIAL_PURPOSE, "secret"))
                .thenReturn("ciphertext");
        ManagedServerForm form = form();
        form.setPassword("secret");
        form.setSavePassword(true);

        withOwner(7, () -> fixture.service.add(form));

        ArgumentCaptor<ManagedServer> saved = ArgumentCaptor.forClass(ManagedServer.class);
        verify(fixture.serverService).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(7);
        assertThat(saved.getValue().getPasswordCiphertext()).isEqualTo("ciphertext");
    }

    @Test
    void neverFallsBackToAnAdminWideLookup() {
        Fixture fixture = new Fixture();
        when(fixture.serverService.getByIdAndOwnerId(9L, 7)).thenReturn(null);

        assertThatThrownBy(() -> withOwner(7, () -> fixture.service.get(9L)))
                .isInstanceOf(BizException.class)
                .hasMessage(ServerConstants.SERVER_UNAVAILABLE_MESSAGE);

        verify(fixture.serverService).getByIdAndOwnerId(9L, 7);
        verify(fixture.serverService, never()).getById(9L);
    }

    @Test
    void probesTheHostKeyBeforeRequestingAPassword() {
        Fixture fixture = new Fixture();
        ManagedServer server = server();
        when(fixture.serverService.getByIdAndOwnerId(11L, 7)).thenReturn(server);
        when(fixture.connectionService.probeHostKey(server))
                .thenReturn(new SshConnectionService.SshHostKey("SHA256:test", "ED25519"));

        var result = withOwner(7, () -> fixture.service.test(11L, new ServerPasswordForm()));

        assertThat(result.getStatus()).isEqualTo(ServerConstants.TEST_STATUS_CONFIRM_REQUIRED);
        assertThat(result.getFingerprint()).isEqualTo("SHA256:test");
        verify(fixture.cipher, never()).decrypt(any(), any());
    }

    @Test
    void issuesAShortLivedTicketWithAnExplicitPassword() {
        Fixture fixture = new Fixture();
        ManagedServer server = server();
        server.setTrustedFingerprint("SHA256:test");
        when(fixture.serverService.getByIdAndOwnerId(11L, 7)).thenReturn(server);
        TerminalTicketStore.TerminalTicket issued = new TerminalTicketStore.TerminalTicket(
                "ticket", 7, 11L, "temporary", 90, 28, Instant.now().plusSeconds(30));
        when(fixture.ticketStore.issue(7, 11L, "temporary", 90, 28)).thenReturn(issued);
        TerminalTicketForm form = new TerminalTicketForm();
        form.setPassword("temporary");
        form.setColumns(90);
        form.setRows(28);

        var result = withOwner(7, () -> fixture.service.issueTerminalTicket(11L, form));

        assertThat(result.getTicket()).isEqualTo("ticket");
        verify(fixture.cipher, never()).decrypt(any(), any());
    }

    @Test
    void usesStableTerminalDimensionsWhenTheClientOmitsThem() {
        Fixture fixture = new Fixture();
        ManagedServer server = server();
        server.setTrustedFingerprint("SHA256:test");
        when(fixture.serverService.getByIdAndOwnerId(11L, 7)).thenReturn(server);
        TerminalTicketStore.TerminalTicket issued = new TerminalTicketStore.TerminalTicket(
                "ticket", 7, 11L, "temporary",
                ServerConstants.DEFAULT_TERMINAL_COLUMNS,
                ServerConstants.DEFAULT_TERMINAL_ROWS,
                Instant.now().plusSeconds(30));
        when(fixture.ticketStore.issue(
                7, 11L, "temporary",
                ServerConstants.DEFAULT_TERMINAL_COLUMNS,
                ServerConstants.DEFAULT_TERMINAL_ROWS)).thenReturn(issued);
        TerminalTicketForm form = new TerminalTicketForm();
        form.setPassword("temporary");

        withOwner(7, () -> fixture.service.issueTerminalTicket(11L, form));

        verify(fixture.ticketStore).issue(
                7, 11L, "temporary",
                ServerConstants.DEFAULT_TERMINAL_COLUMNS,
                ServerConstants.DEFAULT_TERMINAL_ROWS);
    }

    @Test
    void invalidatesAllTerminalAccessBeforeDeletingAServer() {
        Fixture fixture = new Fixture();
        when(fixture.serverService.getByIdAndOwnerId(11L, 7)).thenReturn(server());
        when(fixture.serverService.removeByIdAndOwnerId(11L, 7)).thenReturn(true);

        withOwner(7, () -> fixture.service.delete(11L));

        verify(fixture.ticketStore).removeByServer(7, 11L);
        verify(fixture.terminalSessionManager).closeByServer(7, 11L);
        verify(fixture.serverService).removeByIdAndOwnerId(11L, 7);
    }

    private static ManagedServerForm form() {
        ManagedServerForm form = new ManagedServerForm();
        form.setName("Production");
        form.setHost("203.0.113.20");
        form.setPort(22);
        form.setUsername("deploy");
        form.setEnabled(1);
        form.setSort(0);
        return form;
    }

    private static ManagedServer server() {
        ManagedServer server = new ManagedServer();
        server.setId(11L);
        server.setOwnerId(7);
        server.setHost("203.0.113.20");
        server.setPort(22);
        server.setUsername("deploy");
        server.setEnabled(1);
        return server;
    }

    private static <T> T withOwner(int ownerId, java.util.concurrent.Callable<T> action) {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(ownerId);
            try {
                return action.call();
            } catch (RuntimeException runtimeException) {
                throw runtimeException;
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static void withOwner(int ownerId, Runnable action) {
        withOwner(ownerId, () -> {
            action.run();
            return null;
        });
    }

    private static final class Fixture {
        private final ManagedServerService serverService = mock(ManagedServerService.class);
        private final PlatformCredentialCipher cipher = mock(PlatformCredentialCipher.class);
        private final SshTargetValidator targetValidator = mock(SshTargetValidator.class);
        private final SshConnectionService connectionService = mock(SshConnectionService.class);
        private final TerminalTicketStore ticketStore = mock(TerminalTicketStore.class);
        private final SshTerminalSessionManager terminalSessionManager =
                mock(SshTerminalSessionManager.class);
        private final ManagedServerBizService service = new ManagedServerBizService(
                serverService, cipher, targetValidator, connectionService,
                ticketStore, terminalSessionManager);
    }
}
