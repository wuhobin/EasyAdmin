package com.nexora.identity.biz;

import com.nexora.contract.UserDisabledCleanup;
import com.nexora.identity.cache.SecurityPermissionCache;
import com.nexora.identity.domain.form.SysUserForm;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.infrastructure.PasswordPolicyValidator;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserManagementDisabledCleanupTest {

    @Test
    void invokesCleanupWhenUserChangesToDisabled() {
        SysUserService userService = mock(SysUserService.class);
        SysRoleService roleService = mock(SysRoleService.class);
        SecurityPermissionCache permissionCache = mock(SecurityPermissionCache.class);
        PasswordPolicyValidator passwordPolicyValidator = mock(PasswordPolicyValidator.class);
        UserDisabledCleanup disabledCleanup = mock(UserDisabledCleanup.class);
        UserManagementService managementService = new UserManagementService(
                userService, roleService, permissionCache, List.of(),
                List.of(disabledCleanup), passwordPolicyValidator);

        SysUser existing = new SysUser();
        existing.setId(27);
        existing.setStatus(1);
        when(userService.getById(27)).thenReturn(existing);

        SysUserForm form = new SysUserForm();
        form.setId(27);
        form.setNickname("disabled-user");
        form.setStatus(0);
        form.setRoleIds(List.of(20));

        managementService.update(form);

        verify(disabledCleanup).cleanup(27);
    }
}
