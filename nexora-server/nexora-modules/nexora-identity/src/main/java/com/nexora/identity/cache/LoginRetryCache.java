package com.nexora.identity.cache;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.core.RedisCache;
import com.nexora.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class LoginRetryCache {

    private final RedisCache redisCache;

    public int getFailureCount(String normalizedEmail) {
        Object value = redisCache.getCacheObject(redisKey(normalizedEmail));
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    public int recordFailure(String normalizedEmail, int lockTimeMinutes) {
        Long count = redisCache.increment(
                redisKey(normalizedEmail), 1, lockTimeMinutes, TimeUnit.MINUTES);
        return count == null ? 0 : count.intValue();
    }

    public long getRemainingMinutes(String normalizedEmail) {
        Long seconds = redisCache.getExpire(redisKey(normalizedEmail), TimeUnit.SECONDS);
        if (seconds == null || seconds <= 0) {
            return 1;
        }
        return Math.max(1, (seconds + 59) / 60);
    }

    public void clear(String normalizedEmail) {
        redisCache.deleteObject(redisKey(normalizedEmail));
    }

    private static String redisKey(String normalizedEmail) {
        return RedisKeyUtil.generate(RedisConstants.LOGIN_RETRY_KEY, normalizedEmail);
    }
}
