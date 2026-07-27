package com.nexora.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.cache.SysConfigCache;
import com.nexora.service.SysConfigService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysConfigReaderTest {

    private final SysConfigService configService = mock(SysConfigService.class);
    private final SysConfigCache configCache = mock(SysConfigCache.class);
    private final SysConfigReader reader = new SysConfigReader(configService, configCache);

    @Test
    void readsTypedValuesDeclaredByCaller() {
        when(configCache.get(eq("feature.enabled"), any())).thenReturn("true");
        when(configCache.get(eq("upload.limit"), any())).thenReturn("12");
        when(configCache.get(eq("sequence.start"), any())).thenReturn("9000000000");
        when(configCache.get(eq("mail.options"), any()))
                .thenReturn("{\"host\":\"localhost\",\"port\":25}");

        assertThat(reader.getRequiredBoolean("feature.enabled")).isTrue();
        assertThat(reader.getRequiredInt("upload.limit")).isEqualTo(12);
        assertThat(reader.getRequiredLong("sequence.start")).isEqualTo(9_000_000_000L);
        assertThat(reader.getRequiredJson("mail.options", MailOptions.class))
                .isEqualTo(new MailOptions("localhost", 25));
    }

    @Test
    void returnsDefaultOnlyWhenConfigurationIsMissing() {
        when(configCache.get(eq("missing.config"), any())).thenReturn(null);

        assertThat(reader.getString("missing.config", "fallback")).isEqualTo("fallback");
        assertThat(reader.getInt("missing.config", 10)).isEqualTo(10);
    }

    @Test
    void rejectsMissingRequiredAndMalformedValues() {
        when(configCache.get(eq("required.config"), any())).thenReturn(null);
        when(configCache.get(eq("feature.enabled"), any())).thenReturn("yes");

        assertThatThrownBy(() -> reader.getRequiredString("required.config"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("required.config");
        assertThatThrownBy(() -> reader.getRequiredBoolean("feature.enabled"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("feature.enabled");
    }

    private record MailOptions(String host, int port) {
    }
}
