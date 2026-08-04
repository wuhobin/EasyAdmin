package com.nexora.identity.security;

import com.nexora.constants.SecurityConstants;
import com.nexora.identity.constants.MenuTypeEnum;
import com.nexora.identity.mapper.SysMenuMapper;
import com.nexora.identity.mapper.SysRoleMapper;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.spi.PermissionProvider;
import com.nexora.identity.cache.SecurityAuthorizationCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NexoraPermissionProvider implements PermissionProvider {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SecurityAuthorizationCache authorizationCache;

    @Override
    public List<String> getPermissionList(Object loginId, AccountType loginType) {
        return getAuthorization(loginId, loginType).permissions();
    }

    @Override
    public List<String> getRoleList(Object loginId, AccountType loginType) {
        return getAuthorization(loginId, loginType).roles();
    }

    public SecurityAuthorizationCache.Authorization getAuthorization(
            Object loginId, AccountType loginType) {
        Integer userId = toInt(loginId);
        return authorizationCache.get(userId, loginType,
                () -> loadAuthorization(userId));
    }

    private SecurityAuthorizationCache.Authorization loadAuthorization(Integer userId) {
        List<String> roles = roleMapper.selectRolesCodeByUserId(userId);
        List<String> permissions;
        if (roles.contains(SecurityConstants.ADMIN_ROLE_CODE)) {
            permissions = menuMapper.getPermissionList(MenuTypeEnum.BUTTON.getCode());
        } else {
            permissions = menuMapper.getPermissionListByUserId(userId, MenuTypeEnum.BUTTON.getCode());
        }
        return new SecurityAuthorizationCache.Authorization(roles, permissions);
    }

    private Integer toInt(Object loginId) {
        if (loginId instanceof Integer i) {
            return i;
        }
        if (loginId instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(loginId.toString());
    }
}
