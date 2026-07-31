package com.nexora.cache;

import com.aurora.starter.redis.core.TwoLevelCache;
import com.aurora.starter.redis.core.manager.TwoLevelCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 通用二级缓存操作模板。
 * <p>
 * 读取缓存时 Redis 不可用会回退到数据加载器。修改方法通过 Required 与
 * BestEffort 后缀明确区分异常是否向上传递，并支持在当前事务提交后执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwoLevelCacheTemplate {

    private final TwoLevelCacheManager cacheManager;

    public <T> T get(String cacheName, String cacheKey, Supplier<T> loader,
                     long ttl, TimeUnit timeUnit) {
        try {
            return cache(cacheName).get(cacheKey, loader, ttl, timeUnit);
        } catch (RuntimeException exception) {
            if (!isRedisFailure(exception)) {
                throw exception;
            }
            log.warn("Redis unavailable while reading two-level cache [{}] key [{}], falling back to loader",
                    cacheName, cacheKey, exception);
            return loader.get();
        }
    }

    public void setBestEffort(String cacheName, String cacheKey, Object value,
                              long ttl, TimeUnit timeUnit) {
        try {
            cache(cacheName).set(cacheKey, value, ttl, timeUnit);
        } catch (RuntimeException exception) {
            log.warn("Failed to write two-level cache [{}] key [{}]", cacheName, cacheKey, exception);
        }
    }

    public void setRequired(String cacheName, String cacheKey, Object value,
                            long ttl, TimeUnit timeUnit) {
        cache(cacheName).set(cacheKey, value, ttl, timeUnit);
    }

    public void evictBestEffort(String cacheName, String cacheKey) {
        try {
            cache(cacheName).evict(cacheKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to evict two-level cache [{}] key [{}]", cacheName, cacheKey, exception);
        }
    }

    public void evictRequired(String cacheName, String cacheKey) {
        cache(cacheName).evict(cacheKey);
    }

    public void evictAfterCommitBestEffort(String cacheName, String cacheKey) {
        runAfterCommit(() -> evictBestEffort(cacheName, cacheKey));
    }

    public void replaceAfterCommitBestEffort(String cacheName, String cacheKey, Object value,
                                             long ttl, TimeUnit timeUnit) {
        runAfterCommit(() -> replaceBestEffort(cacheName, cacheKey, value, ttl, timeUnit));
    }

    private void replaceBestEffort(String cacheName, String cacheKey, Object value,
                                   long ttl, TimeUnit timeUnit) {
        try {
            evictRequired(cacheName, cacheKey);
            setRequired(cacheName, cacheKey, value, ttl, timeUnit);
        } catch (RuntimeException exception) {
            log.error("Failed to replace two-level cache [{}] key [{}] after database commit",
                    cacheName, cacheKey, exception);
            evictBestEffort(cacheName, cacheKey);
        }
    }

    private TwoLevelCache cache(String cacheName) {
        return cacheManager.get(cacheName);
    }

    private static void runAfterCommit(Runnable action) {
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
