package com.nexora.biz.mail;

import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.constants.MailProviderEnum;
import com.nexora.domain.form.mail.MailAccountForm;
import com.nexora.entity.MailAccount;
import com.nexora.handler.mail.ImapMailClient;
import com.nexora.handler.mail.MailCredentialCipher;
import com.nexora.service.MailAccountService;
import com.nexora.service.SysDictDataService;
import com.nexora.service.SysDictService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailAccountBizServiceTest {

    @Test
    void listsOnlyTheCurrentUsersAccounts() {
        MailAccountService accountService = mock(MailAccountService.class);
        MailAccount account = new MailAccount();
        account.setId(10L);
        account.setOwnerId(7);
        when(accountService.listOrderedByOwnerId(7)).thenReturn(List.of(account));
        MailAccountBizService service = service(accountService);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            assertThat(service.list()).extracting(item -> item.getId()).containsExactly(10L);
        }

        verify(accountService).listOrderedByOwnerId(7);
    }

    @Test
    void assignsTheCurrentUserAndChecksEmailUniquenessWithinThatUser() {
        MailAccountService accountService = mock(MailAccountService.class);
        MailCredentialCipher credentialCipher = mock(MailCredentialCipher.class);
        when(credentialCipher.encrypt("secret")).thenReturn("ciphertext");
        MailAccountBizService service = service(accountService, credentialCipher);
        MailAccountForm form = form();

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            service.add(form);
        }

        verify(accountService).existsByOwnerIdAndEmail(7, "owner@qq.com", null);
        ArgumentCaptor<MailAccount> captor = ArgumentCaptor.forClass(MailAccount.class);
        verify(accountService).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(7);
        assertThat(captor.getValue().getEmail()).isEqualTo("owner@qq.com");
        assertThat(captor.getValue().getAuthCodeCiphertext()).isEqualTo("ciphertext");
    }

    @Test
    void rejectsUpdatingAnotherUsersAccount() {
        MailAccountService accountService = mock(MailAccountService.class);
        when(accountService.getByIdAndOwnerId(10L, 7)).thenReturn(null);
        MailAccountBizService service = service(accountService);
        MailAccountForm form = form();
        form.setId(10L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            assertThatThrownBy(() -> service.update(form))
                    .isInstanceOf(BizException.class)
                    .hasMessage("邮箱账户不存在或不可用");
        }
    }

    private static MailAccountBizService service(MailAccountService accountService) {
        return service(accountService, mock(MailCredentialCipher.class));
    }

    private static MailAccountBizService service(MailAccountService accountService,
                                                 MailCredentialCipher credentialCipher) {
        return new MailAccountBizService(
                accountService,
                mock(SysDictService.class),
                mock(SysDictDataService.class),
                credentialCipher,
                mock(ImapMailClient.class));
    }

    private static MailAccountForm form() {
        MailAccountForm form = new MailAccountForm();
        form.setAccountName("Owner mailbox");
        form.setProvider(MailProviderEnum.QQ);
        form.setEmail(" Owner@QQ.com ");
        form.setAuthCode("secret");
        form.setEnabled(1);
        form.setSort(0);
        return form;
    }
}
