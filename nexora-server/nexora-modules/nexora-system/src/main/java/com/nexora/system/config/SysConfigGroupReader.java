package com.nexora.system.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.system.cache.SysConfigGroupCache;
import com.nexora.constants.CommonConstants;
import com.nexora.system.constants.SysConfigGroupEnum;
import com.nexora.system.api.EmailSettings;
import com.nexora.system.api.LoginSettings;
import com.nexora.system.api.PasswordSettings;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemConfigReader;
import com.nexora.system.api.SystemSettings;
import com.nexora.system.service.SysConfigGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SysConfigGroupReader implements SystemConfigReader {

    private final SysConfigGroupService configGroupService;
    private final SysConfigGroupCache configGroupCache;
    private final SysConfigGroupRegistry registry;

    @Override
    public SystemSettings system() {
        return read(SysConfigGroupEnum.SYSTEM.getCode(), SystemSettings.class);
    }

    @Override
    public RegistrationSettings register() {
        return read(SysConfigGroupEnum.REGISTER.getCode(), RegistrationSettings.class);
    }

    @Override
    public LoginSettings login() {
        return read(SysConfigGroupEnum.LOGIN.getCode(), LoginSettings.class);
    }

    @Override
    public PasswordSettings password() {
        return read(SysConfigGroupEnum.PASSWORD.getCode(), PasswordSettings.class);
    }

    @Override
    public EmailSettings email() {
        return read(SysConfigGroupEnum.EMAIL.getCode(), EmailSettings.class);
    }

    public <T> T read(String groupCode, Class<T> valueType) {
        String normalizedCode = registry.normalizeCode(groupCode);
        String configValue = configGroupCache.get(normalizedCode,
                () -> configGroupService.getValueByGroupCode(normalizedCode));
        if (configValue == null) {
            throw new BizException(CommonConstants.CONFIG_GROUP_MISSING_MESSAGE.formatted(normalizedCode));
        }
        return registry.parse(normalizedCode, configValue, valueType);
    }
}
