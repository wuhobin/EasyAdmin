package com.aurora.biz;

import com.aurora.domain.vo.mail.MailMessageDetailVo;
import com.aurora.domain.vo.mail.MailMessageSummaryVo;
import com.aurora.entity.MailAccount;
import com.aurora.mail.ImapMailClient;
import com.aurora.mail.MailCredentialCipher;
import com.aurora.service.MailAccountService;
import com.aurora.starter.webmvc.exception.BizException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailInboxBizService {
    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 50;

    private final MailAccountService mailAccountService;
    private final MailCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;

    public List<MailMessageSummaryVo> list(Long accountId, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        if (accountId != null) {
            MailAccount account = getEnabledAccount(accountId);
            return listAccount(account, normalizedLimit, true);
        }

        List<MailMessageSummaryVo> result = new ArrayList<>();
        for (MailAccount account : mailAccountService.listEnabled()) {
            result.addAll(listAccount(account, normalizedLimit, false));
        }
        result.sort(Comparator.comparing(MailMessageSummaryVo::getReceivedTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result.stream().limit(normalizedLimit).toList();
    }

    public MailMessageDetailVo getDetail(Long accountId, long uid, long uidValidity) {
        MailAccount account = getEnabledAccount(accountId);
        return imapMailClient.getDetail(account, decrypt(account), uid, uidValidity);
    }

    public void downloadAttachment(Long accountId, long uid, long uidValidity, String partId,
                                   HttpServletResponse response) {
        MailAccount account = getEnabledAccount(accountId);
        imapMailClient.downloadAttachment(account, decrypt(account), uid, uidValidity, partId, response);
    }

    private List<MailMessageSummaryVo> listAccount(MailAccount account, int limit, boolean failFast) {
        try {
            List<MailMessageSummaryVo> messages = imapMailClient.listLatest(account, decrypt(account), limit);
            updateConnection(account, null);
            return messages;
        } catch (BizException exception) {
            updateConnection(account, exception.getMessage());
            if (failFast) {
                throw exception;
            }
            log.warn("Failed to read mailbox, accountId={}, provider={}",
                    account.getId(), account.getProvider(), exception);
            return List.of();
        }
    }

    private MailAccount getEnabledAccount(Long id) {
        MailAccount account = mailAccountService.getById(id);
        if (account == null || !Integer.valueOf(1).equals(account.getEnabled())) {
            throw new BizException("邮箱账户不存在或未启用");
        }
        return account;
    }

    private String decrypt(MailAccount account) {
        return credentialCipher.decrypt(account.getAuthCodeCiphertext());
    }

    private void updateConnection(MailAccount account, String error) {
        MailAccount update = new MailAccount();
        update.setId(account.getId());
        update.setLastConnectTime(LocalDateTime.now());
        update.setLastError(error == null ? "" : error);
        mailAccountService.updateById(update);
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
