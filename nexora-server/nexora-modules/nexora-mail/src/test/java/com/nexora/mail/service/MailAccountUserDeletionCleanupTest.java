package com.nexora.mail.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MailAccountUserDeletionCleanupTest {

    @Test
    void removesMailAccountsForTheDeletedUsers() {
        MailAccountService mailAccountService = mock(MailAccountService.class);
        MailAccountUserDeletionCleanup cleanup = new MailAccountUserDeletionCleanup(mailAccountService);

        cleanup.cleanup(List.of(7, 8));

        verify(mailAccountService).removeByOwnerIds(List.of(7, 8));
    }
}
