package com.nexora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.domain.form.system.SysUserForm;
import com.nexora.domain.form.system.UserProfileForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysUserControllerTest {

    @Test
    void profileUpdateIsAvailableWithoutUserManagementPermission() throws Exception {
        assertThat(SysUserController.class
                .getMethod("updateProfile", UserProfileForm.class)
                .getAnnotation(SaCheckPermission.class))
                .isNull();

        assertThat(SysUserController.class
                .getMethod("update", SysUserForm.class)
                .getAnnotation(SaCheckPermission.class))
                .isNotNull();
    }
}
