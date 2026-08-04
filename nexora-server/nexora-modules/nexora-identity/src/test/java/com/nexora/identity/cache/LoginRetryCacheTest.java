package com.nexora.identity.cache;

import com.aurora.starter.redis.core.RedisCache;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginRetryCacheTest {

    private static final String EMAIL = "user@example.com";
    private static final String KEY = "nexora:login-retry:user@example.com";

    private final RedisCache redisCache = mock(RedisCache.class);
    private final LoginRetryCache cache = new LoginRetryCache(redisCache);

    @Test
    void readsNumericAndStringFailureCountsFromTheNormalizedEmailKey() {
        when(redisCache.getCacheObject(KEY)).thenReturn(2, "3", null);

        assertThat(cache.getFailureCount(EMAIL)).isEqualTo(2);
        assertThat(cache.getFailureCount(EMAIL)).isEqualTo(3);
        assertThat(cache.getFailureCount(EMAIL)).isZero();
    }

    @Test
    void incrementsTheFailureCountWithTheConfiguredLockTtl() {
        when(redisCache.increment(KEY, 1, 30, TimeUnit.MINUTES)).thenReturn(4L);

        assertThat(cache.recordFailure(EMAIL, 30)).isEqualTo(4);
        verify(redisCache).increment(KEY, 1, 30, TimeUnit.MINUTES);
    }

    @Test
    void roundsTheRemainingLockTimeUpToWholeMinutes() {
        when(redisCache.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(61L, 0L);

        assertThat(cache.getRemainingMinutes(EMAIL)).isEqualTo(2);
        assertThat(cache.getRemainingMinutes(EMAIL)).isEqualTo(1);
    }

    @Test
    void clearsTheNormalizedEmailKeyAfterSuccessfulLogin() {
        cache.clear(EMAIL);

        verify(redisCache).deleteObject(KEY);
    }
}
