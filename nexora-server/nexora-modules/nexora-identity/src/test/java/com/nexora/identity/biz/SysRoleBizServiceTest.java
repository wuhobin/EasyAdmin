package com.nexora.identity.biz;

import com.nexora.identity.cache.SecurityAuthorizationCache;
import com.nexora.identity.service.SysRoleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysRoleBizServiceTest {

    @Test
    void evictsAffectedUsersWhenRolePermissionsChange() {
        SysRoleService roleService = mock(SysRoleService.class);
        SecurityAuthorizationCache authorizationCache = mock(SecurityAuthorizationCache.class);
        when(roleService.listUserIdsByRoleIds(List.of(3))).thenReturn(List.of(7, 8));
        SysRoleBizService service = new SysRoleBizService(roleService, authorizationCache);

        service.updateRoleMenus(3, List.of(10, 11));

        verify(roleService).deleteRoleMenus(List.of(3));
        verify(roleService).insertRoleMenus(3, List.of(10, 11));
        verify(authorizationCache).evictUsersAfterCommit(List.of(7, 8));
    }
}
