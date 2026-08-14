package com.nexora.system.biz;

import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.system.cache.SysConfigGroupCache;
import com.nexora.system.api.SystemSettingsValidator;
import com.nexora.system.config.SysConfigGroupReader;
import com.nexora.system.config.SysConfigGroupRegistry;
import com.nexora.system.config.WechatConfigSecretService;
import com.nexora.system.api.WechatLoginSettings;
import com.nexora.system.constants.SysConfigGroupEnum;
import com.nexora.system.api.LoginSettings;
import com.nexora.system.api.PasswordSettings;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemSettings;
import com.nexora.system.entity.SysConfigGroup;
import com.nexora.system.service.SysConfigGroupService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysConfigGroupBizServiceTest {

    private final SysConfigGroupService configService = mock(SysConfigGroupService.class);
    private final SysConfigGroupCache configCache = mock(SysConfigGroupCache.class);
    private final SysConfigGroupRegistry registry = mock(SysConfigGroupRegistry.class);
    private final SysConfigGroupReader reader = mock(SysConfigGroupReader.class);
    private final SystemSettingsValidator registerValidator = mock(SystemSettingsValidator.class);
    private final WechatConfigSecretService wechatConfigSecretService = mock(WechatConfigSecretService.class);
    private final SysConfigGroupBizService bizService = new SysConfigGroupBizService(
            configService, configCache, registry, reader, List.of(registerValidator), wechatConfigSecretService);

    @Test
    void replacesTheWholeGroupAndRefreshesItsCacheAfterCommit() {
        JsonNode input = new ObjectMapper().createObjectNode().put("enabled", true);
        RegistrationSettings value = registerConfig();
        String json = "{\"captchaEnabled\":true,\"verifyEmail\":true,"
                + "\"defaultRoleCode\":\"user\",\"needAudit\":false}";
        SysConfigGroup group = group("register", "{}");
        when(registry.normalizeCode("register")).thenReturn("register");
        when(registry.normalize("register", input))
                .thenReturn(new SysConfigGroupRegistry.NormalizedConfig(value, json));
        when(registerValidator.supports("register")).thenReturn(true);
        when(configService.getByGroupCode("register")).thenReturn(group);
        when(configService.updateById(group)).thenReturn(true);

        bizService.update("register", input);

        verify(configCache).prepareUpdate("register");
        verify(registerValidator).validate(value);
        verify(configService).updateById(group);
        verify(configCache).refreshAfterCommit("register", json);
    }

    @Test
    void refusesAnUnavailableDefaultRegistrationRole() {
        JsonNode input = new ObjectMapper().createObjectNode().put("enabled", true);
        RegistrationSettings value = registerConfig();
        when(registry.normalizeCode("register")).thenReturn("register");
        when(registry.normalize("register", input))
                .thenReturn(new SysConfigGroupRegistry.NormalizedConfig(value, "{}"));
        when(registerValidator.supports("register")).thenReturn(true);
        org.mockito.Mockito.doThrow(new BizException("默认注册角色不可用"))
                .when(registerValidator).validate(value);

        assertThatThrownBy(() -> bizService.update("register", input))
                .isInstanceOf(BizException.class);
        verify(configService, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void exposesTheCaptchaSwitchAsRegistrationConfiguration() {
        SystemSettings system = new SystemSettings();
        RegistrationSettings register = registerConfig();
        register.setCaptchaEnabled(true);
        register.setVerifyEmail(false);
        LoginSettings login = new LoginSettings();
        login.setRememberMeEnabled(false);
        PasswordSettings password = new PasswordSettings();
        WechatLoginSettings wechat = new WechatLoginSettings();
        wechat.setEnabled(false);
        when(reader.system()).thenReturn(system);
        when(reader.register()).thenReturn(register);
        when(reader.login()).thenReturn(login);
        when(reader.password()).thenReturn(password);
        when(reader.wechat()).thenReturn(wechat);

        var publicConfig = bizService.getPublicConfig();

        assertThat(publicConfig.register().captchaEnabled()).isTrue();
        assertThat(publicConfig.register().verifyEmail()).isFalse();
        assertThat(publicConfig.login().rememberMeEnabled()).isFalse();
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
        RegistrationSettings value = registerConfig();
        SysConfigGroup group = group("register", "注册配置", "{\"defaultRoleCode\":\"user\"}");
        when(configService.listOrdered()).thenReturn(List.of(group));
        when(registry.supportedCodes()).thenReturn(Set.of("register"));
        when(registry.parse("register", group.getConfigValue())).thenReturn(value);
        when(registerValidator.supports("register")).thenReturn(true);
        org.mockito.Mockito.doThrow(new BizException("默认注册角色不可用"))
                .when(registerValidator).validate(value);

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

    private static RegistrationSettings registerConfig() {
        RegistrationSettings value = new RegistrationSettings();
        value.setCaptchaEnabled(true);
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
