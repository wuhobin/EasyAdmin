package com.nexora.identity.cache;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.security.account.AccountType;
import com.nexora.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAuthorizationCache {

    private static final long CACHE_TTL_HOURS = 24;

    private final RedisCache redisCache;

    public Authorization get(Integer userId, AccountType accountType,
                             Supplier<Authorization> loader) {
        String key = redisKey(userId, accountType);
        try {
            Authorization cached = redisCache.getCacheObject(key);
            if (cached != null) {
                return cached;
            }
        } catch (RuntimeException exception) {
            log.warn("Redis unavailable while reading authorization cache for user [{}]", userId, exception);
            return loader.get();
        }

        Authorization authorization = loader.get();
        try {
            redisCache.setCacheObject(key, authorization, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (RuntimeException exception) {
            log.warn("Failed to write authorization cache for user [{}]", userId, exception);
        }
        return authorization;
    }

    public void evictUsersAfterCommit(Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<Integer> ids = userIds.stream().distinct().toList();
        runAfterCommit(() -> safeEvictUsers(ids));
    }

    public void evictAllAfterCommit() {
        runAfterCommit(this::safeEvictAll);
    }

    private void safeEvictUsers(Collection<Integer> userIds) {
        try {
            List<String> keys = userIds.stream()
                    .flatMap(userId -> Arrays.stream(AccountType.values())
                            .map(accountType -> redisKey(userId, accountType)))
                    .toList();
            redisCache.deleteObject(keys);
        } catch (RuntimeException exception) {
            log.warn("Failed to evict authorization cache for users {}", userIds, exception);
        }
    }

    private void safeEvictAll() {
        try {
            redisCache.deleteByPattern(RedisKeyUtil.generate(
                    RedisConstants.SECURITY_PERMISSION_LIST_KEY, "*"));
        } catch (RuntimeException exception) {
            log.warn("Failed to evict all authorization caches", exception);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private static String redisKey(Integer userId, AccountType accountType) {
        return RedisKeyUtil.generate(RedisConstants.SECURITY_PERMISSION_LIST_KEY,
                accountType.getCode(), userId.toString());
    }

    public record Authorization(List<String> roles, List<String> permissions) {
        public Authorization {
            roles = roles == null ? List.of()
                    : roles.stream().filter(Objects::nonNull).toList();
            permissions = permissions == null ? List.of()
                    : permissions.stream().filter(Objects::nonNull).toList();
        }
    }
}
