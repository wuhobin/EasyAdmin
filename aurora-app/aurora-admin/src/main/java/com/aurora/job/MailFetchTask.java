package com.aurora.job;

import com.aurora.entity.MailAccount;
import com.aurora.handler.mail.ImapMailClient;
import com.aurora.handler.mail.MailCredentialCipher;
import com.aurora.handler.mail.MailCursor;
import com.aurora.service.MailAccountService;
import com.aurora.starter.redis.core.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("mailFetchTask")
@RequiredArgsConstructor
public class MailFetchTask {
    private static final String LATEST_UID_KEY = "mail:latest-uid:";

    private final MailAccountService mailAccountService;
    private final MailCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;
    private final RedisCache redisCache;

    public void checkNewMails() {
        for (MailAccount account : mailAccountService.listEnabled()) {
            checkAccount(account);
        }
    }

    private void checkAccount(MailAccount account) {
        try {
            MailCursor cursor = imapMailClient.latestCursor(account,
                    credentialCipher.decrypt(account.getAuthCodeCiphertext()));
            String key = LATEST_UID_KEY + account.getId();
            redisCache.setCacheObject(key, String.valueOf(cursor.latestUid()), 24, TimeUnit.HOURS);

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
