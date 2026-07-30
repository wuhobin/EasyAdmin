package com.nexora.cache;

import com.aurora.starter.redis.core.TwoLevelCache;
import com.aurora.starter.redis.core.manager.TwoLevelCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TwoLevelCacheTemplateTest {

    private static final String CACHE_NAME = "featureFlags";
    private static final String CACHE_KEY = "nexora:feature-flag:dark-mode";

    private final TwoLevelCacheManager cacheManager = mock(TwoLevelCacheManager.class);
    private final TwoLevelCache twoLevelCache = mock(TwoLevelCache.class);
    private final TwoLevelCacheTemplate cacheTemplate = new TwoLevelCacheTemplate(cacheManager);

    @Test
    void supportsArbitraryCachedValueTypes() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        when(twoLevelCache.<Integer>get(eq(CACHE_KEY), any(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(42);

        Integer value = cacheTemplate.get(
                CACHE_NAME, CACHE_KEY, () -> 0, 30L, TimeUnit.SECONDS);

        assertThat(value).isEqualTo(42);
    }

    @Test
    void fallsBackToLoaderWhenRedisIsUnavailable() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        when(twoLevelCache.get(eq(CACHE_KEY), any(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        String value = cacheTemplate.get(
                CACHE_NAME, CACHE_KEY, () -> "enabled", 30L, TimeUnit.SECONDS);

        assertThat(value).isEqualTo("enabled");
    }

    @Test
    void propagatesFailuresThatAreNotCausedByRedis() {
        AtomicInteger loaderCalls = new AtomicInteger();
        Supplier<String> loader = () -> {
            loaderCalls.incrementAndGet();
            return "enabled";
        };
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        when(twoLevelCache.get(eq(CACHE_KEY), any(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenThrow(new IllegalStateException("invalid cache configuration"));

        assertThatThrownBy(() -> cacheTemplate.get(
                CACHE_NAME, CACHE_KEY, loader, 30L, TimeUnit.SECONDS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("invalid cache configuration");
        assertThat(loaderCalls).hasValue(0);
    }

    @Test
    void writesAndEvictsImmediatelyWithoutAnActiveTransaction() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);

        cacheTemplate.setAfterCommit(
                CACHE_NAME, CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
        cacheTemplate.evictAfterCommit(CACHE_NAME, CACHE_KEY);

        verify(twoLevelCache).set(CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
        verify(twoLevelCache).evict(CACHE_KEY);
    }

    @Test
    void defersWritesAndEvictionsUntilTheActiveTransactionCommits() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            cacheTemplate.setAfterCommit(
                    CACHE_NAME, CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
            cacheTemplate.evictAfterCommit(CACHE_NAME, CACHE_KEY);

            verifyNoInteractions(cacheManager);
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(twoLevelCache).set(CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
            verify(twoLevelCache).evict(CACHE_KEY);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void ignoresCacheWriteAndEvictionFailures() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        doThrow(new RedisConnectionFailureException("redis unavailable"))
                .when(twoLevelCache).set(CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
        doThrow(new RedisConnectionFailureException("redis unavailable"))
                .when(twoLevelCache).evict(CACHE_KEY);

        assertThatCode(() -> {
            cacheTemplate.set(CACHE_NAME, CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
            cacheTemplate.evict(CACHE_NAME, CACHE_KEY);
        }).doesNotThrowAnyException();
    }
}
