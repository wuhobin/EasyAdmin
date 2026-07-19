package com.aurora.quartz;

import com.aurora.entity.MailAccount;
import com.aurora.mail.ImapMailClient;
import com.aurora.mail.MailCredentialCipher;
import com.aurora.mail.MailCursor;
import com.aurora.service.MailAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component("mailFetchTask")
@RequiredArgsConstructor
public class MailFetchTask {
    private static final String LATEST_UID_KEY = "mail:latest-uid:";

    private final MailAccountService mailAccountService;
    private final MailCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;
    private final StringRedisTemplate redisTemplate;

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
            redisTemplate.opsForValue().set(key, String.valueOf(cursor.latestUid()), Duration.ofHours(24));

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
