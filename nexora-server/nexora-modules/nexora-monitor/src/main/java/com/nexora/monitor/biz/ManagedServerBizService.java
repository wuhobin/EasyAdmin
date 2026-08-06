package com.nexora.monitor.biz;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.monitor.constants.ServerConstants;
import com.nexora.monitor.domain.convert.ManagedServerConvert;
import com.nexora.monitor.domain.form.ManagedServerForm;
import com.nexora.monitor.domain.form.ManagedServerQueryForm;
import com.nexora.monitor.domain.form.ServerFingerprintForm;
import com.nexora.monitor.domain.form.ServerPasswordForm;
import com.nexora.monitor.domain.form.TerminalTicketForm;
import com.nexora.monitor.domain.query.ManagedServerQuery;
import com.nexora.monitor.domain.vo.ManagedServerVo;
import com.nexora.monitor.domain.vo.ServerConnectionTestVo;
import com.nexora.monitor.domain.vo.TerminalTicketVo;
import com.nexora.monitor.entity.ManagedServer;
import com.nexora.monitor.infrastructure.serverssh.AuthenticatedSshConnection;
import com.nexora.monitor.infrastructure.serverssh.SshConnectionService;
import com.nexora.monitor.infrastructure.serverssh.SshHostKeyMismatchException;
import com.nexora.monitor.infrastructure.serverssh.SshTargetValidator;
import com.nexora.monitor.infrastructure.serverssh.SshTerminalSessionManager;
import com.nexora.monitor.infrastructure.serverssh.TerminalTicketStore;
import com.nexora.monitor.service.ManagedServerService;
import com.nexora.security.PlatformCredentialCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class ManagedServerBizService {

    private final ManagedServerService serverService;
    private final PlatformCredentialCipher credentialCipher;
    private final SshTargetValidator targetValidator;
    private final SshConnectionService connectionService;
    private final TerminalTicketStore ticketStore;
    private final SshTerminalSessionManager terminalSessionManager;

    public ManagedServerBizService(ManagedServerService serverService,
                                   PlatformCredentialCipher credentialCipher,
                                   SshTargetValidator targetValidator,
                                   SshConnectionService connectionService,
                                   TerminalTicketStore ticketStore,
                                   SshTerminalSessionManager terminalSessionManager) {
        this.serverService = serverService;
        this.credentialCipher = credentialCipher;
        this.targetValidator = targetValidator;
        this.connectionService = connectionService;
        this.ticketStore = ticketStore;
        this.terminalSessionManager = terminalSessionManager;
    }

    public IPage<ManagedServerVo> list(ManagedServerQueryForm form, PageParam pageParam) {
        ManagedServerQuery query = ManagedServerConvert.INSTANCE.toQuery(form);
        if (query == null) {
            query = new ManagedServerQuery();
        }
        query.setOwnerId(currentOwnerId());
        return serverService.listOwned(query, normalizePage(pageParam)).convert(this::toVo);
    }

    public ManagedServerVo get(Long id) {
        return toVo(getRequired(id, currentOwnerId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ManagedServerVo add(ManagedServerForm form) {
        Integer ownerId = currentOwnerId();
        normalize(form);
        targetValidator.resolveAllowedAddress(form.getHost());
        ManagedServer server = new ManagedServer();
        applyEditableFields(server, form);
        server.setOwnerId(ownerId);
        if (Boolean.TRUE.equals(form.getSavePassword())) {
            server.setPasswordCiphertext(credentialCipher.encrypt(
                    ServerConstants.SSH_PASSWORD_CREDENTIAL_PURPOSE,
                    requirePassword(form.getPassword())));
        }
        serverService.save(server);
        return toVo(server);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ManagedServerForm form) {
        if (form.getId() == null) {
            throw new BizException("服务器 ID 不能为空");
        }
        Integer ownerId = currentOwnerId();
        ManagedServer current = getRequired(form.getId(), ownerId);
        normalize(form);
        targetValidator.resolveAllowedAddress(form.getHost());
        boolean endpointChanged = !Objects.equals(current.getHost(), form.getHost())
                || !Objects.equals(current.getPort(), form.getPort());

        ManagedServer update = new ManagedServer();
        update.setId(current.getId());
        applyEditableFields(update, form);
        if (Boolean.TRUE.equals(form.getSavePassword()) && StringUtils.hasText(form.getPassword())) {
            update.setPasswordCiphertext(credentialCipher.encrypt(
                    ServerConstants.SSH_PASSWORD_CREDENTIAL_PURPOSE, form.getPassword()));
        }
        if (!serverService.updateByIdAndOwnerId(update, ownerId)) {
            throw new BizException(ServerConstants.SERVER_UNAVAILABLE_MESSAGE);
        }
        if (Boolean.TRUE.equals(form.getClearSavedPassword())) {
            serverService.clearSavedPassword(current.getId(), ownerId);
        }
        invalidateTerminalTickets(ownerId, current.getId());
        if (endpointChanged) {
            serverService.clearTrustedFingerprint(current.getId(), ownerId);
        }
        if (endpointChanged || !Integer.valueOf(1).equals(form.getEnabled())) {
            closeTerminalSessions(ownerId, current.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Integer ownerId = currentOwnerId();
        getRequired(id, ownerId);
        invalidateTerminalAccess(ownerId, id);
        if (!serverService.removeByIdAndOwnerId(id, ownerId)) {
            throw new BizException(ServerConstants.SERVER_UNAVAILABLE_MESSAGE);
        }
    }

    public ServerConnectionTestVo test(Long id, ServerPasswordForm form) {
        Integer ownerId = currentOwnerId();
        ManagedServer server = getEnabled(id, ownerId);
        if (hasNoTrustedFingerprint(server)) {
            return buildFingerprintConfirmationResult(server);
        }

        String password = resolvePassword(server, form == null ? null : form.getPassword());
        try (AuthenticatedSshConnection ignored = connectionService.authenticate(server, password)) {
            return recordSuccessfulConnection(id, ownerId, server);
        } catch (SshHostKeyMismatchException exception) {
            return recordFingerprintMismatch(id, ownerId, exception);
        } catch (BizException exception) {
            serverService.updateConnectionState(id, ownerId, exception.getMessage());
            throw exception;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmFingerprint(Long id, ServerFingerprintForm form) {
        Integer ownerId = currentOwnerId();
        ManagedServer server = getEnabled(id, ownerId);
        SshConnectionService.SshHostKey current = connectionService.probeHostKey(server);
        if (!current.fingerprint().equals(form.getFingerprint())) {
            throw new BizException(ServerConstants.SSH_HOST_KEY_CONFIRMATION_FAILED_MESSAGE);
        }
        serverService.updateTrustedFingerprint(
                id, ownerId, current.fingerprint(), current.algorithm(), LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetFingerprint(Long id) {
        Integer ownerId = currentOwnerId();
        getRequired(id, ownerId);
        invalidateTerminalAccess(ownerId, id);
        serverService.clearTrustedFingerprint(id, ownerId);
    }

    public TerminalTicketVo issueTerminalTicket(Long id, TerminalTicketForm form) {
        Integer ownerId = currentOwnerId();
        ManagedServer server = getEnabled(id, ownerId);
        if (!StringUtils.hasText(server.getTrustedFingerprint())) {
            throw new BizException(ServerConstants.SSH_HOST_KEY_REQUIRED_MESSAGE);
        }
        if (terminalSessionManager.activeCount(ownerId) >= 3) {
            throw new BizException(ServerConstants.SSH_TERMINAL_LIMIT_MESSAGE);
        }
        String password = resolvePassword(server, form == null ? null : form.getPassword());
        int columns = valueOrDefault(
                form == null ? null : form.getColumns(), ServerConstants.DEFAULT_TERMINAL_COLUMNS);
        int rows = valueOrDefault(
                form == null ? null : form.getRows(), ServerConstants.DEFAULT_TERMINAL_ROWS);
        TerminalTicketStore.TerminalTicket ticket =
                ticketStore.issue(ownerId, id, password, columns, rows);
        return TerminalTicketVo.builder()
                .ticket(ticket.value())
                .expiresAt(ticket.expiresAt())
                .build();
    }

    private ManagedServer getEnabled(Long id, Integer ownerId) {
        ManagedServer server = getRequired(id, ownerId);
        if (!Integer.valueOf(1).equals(server.getEnabled())) {
            throw new BizException(ServerConstants.SERVER_DISABLED_MESSAGE);
        }
        return server;
    }

    private ManagedServer getRequired(Long id, Integer ownerId) {
        if (id == null) {
            throw new BizException(ServerConstants.SERVER_UNAVAILABLE_MESSAGE);
        }
        ManagedServer server = serverService.getByIdAndOwnerId(id, ownerId);
        if (server == null) {
            throw new BizException(ServerConstants.SERVER_UNAVAILABLE_MESSAGE);
        }
        return server;
    }

    private String resolvePassword(ManagedServer server, String providedPassword) {
        if (StringUtils.hasText(providedPassword)) {
            return providedPassword;
        }
        if (StringUtils.hasText(server.getPasswordCiphertext())) {
            return credentialCipher.decrypt(
                    ServerConstants.SSH_PASSWORD_CREDENTIAL_PURPOSE,
                    server.getPasswordCiphertext());
        }
        throw new BizException(ServerConstants.SSH_PASSWORD_REQUIRED_MESSAGE);
    }

    private ManagedServerVo toVo(ManagedServer server) {
        ManagedServerVo vo = ManagedServerConvert.INSTANCE.toVo(server);
        vo.setHasSavedPassword(StringUtils.hasText(server.getPasswordCiphertext()));
        return vo;
    }

    private static void applyEditableFields(ManagedServer server, ManagedServerForm form) {
        server.setName(form.getName());
        server.setHost(form.getHost());
        server.setPort(form.getPort());
        server.setUsername(form.getUsername());
        server.setDescription(form.getDescription());
        server.setEnabled(form.getEnabled());
        server.setSort(form.getSort());
    }

    private static void normalize(ManagedServerForm form) {
        form.setName(form.getName().trim());
        form.setHost(form.getHost().trim());
        form.setUsername(form.getUsername().trim());
        form.setDescription(StringUtils.hasText(form.getDescription())
                ? form.getDescription().trim() : "");
        if (form.getPort() == null) {
            form.setPort(22);
        }
        if (form.getEnabled() == null) {
            form.setEnabled(1);
        }
        if (form.getSort() == null) {
            form.setSort(0);
        }
    }

    private static String requirePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BizException(ServerConstants.SSH_PASSWORD_REQUIRED_MESSAGE);
        }
        return password;
    }

    private static Integer currentOwnerId() {
        int ownerId = SecurityUtils.getLoginIdAsInt();
        if (ownerId <= 0) {
            throw new BizException(ServerConstants.SERVER_UNAVAILABLE_MESSAGE);
        }
        return ownerId;
    }

    private static PageParam normalizePage(PageParam pageParam) {
        PageParam result = pageParam == null ? new PageParam() : pageParam;
        if (result.getPageNum() == null) {
            result.setPageNum(PageParam.DEFAULT_PAGE);
        }
        if (result.getPageSize() == null) {
            result.setPageSize(PageParam.DEFAULT_SIZE);
        }
        if (result.getOrderBy() == null || result.getOrderBy().isBlank()) {
            result.setOrderBy("sort asc,id desc");
        }
        return result;
    }

    private void invalidateTerminalAccess(Integer ownerId, Long serverId) {
        invalidateTerminalTickets(ownerId, serverId);
        closeTerminalSessions(ownerId, serverId);
    }

    private void invalidateTerminalTickets(Integer ownerId, Long serverId) {
        ticketStore.removeByServer(ownerId, serverId);
    }

    private void closeTerminalSessions(Integer ownerId, Long serverId) {
        terminalSessionManager.closeByServer(ownerId, serverId);
    }

    private static boolean hasNoTrustedFingerprint(ManagedServer server) {
        return !StringUtils.hasText(server.getTrustedFingerprint());
    }

    private ServerConnectionTestVo buildFingerprintConfirmationResult(ManagedServer server) {
        SshConnectionService.SshHostKey hostKey = connectionService.probeHostKey(server);
        return ServerConnectionTestVo.builder()
                .status(ServerConstants.TEST_STATUS_CONFIRM_REQUIRED)
                .fingerprint(hostKey.fingerprint())
                .algorithm(hostKey.algorithm())
                .build();
    }

    private ServerConnectionTestVo recordSuccessfulConnection(
            Long id, Integer ownerId, ManagedServer server) {
        serverService.updateConnectionState(id, ownerId, "");
        return ServerConnectionTestVo.builder()
                .status(ServerConstants.TEST_STATUS_SUCCESS)
                .fingerprint(server.getTrustedFingerprint())
                .trustedFingerprint(server.getTrustedFingerprint())
                .algorithm(server.getFingerprintAlgorithm())
                .build();
    }

    private ServerConnectionTestVo recordFingerprintMismatch(
            Long id, Integer ownerId, SshHostKeyMismatchException exception) {
        serverService.updateConnectionState(
                id, ownerId, ServerConstants.SSH_HOST_KEY_CHANGED_MESSAGE);
        return ServerConnectionTestVo.builder()
                .status(ServerConstants.TEST_STATUS_FINGERPRINT_MISMATCH)
                .fingerprint(exception.getPresentedFingerprint())
                .trustedFingerprint(exception.getTrustedFingerprint())
                .algorithm(exception.getAlgorithm())
                .build();
    }

    private static int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
