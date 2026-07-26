package com.nexora.biz;

import com.nexora.domain.vo.mail.MailMessagePageVo;
import com.nexora.domain.vo.mail.MailMessageSummaryVo;
import com.nexora.entity.MailAccount;
import com.nexora.handler.mail.ImapMailClient;
import com.nexora.handler.mail.MailCredentialCipher;
import com.nexora.handler.mail.MailMessagePage;
import com.nexora.service.MailAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailInboxBizServiceTest {

    @Test
    void keepsPerAccountOffsetsInAggregateCursor() {
        MailAccountService accountService = mock(MailAccountService.class);
        MailCredentialCipher credentialCipher = mock(MailCredentialCipher.class);
        ImapMailClient imapMailClient = mock(ImapMailClient.class);
        MailAccount first = account(1L);
        MailAccount second = account(2L);
        when(accountService.listEnabled()).thenReturn(List.of(first, second));
        when(credentialCipher.decrypt(anyString())).thenReturn("auth-code");
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

        MailMessagePageVo firstPage = service.list(null, 2, null);
        MailMessagePageVo secondPage = service.list(null, 2, firstPage.getNextCursor());

        assertThat(firstPage.getItems()).extracting(MailMessageSummaryVo::getUid).containsExactly(11L, 21L);
        assertThat(firstPage.isHasMore()).isTrue();
        assertThat(secondPage.getItems()).extracting(MailMessageSummaryVo::getUid).containsExactly(10L, 20L);
        verify(imapMailClient).listPage(first, "auth-code", 2, 100L, 1);
        verify(imapMailClient).listPage(second, "auth-code", 2, 200L, 1);
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
