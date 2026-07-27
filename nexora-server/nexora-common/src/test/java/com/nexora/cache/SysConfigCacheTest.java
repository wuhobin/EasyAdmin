package com.nexora.cache;

import com.aurora.starter.redis.core.TwoLevelCache;
import com.aurora.starter.redis.core.manager.TwoLevelCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SysConfigCacheTest {

    private final TwoLevelCacheManager cacheManager = mock(TwoLevelCacheManager.class);
    private final TwoLevelCache twoLevelCache = mock(TwoLevelCache.class);
    private final SysConfigCache configCache = new SysConfigCache(cacheManager);

    @Test
    void fallsBackToDatabaseLoaderWhenRedisIsUnavailable() {
        when(cacheManager.get("sysConfig")).thenReturn(twoLevelCache);
        when(twoLevelCache.get(eq("nexora:sys-config:site.title"), any(),
                eq(5L), eq(TimeUnit.MINUTES)))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));
        AtomicInteger databaseLoads = new AtomicInteger();

        String value = configCache.get("site.title", () -> {
            databaseLoads.incrementAndGet();
            return "Nexora";
        });

        assertThat(value).isEqualTo("Nexora");
        assertThat(databaseLoads).hasValue(1);
    }

    @Test
    void evictsOnlyAfterActiveTransactionCommits() {
        when(cacheManager.get("sysConfig")).thenReturn(twoLevelCache);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            configCache.evictAfterCommit("site.title");

            verifyNoInteractions(cacheManager);
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(twoLevelCache).evict("nexora:sys-config:site.title");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void writesValueOnlyAfterActiveTransactionCommits() {
        when(cacheManager.get("sysConfig")).thenReturn(twoLevelCache);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            configCache.setAfterCommit("site.title", "Nexora");

            verifyNoInteractions(cacheManager);
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(twoLevelCache).set(
                    "nexora:sys-config:site.title", "Nexora", 5L, TimeUnit.MINUTES);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}
