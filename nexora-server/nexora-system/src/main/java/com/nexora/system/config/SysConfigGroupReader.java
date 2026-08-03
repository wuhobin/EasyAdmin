package com.nexora.system.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.system.cache.SysConfigGroupCache;
import com.nexora.constants.CommonConstants;
import com.nexora.system.constants.SysConfigGroupEnum;
import com.nexora.system.domain.form.EmailConfigForm;
import com.nexora.system.domain.form.LoginConfigForm;
import com.nexora.system.domain.form.PasswordConfigForm;
import com.nexora.system.domain.form.RegisterConfigForm;
import com.nexora.system.domain.form.SystemConfigForm;
import com.nexora.system.service.SysConfigGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SysConfigGroupReader {

    private final SysConfigGroupService configGroupService;
    private final SysConfigGroupCache configGroupCache;
    private final SysConfigGroupRegistry registry;

    public SystemConfigForm system() {
        return read(SysConfigGroupEnum.SYSTEM.getCode(), SystemConfigForm.class);
    }

    public RegisterConfigForm register() {
        return read(SysConfigGroupEnum.REGISTER.getCode(), RegisterConfigForm.class);
    }

    public LoginConfigForm login() {
        return read(SysConfigGroupEnum.LOGIN.getCode(), LoginConfigForm.class);
    }

    public PasswordConfigForm password() {
        return read(SysConfigGroupEnum.PASSWORD.getCode(), PasswordConfigForm.class);
    }

    public EmailConfigForm email() {
        return read(SysConfigGroupEnum.EMAIL.getCode(), EmailConfigForm.class);
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
