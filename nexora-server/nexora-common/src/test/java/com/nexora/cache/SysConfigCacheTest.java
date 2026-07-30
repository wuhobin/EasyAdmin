package com.nexora.cache;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysConfigCacheTest {

    private final TwoLevelCacheTemplate cacheTemplate = mock(TwoLevelCacheTemplate.class);
    private final SysConfigCache configCache = new SysConfigCache(cacheTemplate);

    @Test
    void readsFromTheSystemConfigCacheRegion() {
        Supplier<String> loader = () -> "database value";
        when(cacheTemplate.get(
                "sysConfig", "nexora:sys-config:site.title", loader, 5L, TimeUnit.MINUTES))
                .thenReturn("Nexora");

        String value = configCache.get("site.title", loader);

        assertThat(value).isEqualTo("Nexora");
        verify(cacheTemplate).get(
                "sysConfig", "nexora:sys-config:site.title", loader, 5L, TimeUnit.MINUTES);
    }

    @Test
    void evictsFromTheSystemConfigCacheRegionAfterCommit() {
        configCache.evictAfterCommit("site.title");

        verify(cacheTemplate).evictAfterCommit(
                "sysConfig", "nexora:sys-config:site.title");
    }

    @Test
    void writesToTheSystemConfigCacheRegionAfterCommit() {
        configCache.setAfterCommit("site.title", "Nexora");

        verify(cacheTemplate).setAfterCommit(
                "sysConfig", "nexora:sys-config:site.title", "Nexora", 5L, TimeUnit.MINUTES);
    }
}
