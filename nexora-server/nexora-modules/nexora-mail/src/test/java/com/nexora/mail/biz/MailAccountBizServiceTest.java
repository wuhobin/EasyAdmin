package com.nexora.mail.biz;

import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.mail.constants.MailConstants;
import com.nexora.mail.constants.MailProviderEnum;
import com.nexora.mail.domain.form.MailAccountForm;
import com.nexora.mail.entity.MailAccount;
import com.nexora.mail.infrastructure.ImapMailClient;
import com.nexora.mail.infrastructure.MailCredentialCipher;
import com.nexora.mail.service.MailAccountService;
import com.nexora.system.api.DictionaryEntry;
import com.nexora.system.api.SystemDictionaryReader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

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

    @Test
    void listsConfiguredProvidersThroughTheSystemApi() {
        SystemDictionaryReader dictionaryReader = mock(SystemDictionaryReader.class);
        when(dictionaryReader.findEnabledEntries(MailConstants.MAIL_PROVIDER_DICT_TYPE))
                .thenReturn(Optional.of(List.of(
                        new DictionaryEntry("QQ邮箱", "QQ", true),
                        new DictionaryEntry("未知邮箱", "UNKNOWN", false))));
        MailAccountBizService service = service(
                mock(MailAccountService.class),
                mock(MailCredentialCipher.class),
                dictionaryReader);

        assertThat(service.listProviders())
                .singleElement()
                .satisfies(provider -> {
                    assertThat(provider.getLabel()).isEqualTo("QQ邮箱");
                    assertThat(provider.getValue()).isEqualTo("QQ");
                    assertThat(provider.isDefaultProvider()).isTrue();
                });
    }

    private static MailAccountBizService service(MailAccountService accountService) {
        return service(accountService, mock(MailCredentialCipher.class));
    }

    private static MailAccountBizService service(MailAccountService accountService,
                                                 MailCredentialCipher credentialCipher) {
        return service(accountService, credentialCipher, mock(SystemDictionaryReader.class));
    }

    private static MailAccountBizService service(MailAccountService accountService,
                                                 MailCredentialCipher credentialCipher,
                                                 SystemDictionaryReader dictionaryReader) {
        return new MailAccountBizService(
                accountService,
                dictionaryReader,
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
