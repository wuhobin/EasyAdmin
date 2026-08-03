package com.nexora.system.cache;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.nexora.cache.TwoLevelCacheTemplate;
import com.nexora.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class SysConfigGroupCache {

    static final String CACHE_NAME = "sysConfigGroup";
    private static final long CACHE_TTL_DAYS = 3;

    private final TwoLevelCacheTemplate cacheTemplate;

    public String get(String groupCode, Supplier<String> loader) {
        return cacheTemplate.get(
                CACHE_NAME, redisKey(groupCode), loader, CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    public void prepareUpdate(String groupCode) {
        cacheTemplate.evictRequired(CACHE_NAME, redisKey(groupCode));
    }

    public void refreshAfterCommit(String groupCode, String configValue) {
        cacheTemplate.replaceAfterCommitBestEffort(
                CACHE_NAME, redisKey(groupCode), configValue, CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    public void setRequired(String groupCode, String configValue) {
        cacheTemplate.setRequired(
                CACHE_NAME, redisKey(groupCode), configValue, CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    private static String redisKey(String groupCode) {
        return RedisKeyUtil.generate(RedisConstants.SYS_CONFIG_GROUP_KEY, groupCode);
    }
}
