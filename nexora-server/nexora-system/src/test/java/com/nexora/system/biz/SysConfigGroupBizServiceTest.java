package com.nexora.system.biz;

import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.system.cache.SysConfigGroupCache;
import com.nexora.system.config.SysConfigGroupReader;
import com.nexora.system.config.SysConfigGroupRegistry;
import com.nexora.system.constants.SysConfigGroupEnum;
import com.nexora.system.domain.form.RegisterConfigForm;
import com.nexora.system.entity.SysConfigGroup;
import com.nexora.system.service.SysConfigGroupService;
import com.nexora.identity.service.SysRoleService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysConfigGroupBizServiceTest {

    private final SysConfigGroupService configService = mock(SysConfigGroupService.class);
    private final SysRoleService roleService = mock(SysRoleService.class);
    private final SysConfigGroupCache configCache = mock(SysConfigGroupCache.class);
    private final SysConfigGroupRegistry registry = mock(SysConfigGroupRegistry.class);
    private final SysConfigGroupReader reader = mock(SysConfigGroupReader.class);
    private final SysConfigGroupBizService bizService = new SysConfigGroupBizService(
            configService, roleService, configCache, registry, reader);

    @Test
    void replacesTheWholeGroupAndRefreshesItsCacheAfterCommit() {
        JsonNode input = new ObjectMapper().createObjectNode().put("enabled", true);
        RegisterConfigForm value = registerConfig();
        String json = "{\"enabled\":true,\"verifyEmail\":true,"
                + "\"defaultRoleCode\":\"user\",\"needAudit\":false}";
        SysConfigGroup group = group("register", "{}");
        when(registry.normalizeCode("register")).thenReturn("register");
        when(registry.normalize("register", input))
                .thenReturn(new SysConfigGroupRegistry.NormalizedConfig(value, json));
        when(roleService.getByCode("user")).thenReturn(new com.nexora.identity.entity.SysRole());
        when(configService.getByGroupCode("register")).thenReturn(group);
        when(configService.updateById(group)).thenReturn(true);

        bizService.update("register", input);

        verify(configCache).prepareUpdate("register");
        verify(configService).updateById(group);
        verify(configCache).refreshAfterCommit("register", json);
    }

    @Test
    void refusesAnUnavailableDefaultRegistrationRole() {
        JsonNode input = new ObjectMapper().createObjectNode().put("enabled", true);
        RegisterConfigForm value = registerConfig();
        when(registry.normalizeCode("register")).thenReturn("register");
        when(registry.normalize("register", input))
                .thenReturn(new SysConfigGroupRegistry.NormalizedConfig(value, "{}"));

        assertThatThrownBy(() -> bizService.update("register", input))
                .isInstanceOf(BizException.class);
        verify(configService, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesToUpdateWhenTheCacheCannotBeInvalidated() {
        JsonNode input = new ObjectMapper().createObjectNode().put("siteName", "Nexora");
        Object value = new Object();
        SysConfigGroup group = group("system", "{}");
        when(registry.normalizeCode("system")).thenReturn("system");
        when(registry.normalize("system", input))
                .thenReturn(new SysConfigGroupRegistry.NormalizedConfig(value, "{\"siteName\":\"Nexora\"}"));
        when(configService.getByGroupCode("system")).thenReturn(group);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
                .when(configCache).prepareUpdate("system");

        assertThatThrownBy(() -> bizService.update("system", input))
                .isInstanceOf(BizException.class);
        verify(configService, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void startupValidationRejectsAnUnavailableRegistrationRole() {
        RegisterConfigForm value = registerConfig();
        SysConfigGroup group = group("register", "注册配置", "{\"defaultRoleCode\":\"user\"}");
        when(configService.listOrdered()).thenReturn(List.of(group));
        when(registry.supportedCodes()).thenReturn(Set.of("register"));
        when(registry.parse("register", group.getConfigValue())).thenReturn(value);

        assertThatThrownBy(bizService::validateDatabase)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("默认注册角色不可用");
    }

    @Test
    void startupValidationRejectsAGroupNameThatDiffersFromTheEnum() {
        SysConfigGroup group = group("system", "旧系统名称", "{}");
        when(configService.listOrdered()).thenReturn(List.of(group));
        when(registry.supportedCodes()).thenReturn(Set.of("system"));

        assertThatThrownBy(bizService::validateDatabase)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("数据库=旧系统名称")
                .hasMessageContaining("预期=系统配置");
        verify(registry, never()).parse(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private static RegisterConfigForm registerConfig() {
        RegisterConfigForm value = new RegisterConfigForm();
        value.setEnabled(true);
        value.setVerifyEmail(true);
        value.setDefaultRoleCode("user");
        value.setNeedAudit(false);
        return value;
    }

    private static SysConfigGroup group(String code, String value) {
        return group(code, SysConfigGroupEnum.getByCode(code).getDescription(), value);
    }

    private static SysConfigGroup group(String code, String name, String value) {
        SysConfigGroup group = new SysConfigGroup();
        group.setId(1L);
        group.setGroupCode(code);
        group.setGroupName(name);
        group.setConfigValue(value);
        return group;
    }
}
