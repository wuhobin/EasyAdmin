package com.nexora.identity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.identity.domain.form.SysUserForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysUserControllerTest {

    @Test
    void profileUpdateIsAvailableWithoutUserManagementPermission() throws Exception {
        assertThat(SysUserController.class
                .getMethod("updateProfile", SysUserForm.class)
                .getAnnotation(SaCheckPermission.class))
                .isNull();

        assertThat(SysUserController.class
                .getMethod("update", SysUserForm.class)
                .getAnnotation(SaCheckPermission.class))
                .isNotNull();

        assertThat(SysUserController.class
                .getMethod("sendEmailCode", SysUserForm.class)
                .getAnnotation(SaCheckPermission.class))
                .isNull();
        assertThat(SysUserController.class
                .getMethod("changeEmail", SysUserForm.class)
                .getAnnotation(SaCheckPermission.class))
                .isNull();
    }

    @Test
    void allUserMutationEndpointsUseTheUnifiedForm() throws Exception {
        for (String method : new String[]{
                "addUser", "update", "updatePwd", "updateProfile",
                "sendEmailCode", "changeEmail", "resetPassword"}) {
            assertThat(SysUserController.class.getMethod(method, SysUserForm.class)).isNotNull();
        }
    }
}
