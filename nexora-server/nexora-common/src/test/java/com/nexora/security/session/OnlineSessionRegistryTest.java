package com.nexora.security.session;

import cn.dev33.satoken.dao.SaTokenDao;
import com.aurora.starter.redis.core.RedisCache;
import com.nexora.handler.onlineuser.OnlineSessionRecord;
import com.nexora.handler.onlineuser.OnlineSessionRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnlineSessionRegistryTest {

    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String DATA_KEY = "nexora:online-session:data:" + SESSION_ID;
    private static final String LAST_ACCESS_KEY = "nexora:online-session:last-access:" + SESSION_ID;
    private static final String TOUCH_KEY = "nexora:online-session:touch:" + SESSION_ID;
    private static final String INDEX_KEY = "nexora:online-session:index";
    private static final String USER_KEY = "nexora:online-session:user:7";
    private static final String SECOND_USER_KEY = "nexora:online-session:user:8";
    private static final String USER_KEY_PATTERN = "nexora:online-session:user:*";
    private static final LocalDateTime LOGIN_TIME = LocalDateTime.of(2026, 8, 4, 10, 30);

    private final RedisCache redisCache = mock(RedisCache.class);
    private final OnlineSessionRegistry registry = new OnlineSessionRegistry(redisCache);

    @Test
    void registersCredentialFreeSessionWithTokenTtlAndIndexes() {
        OnlineSessionRecord record = record();
        long loginMillis = LOGIN_TIME.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        registry.register(record, 120);

        verify(redisCache).setCacheObject(DATA_KEY, record, 120, TimeUnit.SECONDS);
        verify(redisCache).setCacheObject(LAST_ACCESS_KEY, loginMillis, 120, TimeUnit.SECONDS);
        verify(redisCache).addZset(INDEX_KEY, SESSION_ID, loginMillis);
        verify(redisCache).addCacheSet(USER_KEY, SESSION_ID);
    }

    @Test
    void preservesNeverExpireTokenSemantics() {
        OnlineSessionRecord record = record();

        registry.register(record, SaTokenDao.NEVER_EXPIRE);

        verify(redisCache).setCacheObject(DATA_KEY, record);
        verify(redisCache).setCacheObject(
                LAST_ACCESS_KEY,
                LOGIN_TIME.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    @Test
    void rejectsExpiredTokenTimeouts() {
        assertThatThrownBy(() -> registry.register(record(), SaTokenDao.NOT_VALUE_EXPIRE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void batchReadsRecordsWithoutLosingSessionIdAssociation() {
        String secondId = "550e8400-e29b-41d4-a716-446655440001";
        OnlineSessionRecord first = record();
        OnlineSessionRecord second = new OnlineSessionRecord(
                secondId, 8, "second@example.com", null, "10.0.0.2",
                null, null, LOGIN_TIME.plusMinutes(1));
        LinkedHashSet<String> keys = new LinkedHashSet<>(List.of(
                DATA_KEY, "nexora:online-session:data:" + secondId));
        when(redisCache.multiGet(keys)).thenReturn(List.of(first, second));

        Map<String, OnlineSessionRecord> result =
                registry.findAll(List.of(SESSION_ID, secondId));

        assertThat(result).containsEntry(SESSION_ID, first).containsEntry(secondId, second);
    }

    @Test
    void returnsSessionIdsInReverseScoreOrder() {
        when(redisCache.getCacheReverseZSet(INDEX_KEY))
                .thenReturn(new LinkedHashSet<>(List.of("new", "old")));

        assertThat(registry.listSessionIds()).containsExactly("new", "old");
    }

    @Test
    void throttlesLastAccessWritesAndCapsWindowAtRemainingTokenTtl() {
        when(redisCache.exists(DATA_KEY)).thenReturn(true);
        when(redisCache.setIfAbsent(TOUCH_KEY, "1", 30, TimeUnit.SECONDS))
                .thenReturn(true);

        assertThat(registry.touch(SESSION_ID, 1234L, 30)).isTrue();

        verify(redisCache).setCacheObject(LAST_ACCESS_KEY, 1234L, 30, TimeUnit.SECONDS);
    }

    @Test
    void skipsTouchWhenThrottleWindowAlreadyExists() {
        when(redisCache.exists(DATA_KEY)).thenReturn(true);
        when(redisCache.setIfAbsent(TOUCH_KEY, "1", 60, TimeUnit.SECONDS))
                .thenReturn(false);

        assertThat(registry.touch(SESSION_ID, 1234L, 300)).isFalse();
    }

    @Test
    void allowsTouchAgainAfterTheThrottleWindowExpires() {
        when(redisCache.exists(DATA_KEY)).thenReturn(true);
        when(redisCache.setIfAbsent(TOUCH_KEY, "1", 60, TimeUnit.SECONDS))
                .thenReturn(true, false, true);

        assertThat(registry.touch(SESSION_ID, 1000L, 300)).isTrue();
        assertThat(registry.touch(SESSION_ID, 2000L, 300)).isFalse();
        assertThat(registry.touch(SESSION_ID, 3000L, 300)).isTrue();

        verify(redisCache).setCacheObject(LAST_ACCESS_KEY, 1000L, 300, TimeUnit.SECONDS);
        verify(redisCache, never())
                .setCacheObject(LAST_ACCESS_KEY, 2000L, 300, TimeUnit.SECONDS);
        verify(redisCache).setCacheObject(LAST_ACCESS_KEY, 3000L, 300, TimeUnit.SECONDS);
    }

    @Test
    void skipsUnregisteredSessionsWithoutCreatingThrottleState() {
        when(redisCache.exists(DATA_KEY)).thenReturn(false);

        assertThat(registry.touch(SESSION_ID, 1234L, 300)).isFalse();

        verify(redisCache, never())
                .setIfAbsent(TOUCH_KEY, "1", 60, TimeUnit.SECONDS);
        verify(redisCache, never())
                .setCacheObject(LAST_ACCESS_KEY, 1234L, 300, TimeUnit.SECONDS);
    }

    @Test
    void removesAllKeysAndIndexesForOneSession() {
        when(redisCache.getCacheSet(USER_KEY)).thenReturn(Set.of());

        registry.remove(SESSION_ID, 7);

        verify(redisCache).deleteObject(List.of(DATA_KEY, LAST_ACCESS_KEY, TOUCH_KEY));
        verify(redisCache).removeZset(INDEX_KEY, SESSION_ID);
        verify(redisCache).removeSet(USER_KEY, SESSION_ID);
        verify(redisCache).deleteObject(USER_KEY);
    }

    @Test
    void removesStaleSessionsWithOneScanAndOneReadPerUserIndex() {
        String secondId = "550e8400-e29b-41d4-a716-446655440001";
        when(redisCache.scan(USER_KEY_PATTERN)).thenReturn(Set.of(USER_KEY, SECOND_USER_KEY));
        when(redisCache.getCacheSet(USER_KEY)).thenReturn(Set.of(SESSION_ID, "another-session"));
        when(redisCache.getCacheSet(SECOND_USER_KEY)).thenReturn(Set.of(secondId));

        registry.removeStaleSessions(List.of(SESSION_ID, secondId));

        verify(redisCache).removeSet(USER_KEY, SESSION_ID);
        verify(redisCache).removeSet(SECOND_USER_KEY, secondId);
        verify(redisCache, times(1)).scan(USER_KEY_PATTERN);
        verify(redisCache, times(1)).getCacheSet(USER_KEY);
        verify(redisCache, times(1)).getCacheSet(SECOND_USER_KEY);
    }

    @Test
    void removesAllSessionsForOneUser() {
        String secondId = "550e8400-e29b-41d4-a716-446655440001";
        when(redisCache.getCacheSet(USER_KEY)).thenReturn(Set.of(SESSION_ID, secondId));

        registry.removeByUserId(7);

        verify(redisCache).removeZset(INDEX_KEY, SESSION_ID);
        verify(redisCache).removeZset(INDEX_KEY, secondId);
        verify(redisCache).deleteObject(USER_KEY);
    }

    private static OnlineSessionRecord record() {
        return new OnlineSessionRecord(
                SESSION_ID,
                7,
                "user@example.com",
                "User",
                "127.0.0.1",
                "Chrome",
                "Windows",
                LOGIN_TIME);
    }
}
