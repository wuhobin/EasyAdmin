package com.nexora.cache;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.core.TwoLevelCache;
import com.aurora.starter.redis.core.manager.TwoLevelCacheManager;
import com.nexora.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class SysConfigCache {

    static final String CACHE_NAME = "sysConfig";
    private static final long CACHE_TTL_MINUTES = 5;

    private final TwoLevelCacheManager cacheManager;

    public String get(String configKey, Supplier<String> loader) {
        try {
            return cache().get(redisKey(configKey), loader, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (RuntimeException exception) {
            if (!isRedisFailure(exception)) {
                throw exception;
            }
            log.warn("Redis unavailable while reading system config [{}], falling back to database",
                    configKey, exception);
            return loader.get();
        }
    }

    public void evictAfterCommit(String configKey) {
        runAfterCommit(() -> safeEvict(configKey));
    }

    public void setAfterCommit(String configKey, String configValue) {
        runAfterCommit(() -> safeSet(configKey, configValue));
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

    private void safeSet(String configKey, String configValue) {
        try {
            cache().set(redisKey(configKey), configValue, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (RuntimeException exception) {
            log.warn("Failed to write system config cache [{}]; database change remains committed",
                    configKey, exception);
        }
    }

    private void safeEvict(String configKey) {
        try {
            cache().evict(redisKey(configKey));
        } catch (RuntimeException exception) {
            log.warn("Failed to evict system config cache [{}]; database change remains committed",
                    configKey, exception);
        }
    }

    private TwoLevelCache cache() {
        return cacheManager.get(CACHE_NAME);
    }

    private static String redisKey(String configKey) {
        return RedisKeyUtil.generate(RedisConstants.SYS_CONFIG_KEY, configKey);
    }

    private static boolean isRedisFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            if (className.startsWith("org.springframework.data.redis.")
                    || className.startsWith("org.redisson.")
                    || className.startsWith("io.lettuce.core.")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
