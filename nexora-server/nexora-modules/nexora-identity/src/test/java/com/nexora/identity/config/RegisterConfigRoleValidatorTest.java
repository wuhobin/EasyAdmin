package com.nexora.identity.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.service.SysRoleService;
import com.nexora.system.constants.SysConfigGroupEnum;
import com.nexora.system.domain.form.RegisterConfigForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegisterConfigRoleValidatorTest {

    @Test
    void validatesTheConfiguredDefaultRole() {
        SysRoleService roleService = mock(SysRoleService.class);
        RegisterConfigRoleValidator validator = new RegisterConfigRoleValidator(roleService);
        RegisterConfigForm config = registerConfig();
        when(roleService.getByCode("user")).thenReturn(new SysRole());

        validator.validate(config);

        assertThat(validator.supports(SysConfigGroupEnum.REGISTER.getCode())).isTrue();
        assertThat(validator.supports(SysConfigGroupEnum.SYSTEM.getCode())).isFalse();
    }

    @Test
    void rejectsAnUnavailableDefaultRole() {
        SysRoleService roleService = mock(SysRoleService.class);
        RegisterConfigRoleValidator validator = new RegisterConfigRoleValidator(roleService);

        assertThatThrownBy(() -> validator.validate(registerConfig()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("默认注册角色不可用");
    }

    private static RegisterConfigForm registerConfig() {
        RegisterConfigForm config = new RegisterConfigForm();
        config.setDefaultRoleCode("user");
        return config;
    }
}
