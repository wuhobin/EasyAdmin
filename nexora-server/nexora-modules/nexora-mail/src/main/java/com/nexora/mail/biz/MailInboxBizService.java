package com.nexora.mail.biz;

import com.nexora.mail.constants.MailConstants;
import com.nexora.mail.domain.vo.MailMessageDetailVo;
import com.nexora.mail.domain.vo.MailMessagePageVo;
import com.nexora.mail.domain.vo.MailMessageSummaryVo;
import com.nexora.mail.entity.MailAccount;
import com.nexora.mail.infrastructure.ImapMailClient;
import com.nexora.mail.infrastructure.MailCredentialCipher;
import com.nexora.mail.infrastructure.MailMessagePage;
import com.nexora.mail.service.MailAccountService;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class MailInboxBizService {
    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 50;
    private static final Comparator<MailMessageSummaryVo> RECEIVED_TIME_COMPARATOR = Comparator.comparing(
            MailMessageSummaryVo::getReceivedTime, Comparator.nullsLast(Comparator.reverseOrder()));

    private final MailAccountService mailAccountService;
    private final MailCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;
    private final ObjectMapper objectMapper;
    private final Executor mailExecutor;

    public MailInboxBizService(MailAccountService mailAccountService,
                               MailCredentialCipher credentialCipher,
                               ImapMailClient imapMailClient,
                               ObjectMapper objectMapper,
                               @Qualifier("mailExecutor") Executor mailExecutor) {
        this.mailAccountService = mailAccountService;
        this.credentialCipher = credentialCipher;
        this.imapMailClient = imapMailClient;
        this.objectMapper = objectMapper;
        this.mailExecutor = mailExecutor;
    }

    public MailMessagePageVo list(Long accountId, Integer limit, String cursor) {
        int normalizedLimit = normalizeLimit(limit);
        CursorState cursorState = decodeCursor(cursor);
        if (accountId != null) {
            AccountPage accountPage = listAccount(getEnabledAccount(accountId), normalizedLimit,
                    cursorState.accounts().get(accountId), true);
            return buildPage(List.of(accountPage), normalizedLimit);
        }

        List<CompletableFuture<AccountPage>> futures =
                mailAccountService.listEnabledByOwnerId(currentOwnerId()).stream()
                .map(account -> CompletableFuture.supplyAsync(
                        () -> listAccount(account, normalizedLimit,
                                cursorState.accounts().get(account.getId()), false),
                        mailExecutor))
                .toList();
        List<AccountPage> accountPages = futures.stream().map(CompletableFuture::join).toList();

        return buildPage(accountPages, normalizedLimit);
    }

    private MailMessagePageVo buildPage(List<AccountPage> accountPages, int normalizedLimit) {
        List<MailMessageSummaryVo> candidates = accountPages.stream()
                .flatMap(page -> page.page().items().stream())
                .sorted(RECEIVED_TIME_COMPARATOR)
                .toList();
        List<MailMessageSummaryVo> items = candidates.stream().limit(normalizedLimit).toList();

        Map<Long, AccountCursor> nextAccounts = new HashMap<>();
        for (AccountPage accountPage : accountPages) {
            AccountCursor previous = accountPage.cursor();
            long consumed = items.stream()
                    .filter(item -> item.getAccountId().equals(accountPage.accountId()))
                    .count();
            nextAccounts.put(accountPage.accountId(), new AccountCursor(
                    accountPage.page().anchorUid(), previous.offset() + (int) consumed));
        }
        boolean hasMore = candidates.size() > items.size()
                || accountPages.stream().anyMatch(page -> page.page().hasMore());
        return MailMessagePageVo.builder()
                .items(items)
                .hasMore(hasMore)
                .nextCursor(hasMore ? encodeCursor(new CursorState(nextAccounts)) : null)
                .build();
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

    private AccountPage listAccount(MailAccount account, int limit, AccountCursor cursor, boolean failFast) {
        AccountCursor normalizedCursor = cursor == null ? new AccountCursor(0, 0) : cursor;
        try {
            MailMessagePage page = imapMailClient.listPage(account, decrypt(account), limit,
                    normalizedCursor.anchorUid() > 0 ? normalizedCursor.anchorUid() : null,
                    normalizedCursor.offset());
            updateConnection(account, null);
            return new AccountPage(account.getId(), normalizedCursor, page);
        } catch (BizException exception) {
            updateConnection(account, exception.getMessage());
            if (failFast) {
                throw exception;
            }
            log.warn("Failed to read mailbox, accountId={}, provider={}",
                    account.getId(), account.getProvider(), exception);
            return new AccountPage(account.getId(), normalizedCursor,
                    new MailMessagePage(List.of(), normalizedCursor.anchorUid(), false));
        }
    }

    private MailAccount getEnabledAccount(Long id) {
        MailAccount account = mailAccountService.getByIdAndOwnerId(id, currentOwnerId());
        if (account == null || !Integer.valueOf(1).equals(account.getEnabled())) {
            throw new BizException(MailConstants.MAIL_ACCOUNT_UNAVAILABLE_MESSAGE);
        }
        return account;
    }

    private String decrypt(MailAccount account) {
        return credentialCipher.decrypt(account.getAuthCodeCiphertext());
    }

    private void updateConnection(MailAccount account, String error) {
        String normalizedError = error == null ? "" : error;
        boolean errorChanged = !java.util.Objects.equals(
                account.getLastError() == null ? "" : account.getLastError(), normalizedError);
        if (!errorChanged && account.getLastConnectTime() != null
                && account.getLastConnectTime().isAfter(LocalDateTime.now().minusMinutes(1))) {
            return;
        }
        MailAccount update = new MailAccount();
        update.setId(account.getId());
        update.setLastConnectTime(LocalDateTime.now());
        update.setLastError(normalizedError);
        mailAccountService.updateById(update);
    }

    private CursorState decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new CursorState(Map.of());
        }
        if (cursor.length() > 8_192) {
            throw new BizException(MailConstants.MAIL_CURSOR_INVALID_MESSAGE);
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            CursorState state = objectMapper.readValue(json, CursorState.class);
            if (state.accounts() == null || state.accounts().entrySet().stream().anyMatch(entry ->
                    entry.getKey() == null || entry.getValue() == null
                            || entry.getValue().anchorUid() < 0 || entry.getValue().offset() < 0)) {
                throw new IllegalArgumentException("Invalid cursor state");
            }
            return state;
        } catch (Exception exception) {
            throw new BizException(MailConstants.MAIL_CURSOR_INVALID_MESSAGE);
        }
    }

    private String encodeCursor(CursorState state) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(state);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new BizException(MailConstants.MAIL_CURSOR_CREATE_FAILED_MESSAGE);
        }
    }

    private static Integer currentOwnerId() {
        return SecurityUtils.getLoginIdAsInt();
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private record CursorState(Map<Long, AccountCursor> accounts) {
    }

    private record AccountCursor(long anchorUid, int offset) {
    }

    private record AccountPage(Long accountId, AccountCursor cursor, MailMessagePage page) {
    }
}
