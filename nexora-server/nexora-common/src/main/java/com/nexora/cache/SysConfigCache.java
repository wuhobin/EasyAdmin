package com.nexora.cache;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.nexora.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class SysConfigCache {

    static final String CACHE_NAME = "sysConfig";
    private static final long CACHE_TTL_MINUTES = 5;

    private final TwoLevelCacheTemplate cacheTemplate;

    public String get(String configKey, Supplier<String> loader) {
        return cacheTemplate.get(
                CACHE_NAME, redisKey(configKey), loader, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void evictAfterCommit(String configKey) {
        cacheTemplate.evictAfterCommit(CACHE_NAME, redisKey(configKey));
    }

    public void setAfterCommit(String configKey, String configValue) {
        cacheTemplate.setAfterCommit(
                CACHE_NAME, redisKey(configKey), configValue, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private static String redisKey(String configKey) {
        return RedisKeyUtil.generate(RedisConstants.SYS_CONFIG_KEY, configKey);
    }
}
