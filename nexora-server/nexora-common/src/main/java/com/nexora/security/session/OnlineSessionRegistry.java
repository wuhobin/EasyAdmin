package com.nexora.security.session;

import cn.dev33.satoken.dao.SaTokenDao;
import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.core.RedisCache;
import com.nexora.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed registry for credential-free online session metadata.
 */
@Component
@RequiredArgsConstructor
public class OnlineSessionRegistry {

    private static final String TOUCH_MARKER = "1";

    private final RedisCache redisCache;

    public void register(OnlineSessionRecord record, long tokenTimeoutSeconds) {
        requireUsableTimeout(tokenTimeoutSeconds);
        String sessionId = record.sessionId();
        try {
            setWithTokenTimeout(dataKey(sessionId), record, tokenTimeoutSeconds);
            setWithTokenTimeout(lastAccessKey(sessionId), loginTimeMillis(record), tokenTimeoutSeconds);
            redisCache.addZset(indexKey(), sessionId, loginTimeMillis(record));
            redisCache.addCacheSet(userKey(record.userId()), sessionId);
        } catch (RuntimeException exception) {
            try {
                remove(sessionId, record.userId());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    public Optional<OnlineSessionRecord> find(String sessionId) {
        return Optional.ofNullable(redisCache.getCacheObject(dataKey(sessionId)));
    }

    public List<String> listSessionIds() {
        Set<Object> values = redisCache.getCacheReverseZSet(indexKey());
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }

    public Map<String, OnlineSessionRecord> findAll(Collection<String> sessionIds) {
        return multiGet(sessionIds, OnlineSessionRegistry::dataKey, OnlineSessionRecord.class);
    }

    public Optional<Long> findLastAccessTime(String sessionId) {
        return Optional.ofNullable(toLong(redisCache.getCacheObject(lastAccessKey(sessionId))));
    }

    public Map<String, Long> findLastAccessTimes(Collection<String> sessionIds) {
        return multiGet(sessionIds, OnlineSessionRegistry::lastAccessKey, Long.class);
    }

    /**
     * Updates a session's last-access timestamp only when its throttle window can be acquired.
     *
     * @return true when the timestamp was written; false when the record is absent or throttled
     */
    public boolean touch(String sessionId, long accessTimeMillis, long tokenTimeoutSeconds) {
        requireUsableTimeout(tokenTimeoutSeconds);
        if (!redisCache.exists(dataKey(sessionId))) {
            return false;
        }

        long throttleSeconds = tokenTimeoutSeconds == SaTokenDao.NEVER_EXPIRE
                ? RedisConstants.ONLINE_SESSION_TOUCH_INTERVAL_SECONDS
                : Math.min(RedisConstants.ONLINE_SESSION_TOUCH_INTERVAL_SECONDS, tokenTimeoutSeconds);
        Boolean acquired = redisCache.setIfAbsent(
                touchKey(sessionId), TOUCH_MARKER, throttleSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            return false;
        }

        try {
            setWithTokenTimeout(lastAccessKey(sessionId), accessTimeMillis, tokenTimeoutSeconds);
            return true;
        } catch (RuntimeException exception) {
            redisCache.deleteObject(touchKey(sessionId));
            throw exception;
        }
    }

    public void remove(String sessionId) {
        Integer userId = find(sessionId).map(OnlineSessionRecord::userId).orElse(null);
        if (userId == null) {
            removeStaleSessions(List.of(sessionId));
            return;
        }
        remove(sessionId, userId);
    }

    public void remove(String sessionId, Integer userId) {
        if (userId == null) {
            removeStaleSessions(List.of(sessionId));
            return;
        }
        removeSessionState(List.of(sessionId));
        removeFromUserIndex(userKey(userId), sessionId);
    }

    public void removeStaleSessions(Collection<String> sessionIds) {
        List<String> staleSessionIds = normalizedSessionIds(sessionIds);
        if (staleSessionIds.isEmpty()) {
            return;
        }

        removeSessionState(staleSessionIds);
        removeFromAllUserIndexes(new LinkedHashSet<>(staleSessionIds));
    }

    public void removeByUserId(Integer userId) {
        Set<Object> values = redisCache.getCacheSet(userKey(userId));
        if (values == null || values.isEmpty()) {
            redisCache.deleteObject(userKey(userId));
            return;
        }

        List<String> sessionIds = values.stream().map(String::valueOf).toList();
        removeSessionState(sessionIds);
        redisCache.deleteObject(userKey(userId));
    }

    private <T> Map<String, T> multiGet(
            Collection<String> sessionIds,
            java.util.function.Function<String, String> keyFactory,
            Class<T> valueType) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }

        List<String> orderedSessionIds = normalizedSessionIds(sessionIds);
        if (orderedSessionIds.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        orderedSessionIds.forEach(id -> keys.add(keyFactory.apply(id)));
        List<Object> values = redisCache.multiGet(keys);
        if (values == null) {
            values = Collections.emptyList();
        }

        Map<String, T> result = new LinkedHashMap<>();
        for (int index = 0; index < orderedSessionIds.size(); index++) {
            Object value = index < values.size() ? values.get(index) : null;
            T converted = convertValue(value, valueType);
            if (converted != null) {
                result.put(orderedSessionIds.get(index), converted);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static <T> T convertValue(Object value, Class<T> valueType) {
        if (value == null) {
            return null;
        }
        if (valueType == Long.class) {
            return valueType.cast(toLong(value));
        }
        return valueType.isInstance(value) ? valueType.cast(value) : null;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(value.toString());
    }

    private <T> void setWithTokenTimeout(String key, T value, long tokenTimeoutSeconds) {
        if (tokenTimeoutSeconds == SaTokenDao.NEVER_EXPIRE) {
            redisCache.setCacheObject(key, value);
            return;
        }
        redisCache.setCacheObject(key, value, tokenTimeoutSeconds, TimeUnit.SECONDS);
    }

    private static void requireUsableTimeout(long tokenTimeoutSeconds) {
        if (tokenTimeoutSeconds != SaTokenDao.NEVER_EXPIRE && tokenTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("token timeout must be positive or never expire");
        }
    }

    private static long loginTimeMillis(OnlineSessionRecord record) {
        return record.loginTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private static List<String> normalizedSessionIds(Collection<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    private void removeSessionState(Collection<String> sessionIds) {
        List<String> keys = new ArrayList<>(sessionIds.size() * 3);
        for (String sessionId : sessionIds) {
            keys.add(dataKey(sessionId));
            keys.add(lastAccessKey(sessionId));
            keys.add(touchKey(sessionId));
            redisCache.removeZset(indexKey(), sessionId);
        }
        redisCache.deleteObject(keys);
    }

    private void removeFromAllUserIndexes(Set<String> sessionIds) {
        Collection<String> userKeys = redisCache.scan(userKeyPattern());
        if (userKeys == null || userKeys.isEmpty()) {
            return;
        }
        for (String key : userKeys) {
            Set<Object> members = redisCache.getCacheSet(key);
            if (members == null || members.isEmpty()) {
                redisCache.deleteObject(key);
                continue;
            }
            members.stream()
                    .filter(member -> sessionIds.contains(String.valueOf(member)))
                    .forEach(member -> redisCache.removeSet(key, member));
        }
    }

    private void removeFromUserIndex(String key, String sessionId) {
        redisCache.removeSet(key, sessionId);
        Set<Object> remaining = redisCache.getCacheSet(key);
        if (remaining == null || remaining.isEmpty()) {
            redisCache.deleteObject(key);
        }
    }

    static String dataKey(String sessionId) {
        return RedisKeyUtil.generate(RedisConstants.ONLINE_SESSION_DATA_KEY, sessionId);
    }

    static String lastAccessKey(String sessionId) {
        return RedisKeyUtil.generate(RedisConstants.ONLINE_SESSION_LAST_ACCESS_KEY, sessionId);
    }

    static String touchKey(String sessionId) {
        return RedisKeyUtil.generate(RedisConstants.ONLINE_SESSION_TOUCH_KEY, sessionId);
    }

    static String indexKey() {
        return RedisKeyUtil.generate(RedisConstants.ONLINE_SESSION_INDEX_KEY);
    }

    static String userKey(Integer userId) {
        return RedisKeyUtil.generate(RedisConstants.ONLINE_SESSION_USER_KEY, userId.toString());
    }

    static String userKeyPattern() {
        return RedisKeyUtil.generate(RedisConstants.ONLINE_SESSION_USER_KEY, "*");
    }
}
