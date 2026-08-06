package com.nexora.identity.cache;

import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.security.account.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SecurityAuthorizationCacheTest {

    private final RedisCache redisCache = mock(RedisCache.class);
    private final SecurityPermissionCache cache = new SecurityPermissionCache(redisCache);

    @Test
    void returnsCachedAuthorizationWithoutLoadingDatabase() {
        SecurityPermissionCache.Authorization authorization = authorization();
        when(redisCache.getCacheObject("nexora:user:permission-list:login:7"))
                .thenReturn(authorization);
        AtomicInteger loads = new AtomicInteger();

        SecurityPermissionCache.Authorization result = cache.get(7, AccountType.LOGIN, () -> {
            loads.incrementAndGet();
            return authorization();
        });

        assertThat(result).isSameAs(authorization);
        assertThat(loads).hasValue(0);
    }

    @Test
    void loadsDatabaseAndWritesRedisOnCacheMiss() {
        SecurityPermissionCache.Authorization authorization = authorization();

        SecurityPermissionCache.Authorization result =
                cache.get(7, AccountType.LOGIN, () -> authorization);

        assertThat(result).isSameAs(authorization);
        verify(redisCache).setCacheObject("nexora:user:permission-list:login:7",
                authorization, 24L, TimeUnit.HOURS);
    }

    @Test
    void fallsBackToDatabaseWhenRedisIsUnavailable() {
        when(redisCache.getCacheObject("nexora:user:permission-list:login:7"))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        SecurityPermissionCache.Authorization result =
                cache.get(7, AccountType.LOGIN, SecurityAuthorizationCacheTest::authorization);

        assertThat(result.roles()).containsExactly("admin");
    }

    @Test
    void authorizationCanRoundTripThroughStarterJsonSerializer() {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer();
        SecurityPermissionCache.Authorization authorization = authorization();

        Object restored = serializer.deserialize(serializer.serialize(authorization));

        assertThat(restored).isEqualTo(authorization);
    }

    @Test
    void evictsUsersOnlyAfterTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            cache.evictUsersAfterCommit(List.of(7));

            verifyNoInteractions(redisCache);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(redisCache).deleteObject(List.of(
                    "nexora:user:permission-list:login:7",
                    "nexora:user:permission-list:user:7",
                    "nexora:user:permission-list:admin:7",
                    "nexora:user:permission-list:merchant:7"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void evictsAllAuthorizationKeysOnlyAfterTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            cache.evictAllAfterCommit();

            verifyNoInteractions(redisCache);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(redisCache).deleteByPattern("nexora:user:permission-list:*");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private static SecurityPermissionCache.Authorization authorization() {
        return new SecurityPermissionCache.Authorization(
                List.of("admin"), List.of("sys:config:list"));
    }
}
