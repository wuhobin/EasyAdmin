package com.nexora.identity.biz;

import com.aurora.starter.security.context.SecurityUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexora.constants.SecurityConstants;
import com.nexora.identity.constants.MenuTypeEnum;
import com.nexora.identity.domain.vo.WorkbenchSummaryVo;
import com.nexora.identity.entity.SysMenu;
import com.nexora.identity.service.SysMenuService;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkbenchBizService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;

    public WorkbenchSummaryVo summary() {
        if (SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)) {
            return administratorSummary();
        }
        return userSummary(SecurityUtils.getLoginIdAsInt());
    }

    private WorkbenchSummaryVo administratorSummary() {
        long menuCount = sysMenuService.count(Wrappers.<SysMenu>lambdaQuery()
                .ne(SysMenu::getType, MenuTypeEnum.BUTTON));
        long permissionCount = sysMenuService.listPermissions(MenuTypeEnum.BUTTON.getCode())
                .stream().distinct().count();
        return WorkbenchSummaryVo.builder()
                .administrator(true)
                .userCount(sysUserService.count())
                .roleCount(sysRoleService.count())
                .menuCount(menuCount)
                .permissionCount(permissionCount)
                .build();
    }

    private WorkbenchSummaryVo userSummary(Integer userId) {
        long featureCount = sysMenuService.listMenusByUserId(
                        userId, MenuTypeEnum.BUTTON.getCode()).stream()
                .filter(menu -> MenuTypeEnum.MENU.equals(menu.getType()))
                .map(SysMenu::getId)
                .distinct()
                .count();
        long roleCount = sysRoleService.listRoleCodesByUserId(userId)
                .stream().distinct().count();
        long permissionCount = sysMenuService.listPermissionsByUserId(
                        userId, MenuTypeEnum.BUTTON.getCode()).stream()
                .distinct().count();
        return WorkbenchSummaryVo.builder()
                .administrator(false)
                .accessibleFeatureCount(featureCount)
                .roleCount(roleCount)
                .permissionCount(permissionCount)
                .build();
    }
}
