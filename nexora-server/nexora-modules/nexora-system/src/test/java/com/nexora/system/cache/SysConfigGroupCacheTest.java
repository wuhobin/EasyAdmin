package com.nexora.system.cache;

import com.aurora.starter.redis.core.TwoLevelCacheTemplate;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysConfigGroupCacheTest {

    private final TwoLevelCacheTemplate cacheTemplate = mock(TwoLevelCacheTemplate.class);
    private final SysConfigGroupCache configCache = new SysConfigGroupCache(cacheTemplate);

    @Test
    void readsAWholeGroupWithAThreeDayTtl() {
        Supplier<String> loader = () -> "{\"enabled\":true}";
        when(cacheTemplate.get(
                "sysConfigGroup", "nexora:sys-config-group:register",
                loader, 3L, TimeUnit.DAYS)).thenReturn("{\"enabled\":true}");

        String value = configCache.get("register", loader);

        assertThat(value).isEqualTo("{\"enabled\":true}");
        verify(cacheTemplate).get(
                "sysConfigGroup", "nexora:sys-config-group:register",
                loader, 3L, TimeUnit.DAYS);
    }

    @Test
    void strictlyEvictsBeforeUpdatingTheDatabase() {
        configCache.prepareUpdate("system");

        verify(cacheTemplate).evictRequired(
                "sysConfigGroup", "nexora:sys-config-group:system");
    }

    @Test
    void writesTheCommittedGroupWithAThreeDayTtl() {
        configCache.refreshAfterCommit("system", "{\"siteName\":\"Nexora\"}");

        verify(cacheTemplate).replaceAfterCommitBestEffort(
                "sysConfigGroup", "nexora:sys-config-group:system",
                "{\"siteName\":\"Nexora\"}", 3L, TimeUnit.DAYS);
    }
}
