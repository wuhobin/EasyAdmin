package com.nexora.identity.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.constants.CommonConstants;
import com.nexora.identity.service.SysRoleService;
import com.nexora.system.config.SysConfigGroupBusinessValidator;
import com.nexora.system.constants.SysConfigGroupEnum;
import com.nexora.system.domain.form.RegisterConfigForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 校验注册配置引用的默认角色是否可用。
 */
@Component
@RequiredArgsConstructor
public class RegisterConfigRoleValidator implements SysConfigGroupBusinessValidator {

    private final SysRoleService roleService;

    @Override
    public boolean supports(String groupCode) {
        return SysConfigGroupEnum.REGISTER.getCode().equals(groupCode);
    }

    @Override
    public void validate(Object config) {
        RegisterConfigForm registerConfig = (RegisterConfigForm) config;
        if (roleService.getByCode(registerConfig.getDefaultRoleCode()) == null) {
            throw new BizException(CommonConstants.REGISTER_ROLE_UNAVAILABLE_MESSAGE);
        }
    }
}
