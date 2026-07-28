package com.nexora.config;

import com.nexora.constants.CommonConstants;
import com.nexora.constants.MenuTypeEnum;
import com.nexora.mapper.SysMenuMapper;
import com.nexora.mapper.SysRoleMapper;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.spi.PermissionProvider;
import com.nexora.cache.SecurityAuthorizationCache;
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
        if (roles.contains(CommonConstants.ADMIN)) {
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
