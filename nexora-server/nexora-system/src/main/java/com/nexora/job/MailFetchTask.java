package com.nexora.job;

import com.nexora.entity.MailAccount;
import com.nexora.handler.mail.ImapMailClient;
import com.nexora.handler.mail.MailCredentialCipher;
import com.nexora.handler.mail.MailCursor;
import com.nexora.service.MailAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component("mailFetchTask")
public class MailFetchTask {
    private final MailAccountService mailAccountService;
    private final MailCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;
    private final Executor mailExecutor;

    public MailFetchTask(MailAccountService mailAccountService,
                         MailCredentialCipher credentialCipher,
                         ImapMailClient imapMailClient,
                         @Qualifier("mailExecutor") Executor mailExecutor) {
        this.mailAccountService = mailAccountService;
        this.credentialCipher = credentialCipher;
        this.imapMailClient = imapMailClient;
        this.mailExecutor = mailExecutor;
    }

    public void checkNewMails() {
        log.info("邮箱检查定时任务开始");
        CompletableFuture.allOf(mailAccountService.listEnabledForActiveOwners().stream()
                .map(account -> CompletableFuture.runAsync(() -> checkAccount(account), mailExecutor))
                .toArray(CompletableFuture[]::new)).join();
    }

    private void checkAccount(MailAccount account) {
        try {
            MailCursor cursor = imapMailClient.latestCursor(account,
                    credentialCipher.decrypt(account.getAuthCodeCiphertext()));
            if (account.getUidValidity() != null
                    && account.getUidValidity().longValue() == cursor.uidValidity()
                    && account.getLastUid() != null
                    && cursor.latestUid() > account.getLastUid()) {
                log.info("New mail detected, accountId={}, provider={}, latestUid={}, previousUid={}",
                        account.getId(), account.getProvider(), cursor.latestUid(), account.getLastUid());
            }

            MailAccount update = new MailAccount();
            update.setId(account.getId());
            update.setLastUid(cursor.latestUid());
            update.setUidValidity(cursor.uidValidity());
            update.setLastConnectTime(LocalDateTime.now());
            update.setLastError("");
            mailAccountService.updateById(update);
        } catch (Exception exception) {
            MailAccount update = new MailAccount();
            update.setId(account.getId());
            update.setLastConnectTime(LocalDateTime.now());
            update.setLastError(exception.getMessage());
            mailAccountService.updateById(update);
            log.warn("Failed to check mailbox, accountId={}, provider={}",
                    account.getId(), account.getProvider(), exception);
        }
    }
}
