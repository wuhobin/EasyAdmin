package com.nexora.biz.system;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.cache.SysConfigCache;
import com.nexora.config.SysConfigReader;
import com.nexora.domain.form.system.SysConfigForm;
import com.nexora.entity.SysConfig;
import com.nexora.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysConfigBizServiceTest {

    private final SysConfigService configService = mock(SysConfigService.class);
    private final SysConfigCache configCache = mock(SysConfigCache.class);
    private final SysConfigReader configReader = mock(SysConfigReader.class);
    private final SysConfigBizService bizService = new SysConfigBizService(
            configService, configCache, configReader);

    @Test
    void returnsPublicConfigurationValueByKey() {
        when(configReader.getString("register.enabled", null)).thenReturn("true");

        assertThat(bizService.getValue("register.enabled")).isEqualTo("true");
    }

    @Test
    void rejectsDuplicateKeys() {
        SysConfigForm form = form(null, "site.title", "Nexora", null);
        when(configService.existsByConfigKey("site.title")).thenReturn(true);

        assertThatThrownBy(() -> bizService.add(form))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("配置键已存在");
        verify(configService, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void writesNewValueToCacheAfterAdd() {
        SysConfigForm form = form(null, "site.title", "Nexora", null);
        when(configService.existsByConfigKey("site.title")).thenReturn(false);
        when(configService.save(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        bizService.add(form);

        verify(configCache).setAfterCommit("site.title", "Nexora");
    }

    @Test
    void keepsKeyImmutableAndWritesCacheAfterUpdate() {
        SysConfig existing = new SysConfig();
        existing.setId(7L);
        existing.setConfigKey("site.title");
        existing.setConfigValue("Old");
        when(configService.getById(7L)).thenReturn(existing);
        when(configService.updateById(existing)).thenReturn(true);

        bizService.update(form(7L, "site.title", "Nexora", "站点标题"));

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configService).updateById(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("site.title");
        assertThat(captor.getValue().getConfigValue()).isEqualTo("Nexora");
        verify(configCache).setAfterCommit("site.title", "Nexora");
    }

    @Test
    void rejectsAttemptToRenameKey() {
        SysConfig existing = new SysConfig();
        existing.setId(7L);
        existing.setConfigKey("site.title");
        when(configService.getById(7L)).thenReturn(existing);

        assertThatThrownBy(() -> bizService.update(form(7L, "site.name", "Nexora", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不允许修改");
        verify(configService, never()).updateById(existing);
    }

    @Test
    void evictsCacheAfterDelete() {
        SysConfig existing = new SysConfig();
        existing.setId(7L);
        existing.setConfigKey("site.title");
        when(configService.getById(7L)).thenReturn(existing);
        when(configService.removeById(7L)).thenReturn(true);

        bizService.delete(7L);

        verify(configCache).evictAfterCommit("site.title");
    }

    private static SysConfigForm form(Long id, String key, String value, String remark) {
        SysConfigForm form = new SysConfigForm();
        form.setId(id);
        form.setConfigKey(key);
        form.setConfigValue(value);
        form.setRemark(remark);
        return form;
    }
}
