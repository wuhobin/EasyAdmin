package com.nexora.config;

import com.aurora.starter.security.account.AccountType;
import com.nexora.cache.SecurityAuthorizationCache;
import com.nexora.mapper.SysMenuMapper;
import com.nexora.mapper.SysRoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NexoraPermissionProviderTest {

    @Test
    void usesCachedAuthorizationWithoutQueryingMappers() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SecurityAuthorizationCache authorizationCache = mock(SecurityAuthorizationCache.class);
        SecurityAuthorizationCache.Authorization authorization =
                new SecurityAuthorizationCache.Authorization(
                        List.of("admin"), List.of("sys:config:list"));
        when(authorizationCache.get(any(), any(), any())).thenReturn(authorization);
        NexoraPermissionProvider provider =
                new NexoraPermissionProvider(roleMapper, menuMapper, authorizationCache);

        List<String> permissions = provider.getPermissionList(7, AccountType.LOGIN);
        List<String> roles = provider.getRoleList(7, AccountType.LOGIN);

        assertThat(permissions).containsExactly("sys:config:list");
        assertThat(roles).containsExactly("admin");
        verifyNoInteractions(roleMapper, menuMapper);
    }
}
