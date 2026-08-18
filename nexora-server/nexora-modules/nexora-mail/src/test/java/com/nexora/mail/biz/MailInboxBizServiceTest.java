package com.nexora.mail.biz;

import com.nexora.mail.constants.MailConstants;
import com.nexora.mail.domain.vo.MailMessageDetailVo;
import com.nexora.mail.domain.vo.MailMessagePageVo;
import com.nexora.mail.domain.vo.MailMessageSummaryVo;
import com.nexora.mail.entity.MailAccount;
import com.nexora.mail.infrastructure.ImapMailClient;
import com.nexora.mail.infrastructure.MailMessagePage;
import com.nexora.mail.service.MailAccountService;
import com.aurora.starter.webmvc.security.PlatformCredentialCipher;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailInboxBizServiceTest {

    @Test
    void keepsPerAccountOffsetsInAggregateCursor() {
        MailAccountService accountService = mock(MailAccountService.class);
        PlatformCredentialCipher credentialCipher = mock(PlatformCredentialCipher.class);
        ImapMailClient imapMailClient = mock(ImapMailClient.class);
        MailAccount first = account(1L);
        MailAccount second = account(2L);
        when(accountService.listEnabledByOwnerId(7)).thenReturn(List.of(first, second));
        when(credentialCipher.decrypt(eq(MailConstants.MAIL_CREDENTIAL_PURPOSE), anyString()))
                .thenReturn("auth-code");
        when(imapMailClient.listPage(eq(first), anyString(), anyInt(), isNull(), eq(0)))
                .thenReturn(new MailMessagePage(List.of(message(1L, 11, 10), message(1L, 10, 8)), 100, true));
        when(imapMailClient.listPage(eq(second), anyString(), anyInt(), isNull(), eq(0)))
                .thenReturn(new MailMessagePage(List.of(message(2L, 21, 9), message(2L, 20, 7)), 200, true));
        when(imapMailClient.listPage(eq(first), anyString(), anyInt(), eq(100L), eq(1)))
                .thenReturn(new MailMessagePage(List.of(message(1L, 10, 8)), 100, false));
        when(imapMailClient.listPage(eq(second), anyString(), anyInt(), eq(200L), eq(1)))
                .thenReturn(new MailMessagePage(List.of(message(2L, 20, 7)), 200, false));

        MailInboxBizService service = new MailInboxBizService(accountService, credentialCipher,
                imapMailClient, new ObjectMapper(), Runnable::run);

        MailMessagePageVo firstPage;
        MailMessagePageVo secondPage;
        try (MockedStatic<SecurityUtils> securityUtils = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);
            firstPage = service.list(null, 2, null, false);
            secondPage = service.list(null, 2, firstPage.getNextCursor(), false);
        }

        assertThat(firstPage.getItems()).extracting(MailMessageSummaryVo::getUid).containsExactly(11L, 21L);
        assertThat(firstPage.isHasMore()).isTrue();
        assertThat(secondPage.getItems()).extracting(MailMessageSummaryVo::getUid).containsExactly(10L, 20L);
        verify(imapMailClient).listPage(first, "auth-code", 2, 100L, 1);
        verify(imapMailClient).listPage(second, "auth-code", 2, 200L, 1);
    }

    @Test
    void reusesTheFirstPageCacheUnlessRefreshIsRequested() {
        MailAccountService accountService = mock(MailAccountService.class);
        PlatformCredentialCipher credentialCipher = mock(PlatformCredentialCipher.class);
        ImapMailClient imapMailClient = mock(ImapMailClient.class);
        MailAccount account = account(1L);
        when(accountService.getByIdAndOwnerId(1L, 7)).thenReturn(account);
        when(credentialCipher.decrypt(MailConstants.MAIL_CREDENTIAL_PURPOSE, "ciphertext"))
                .thenReturn("auth-code");
        when(imapMailClient.listPage(account, "auth-code", 30, null, 0))
                .thenReturn(new MailMessagePage(List.of(message(1L, 11, 10)), 11, false));
        MailInboxBizService service = new MailInboxBizService(accountService, credentialCipher,
                imapMailClient, new ObjectMapper(), Runnable::run);

        try (MockedStatic<SecurityUtils> securityUtils = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            service.list(1L, 30, null, false);
            service.list(1L, 30, null, false);
            service.list(1L, 30, null, true);
        }

        verify(imapMailClient, times(2)).listPage(account, "auth-code", 30, null, 0);
    }

    @Test
    void openingAMessageMarksItAsReadAndInvalidatesTheFirstPageCache() {
        MailAccountService accountService = mock(MailAccountService.class);
        PlatformCredentialCipher credentialCipher = mock(PlatformCredentialCipher.class);
        ImapMailClient imapMailClient = mock(ImapMailClient.class);
        MailAccount account = account(1L);
        when(accountService.getByIdAndOwnerId(1L, 7)).thenReturn(account);
        when(credentialCipher.decrypt(MailConstants.MAIL_CREDENTIAL_PURPOSE, "ciphertext"))
                .thenReturn("auth-code");
        when(imapMailClient.listPage(account, "auth-code", 30, null, 0))
                .thenReturn(new MailMessagePage(List.of(message(1L, 11, 10)), 11, false));
        when(imapMailClient.openMessage(account, "auth-code", 11, 22))
                .thenReturn(MailMessageDetailVo.builder().accountId(1L).uid(11L).uidValidity(22L).build());
        MailInboxBizService service = new MailInboxBizService(accountService, credentialCipher,
                imapMailClient, new ObjectMapper(), Runnable::run);

        try (MockedStatic<SecurityUtils> securityUtils = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            service.list(1L, 30, null, false);
            service.openMessage(1L, 11, 22);
            service.list(1L, 30, null, false);
        }

        verify(imapMailClient).openMessage(account, "auth-code", 11, 22);
        verify(imapMailClient, times(2)).listPage(account, "auth-code", 30, null, 0);
    }

    @Test
    void reusesCachedDetailsAndOnlyWritesTheSeenFlagWhenOpeningThem() {
        MailAccountService accountService = mock(MailAccountService.class);
        PlatformCredentialCipher credentialCipher = mock(PlatformCredentialCipher.class);
        ImapMailClient imapMailClient = mock(ImapMailClient.class);
        MailAccount account = account(1L);
        MailMessageDetailVo detail = MailMessageDetailVo.builder()
                .accountId(1L).uid(11L).uidValidity(22L).subject("cached").build();
        when(accountService.getByIdAndOwnerId(1L, 7)).thenReturn(account);
        when(credentialCipher.decrypt(MailConstants.MAIL_CREDENTIAL_PURPOSE, "ciphertext"))
                .thenReturn("auth-code");
        when(imapMailClient.getDetail(account, "auth-code", 11, 22)).thenReturn(detail);
        MailInboxBizService service = new MailInboxBizService(accountService, credentialCipher,
                imapMailClient, new ObjectMapper(), Runnable::run);

        try (MockedStatic<SecurityUtils> securityUtils = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            assertThat(service.getDetail(1L, 11, 22)).isSameAs(detail);
            assertThat(service.getDetail(1L, 11, 22)).isSameAs(detail);
            assertThat(service.openMessage(1L, 11, 22)).isSameAs(detail);
        }

        verify(imapMailClient).getDetail(account, "auth-code", 11, 22);
        verify(imapMailClient).markRead(account, "auth-code", 11, 22);
        verify(imapMailClient, never()).openMessage(account, "auth-code", 11, 22);
    }

    @Test
    void rejectsReadingAnotherUsersAccountWithoutCallingImap() {
        MailAccountService accountService = mock(MailAccountService.class);
        PlatformCredentialCipher credentialCipher = mock(PlatformCredentialCipher.class);
        ImapMailClient imapMailClient = mock(ImapMailClient.class);
        when(accountService.getByIdAndOwnerId(99L, 7)).thenReturn(null);
        MailInboxBizService service = new MailInboxBizService(accountService, credentialCipher,
                imapMailClient, new ObjectMapper(), Runnable::run);

        try (MockedStatic<SecurityUtils> securityUtils = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            assertThatThrownBy(() -> service.getDetail(99L, 1L, 1L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("邮箱账户不存在或不可用");
        }

        verify(credentialCipher, never()).decrypt(anyString(), anyString());
    }

    private static MailAccount account(Long id) {
        MailAccount account = new MailAccount();
        account.setId(id);
        account.setProvider("QQ");
        account.setAuthCodeCiphertext("ciphertext");
        account.setEnabled(1);
        return account;
    }

    private static MailMessageSummaryVo message(Long accountId, long uid, int minute) {
        return MailMessageSummaryVo.builder()
                .accountId(accountId)
                .uid(uid)
                .receivedTime(LocalDateTime.of(2026, 7, 20, 10, minute))
                .build();
    }
}
