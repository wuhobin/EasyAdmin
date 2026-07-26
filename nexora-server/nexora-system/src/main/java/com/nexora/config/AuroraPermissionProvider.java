package com.nexora.config;

import com.nexora.constants.Constants;
import com.nexora.constants.MenuTypeEnum;
import com.nexora.mapper.SysMenuMapper;
import com.nexora.mapper.SysRoleMapper;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.spi.PermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuroraPermissionProvider implements PermissionProvider {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public List<String> getPermissionList(Object loginId, AccountType loginType) {
        Integer userId = toInt(loginId);
        List<String> roles = roleMapper.selectRolesCodeByUserId(userId);
        if (roles.contains(Constants.ADMIN)) {
            return menuMapper.getPermissionList(MenuTypeEnum.BUTTON.getCode());
        }
        return menuMapper.getPermissionListByUserId(userId, MenuTypeEnum.BUTTON.getCode());
    }

    @Override
    public List<String> getRoleList(Object loginId, AccountType loginType) {
        return roleMapper.selectRolesCodeByUserId(toInt(loginId));
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