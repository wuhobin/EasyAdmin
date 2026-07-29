package com.nexora.job;

import com.nexora.handler.mail.ImapMailClient;
import com.nexora.handler.mail.MailCredentialCipher;
import com.nexora.service.MailAccountService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailFetchTaskTest {

    @Test
    void fetchesOnlyEnabledMailboxesOwnedByActiveUsers() {
        MailAccountService accountService = mock(MailAccountService.class);
        when(accountService.listEnabledForActiveOwners()).thenReturn(List.of());
        MailFetchTask task = new MailFetchTask(
                accountService,
                mock(MailCredentialCipher.class),
                mock(ImapMailClient.class),
                Runnable::run);

        task.checkNewMails();

        verify(accountService).listEnabledForActiveOwners();
    }
}
