package com.nexora.identity.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.service.SysRoleService;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.RegistrationSettings;
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
        RegistrationSettings config = registerConfig();
        when(roleService.getByCode("user")).thenReturn(new SysRole());

        validator.validate(config);

        assertThat(validator.supports(RegistrationSettings.GROUP_CODE)).isTrue();
        assertThat(validator.supports("system")).isFalse();
    }

    @Test
    void rejectsAnUnavailableDefaultRole() {
        SysRoleService roleService = mock(SysRoleService.class);
        RegisterConfigRoleValidator validator = new RegisterConfigRoleValidator(roleService);

        assertThatThrownBy(() -> validator.validate(registerConfig()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("默认注册角色不可用");
    }

    private static RegistrationSettings registerConfig() {
        RegistrationSettings config = new RegistrationSettings();
        config.setDefaultRoleCode("user");
        return config;
    }
}
