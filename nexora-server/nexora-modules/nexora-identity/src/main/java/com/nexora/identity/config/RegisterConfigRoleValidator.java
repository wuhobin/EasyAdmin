package com.nexora.identity.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.constants.CommonConstants;
import com.nexora.identity.service.SysRoleService;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemSettingsValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 校验注册配置引用的默认角色是否可用。
 */
@Component
@RequiredArgsConstructor
public class RegisterConfigRoleValidator implements SystemSettingsValidator {

    private final SysRoleService roleService;

    @Override
    public boolean supports(String groupCode) {
        return RegistrationSettings.GROUP_CODE.equals(groupCode);
    }

    @Override
    public void validate(Object config) {
        RegistrationSettings registerConfig = (RegistrationSettings) config;
        if (roleService.getByCode(registerConfig.getDefaultRoleCode()) == null) {
            throw new BizException(CommonConstants.REGISTER_ROLE_UNAVAILABLE_MESSAGE);
        }
    }
}
