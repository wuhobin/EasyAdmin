package com.nexora.system.biz;

import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexora.system.cache.SysConfigGroupCache;
import com.nexora.system.api.SystemSettingsValidator;
import com.nexora.system.config.SysConfigGroupReader;
import com.nexora.system.config.SysConfigGroupRegistry;
import com.nexora.system.config.WechatConfigSecretService;
import com.nexora.system.constants.SystemConfigConstants;
import com.nexora.system.constants.SysConfigGroupEnum;
import com.nexora.system.api.LoginSettings;
import com.nexora.system.api.PasswordSettings;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemSettings;
import com.nexora.system.api.WechatLoginSettings;
import com.nexora.system.domain.vo.SysConfigGroupDetailVo;
import com.nexora.system.domain.vo.SysConfigGroupSummaryVo;
import com.nexora.system.domain.vo.SysConfigPublicVo;
import com.nexora.system.entity.SysConfigGroup;
import com.nexora.system.service.SysConfigGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysConfigGroupBizService {

    private final SysConfigGroupService configGroupService;
    private final SysConfigGroupCache configGroupCache;
    private final SysConfigGroupRegistry registry;
    private final SysConfigGroupReader configReader;
    private final List<SystemSettingsValidator> businessValidators;
    private final WechatConfigSecretService wechatConfigSecretService;

    public List<SysConfigGroupSummaryVo> list() {
        return configGroupService.listOrdered().stream().map(group -> SysConfigGroupSummaryVo.builder()
                .id(group.getId())
                .groupCode(group.getGroupCode())
                .groupName(group.getGroupName())
                .sort(group.getSort())
                .updateTime(group.getUpdateTime())
                .build()).toList();
    }

    public SysConfigGroupDetailVo get(String groupCode) {
        String normalizedCode = registry.normalizeCode(groupCode);
        SysConfigGroup group = requireGroup(normalizedCode);
        Object config = registry.parse(normalizedCode, group.getConfigValue());
        if (config instanceof WechatLoginSettings wechat) {
            config = wechatConfigSecretService.mask(wechat);
        }
        return SysConfigGroupDetailVo.builder()
                .id(group.getId())
                .groupCode(group.getGroupCode())
                .groupName(group.getGroupName())
                .configValue(config)
                .sort(group.getSort())
                .createTime(group.getCreateTime())
                .updateTime(group.getUpdateTime())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String groupCode, JsonNode configValue) {
        String normalizedCode = registry.normalizeCode(groupCode);
        SysConfigGroup group = requireGroup(normalizedCode);
        SysConfigGroupRegistry.NormalizedConfig normalized = registry.normalize(normalizedCode, configValue);
        Object value = normalized.value();
        String json = normalized.json();
        if (value instanceof WechatLoginSettings wechat) {
            WechatLoginSettings existing = registry.parse(normalizedCode, group.getConfigValue(), WechatLoginSettings.class);
            WechatLoginSettings stored = wechatConfigSecretService.prepareForStorage(wechat, existing);
            value = wechatConfigSecretService.decrypt(stored);
            json = wechatConfigSecretService.toJson(stored);
        }
        validateBusinessRules(normalizedCode, value);
        prepareCacheUpdate(normalizedCode);
        group.setConfigValue(json);
        if (!configGroupService.updateById(group)) {
            throw new BizException(SystemConfigConstants.CONFIG_GROUP_UPDATE_FAILED_MESSAGE);
        }
        configGroupCache.refreshAfterCommit(normalizedCode, json);
    }

    public void refreshCache() {
        List<SysConfigGroup> groups = loadAndValidateAll();
        try {
            for (SysConfigGroup group : groups) {
                configGroupCache.setRequired(group.getGroupCode(), group.getConfigValue());
            }
        } catch (RuntimeException exception) {
            throw new BizException(SystemConfigConstants.CONFIG_GROUP_CACHE_UNAVAILABLE_MESSAGE);
        }
    }

    public SysConfigPublicVo getPublicConfig() {
        SystemSettings system = configReader.system();
        RegistrationSettings register = configReader.register();
        LoginSettings login = configReader.login();
        PasswordSettings password = configReader.password();
        WechatLoginSettings wechat = configReader.wechat();
        return new SysConfigPublicVo(
                new SysConfigPublicVo.SystemConfig(
                        system.getSiteName(), system.getShortTitle(), system.getSiteDescription(),
                        system.getSiteLogo(), system.getCopyright(), system.getIcp(),
                        system.getWatermarkEnabled(), system.getWatermarkType(),
                        system.getWatermarkCustomText(), system.getWatermarkOpacity()),
                new SysConfigPublicVo.RegisterConfig(
                        register.getCaptchaEnabled(),
                        register.getVerifyEmail(), register.getNeedAudit()),
                new SysConfigPublicVo.LoginConfig(
                        login.getRememberMeEnabled()),
                new SysConfigPublicVo.PasswordConfig(
                        password.getMinLength(), password.getMaxLength(),
                        password.getRequireUppercase(), password.getRequireLowercase(),
                        password.getRequireNumber(), password.getRequireSpecial()),
                new SysConfigPublicVo.WechatConfig(
                        wechat.getEnabled(), wechat.getQrCodeUrl()));
    }

    public void validateDatabase() {
        loadAndValidateAll();
    }

    private List<SysConfigGroup> loadAndValidateAll() {
        List<SysConfigGroup> groups = configGroupService.listOrdered();
        Set<String> actualCodes = new HashSet<>();
        for (SysConfigGroup group : groups) {
            actualCodes.add(group.getGroupCode());
            validateGroupName(group);
            Object config = registry.parse(group.getGroupCode(), group.getConfigValue());
            validateBusinessRules(group.getGroupCode(), config);
        }
        if (!actualCodes.equals(registry.supportedCodes())) {
            Set<String> missing = new HashSet<>(registry.supportedCodes());
            missing.removeAll(actualCodes);
            Set<String> unsupported = new HashSet<>(actualCodes);
            unsupported.removeAll(registry.supportedCodes());
            throw new BizException(SystemConfigConstants.CONFIG_GROUP_STRUCTURE_INVALID_MESSAGE.formatted(
                    missing, unsupported));
        }
        return groups;
    }

    private void validateGroupName(SysConfigGroup group) {
        SysConfigGroupEnum definition = SysConfigGroupEnum.getByCode(group.getGroupCode());
        if (definition != null && !definition.getDescription().equals(group.getGroupName())) {
            throw new BizException(SystemConfigConstants.CONFIG_GROUP_NAME_MISMATCH_MESSAGE.formatted(
                    group.getGroupCode(), group.getGroupName(), definition.getDescription()));
        }
    }

    private void validateBusinessRules(String groupCode, Object config) {
        businessValidators.stream()
                .filter(validator -> validator.supports(groupCode))
                .forEach(validator -> validator.validate(config));
    }

    private SysConfigGroup requireGroup(String groupCode) {
        SysConfigGroup group = configGroupService.getByGroupCode(groupCode);
        if (group == null) {
            throw new BizException(SystemConfigConstants.CONFIG_GROUP_MISSING_MESSAGE.formatted(groupCode));
        }
        return group;
    }

    private void prepareCacheUpdate(String groupCode) {
        try {
            configGroupCache.prepareUpdate(groupCode);
        } catch (RuntimeException exception) {
            throw new BizException(SystemConfigConstants.CONFIG_GROUP_CACHE_UNAVAILABLE_MESSAGE);
        }
    }
}
