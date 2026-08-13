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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MailInboxBizService {
    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 50;
    private static final long FIRST_PAGE_CACHE_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static final long DETAIL_CACHE_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final int DETAIL_CACHE_MAX_ENTRIES = 50;
    private static final Comparator<MailMessageSummaryVo> RECEIVED_TIME_COMPARATOR = Comparator.comparing(
            MailMessageSummaryVo::getReceivedTime, Comparator.nullsLast(Comparator.reverseOrder()));

    private final MailAccountService mailAccountService;
    private final PlatformCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;
    private final ObjectMapper objectMapper;
    private final Executor mailExecutor;
    private final ConcurrentMap<FirstPageCacheKey, CachedPage> firstPageCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<DetailCacheKey, CachedDetail> detailCache = new ConcurrentHashMap<>();

    public MailInboxBizService(MailAccountService mailAccountService,
                               PlatformCredentialCipher credentialCipher,
                               ImapMailClient imapMailClient,
                               ObjectMapper objectMapper,
                               @Qualifier("mailExecutor") Executor mailExecutor) {
        this.mailAccountService = mailAccountService;
        this.credentialCipher = credentialCipher;
        this.imapMailClient = imapMailClient;
        this.objectMapper = objectMapper;
        this.mailExecutor = mailExecutor;
    }

    public MailMessagePageVo list(Long accountId, Integer limit, String cursor, boolean refresh) {
        int normalizedLimit = normalizeLimit(limit);
        CursorState cursorState = decodeCursor(cursor);
        if (accountId != null) {
            AccountPage accountPage = listAccount(getEnabledAccount(accountId), normalizedLimit,
                    cursorState.accounts().get(accountId), true, refresh);
            return buildPage(List.of(accountPage), normalizedLimit);
        }

        List<CompletableFuture<AccountPage>> futures =
                mailAccountService.listEnabledByOwnerId(currentOwnerId()).stream()
                .map(account -> CompletableFuture.supplyAsync(
                        () -> listAccount(account, normalizedLimit,
                                cursorState.accounts().get(account.getId()), false, refresh),
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
        DetailCacheKey cacheKey = new DetailCacheKey(accountId, uidValidity, uid);
        CachedDetail cachedDetail = freshDetail(cacheKey);
        if (cachedDetail != null) {
            return cachedDetail.detail();
        }
        MailMessageDetailVo detail = imapMailClient.getDetail(account, decrypt(account), uid, uidValidity);
        cacheDetail(cacheKey, detail);
        return detail;
    }

    public MailMessageDetailVo openMessage(Long accountId, long uid, long uidValidity) {
        MailAccount account = getEnabledAccount(accountId);
        String authCode = decrypt(account);
        DetailCacheKey cacheKey = new DetailCacheKey(accountId, uidValidity, uid);
        CachedDetail cachedDetail = freshDetail(cacheKey);
        MailMessageDetailVo detail;
        if (cachedDetail == null) {
            detail = imapMailClient.openMessage(account, authCode, uid, uidValidity);
            cacheDetail(cacheKey, detail);
        } else {
            imapMailClient.markRead(account, authCode, uid, uidValidity);
            detail = cachedDetail.detail();
        }
        firstPageCache.keySet().removeIf(key -> key.accountId().equals(accountId));
        return detail;
    }

    public void markRead(Long accountId, long uid, long uidValidity) {
        MailAccount account = getEnabledAccount(accountId);
        imapMailClient.markRead(account, decrypt(account), uid, uidValidity);
        firstPageCache.keySet().removeIf(key -> key.accountId().equals(accountId));
    }

    public void downloadAttachment(Long accountId, long uid, long uidValidity, String partId,
                                   HttpServletResponse response) {
        MailAccount account = getEnabledAccount(accountId);
        imapMailClient.downloadAttachment(account, decrypt(account), uid, uidValidity, partId, response);
    }

    private AccountPage listAccount(MailAccount account, int limit, AccountCursor cursor,
                                    boolean failFast, boolean refresh) {
        AccountCursor normalizedCursor = cursor == null ? new AccountCursor(0, 0) : cursor;
        FirstPageCacheKey cacheKey = new FirstPageCacheKey(account.getId(), limit);
        boolean firstPage = normalizedCursor.anchorUid() == 0 && normalizedCursor.offset() == 0;
        if (firstPage && !refresh) {
            CachedPage cachedPage = firstPageCache.get(cacheKey);
            if (cachedPage != null && cachedPage.isFresh()) {
                return new AccountPage(account.getId(), normalizedCursor, cachedPage.page());
            }
        }
        try {
            MailMessagePage page = imapMailClient.listPage(account, decrypt(account), limit,
                    normalizedCursor.anchorUid() > 0 ? normalizedCursor.anchorUid() : null,
                    normalizedCursor.offset());
            if (firstPage) {
                firstPageCache.put(cacheKey, new CachedPage(page, System.nanoTime()));
                firstPageCache.entrySet().removeIf(entry -> !entry.getValue().isFresh());
            }
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
        return credentialCipher.decrypt(
                MailConstants.MAIL_CREDENTIAL_PURPOSE, account.getAuthCodeCiphertext());
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

    private CachedDetail freshDetail(DetailCacheKey cacheKey) {
        CachedDetail cachedDetail = detailCache.get(cacheKey);
        if (cachedDetail != null && cachedDetail.isFresh()) {
            return cachedDetail;
        }
        if (cachedDetail != null) {
            detailCache.remove(cacheKey, cachedDetail);
        }
        return null;
    }

    private void cacheDetail(DetailCacheKey cacheKey, MailMessageDetailVo detail) {
        detailCache.entrySet().removeIf(entry -> !entry.getValue().isFresh());
        if (detailCache.size() >= DETAIL_CACHE_MAX_ENTRIES) {
            detailCache.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().cachedAtNanos()))
                    .ifPresent(entry -> detailCache.remove(entry.getKey(), entry.getValue()));
        }
        detailCache.put(cacheKey, new CachedDetail(detail, System.nanoTime()));
    }

    private record FirstPageCacheKey(Long accountId, int limit) {
    }

    private record DetailCacheKey(Long accountId, long uidValidity, long uid) {
    }

    private record CachedPage(MailMessagePage page, long cachedAtNanos) {
        private boolean isFresh() {
            return System.nanoTime() - cachedAtNanos < FIRST_PAGE_CACHE_NANOS;
        }
    }


    private record CachedDetail(MailMessageDetailVo detail, long cachedAtNanos) {
        private boolean isFresh() {
            return System.nanoTime() - cachedAtNanos < DETAIL_CACHE_NANOS;
        }
    }
}
