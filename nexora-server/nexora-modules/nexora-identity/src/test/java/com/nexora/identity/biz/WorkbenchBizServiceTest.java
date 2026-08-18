package com.nexora.identity.biz;

import com.aurora.starter.security.context.SecurityUtils;
import com.nexora.constants.SecurityConstants;
import com.nexora.identity.constants.MenuTypeEnum;
import com.nexora.identity.domain.vo.WorkbenchSummaryVo;
import com.nexora.identity.entity.SysMenu;
import com.nexora.identity.service.SysMenuService;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkbenchBizServiceTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final SysRoleService roleService = mock(SysRoleService.class);
    private final SysMenuService menuService = mock(SysMenuService.class);
    private final WorkbenchBizService service =
            new WorkbenchBizService(userService, roleService, menuService);

    @Test
    void returnsGlobalStatisticsForAdministrator() {
        when(userService.count()).thenReturn(12L);
        when(roleService.count()).thenReturn(4L);
        when(menuService.count(any())).thenReturn(9L);
        when(menuService.listPermissions(MenuTypeEnum.BUTTON.getCode()))
                .thenReturn(List.of("user:add", "user:add", "user:update"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE))
                    .thenReturn(true);

            WorkbenchSummaryVo result = service.summary();

            assertThat(result.getAdministrator()).isTrue();
            assertThat(result.getUserCount()).isEqualTo(12);
            assertThat(result.getRoleCount()).isEqualTo(4);
            assertThat(result.getMenuCount()).isEqualTo(9);
            assertThat(result.getPermissionCount()).isEqualTo(2);
            assertThat(result.getAccessibleFeatureCount()).isNull();
        }
    }

    @Test
    void returnsOnlyPersonalStatisticsForRegularUser() {
        SysMenu menu = new SysMenu();
        menu.setId(7);
        menu.setType(MenuTypeEnum.MENU);
        SysMenu catalog = new SysMenu();
        catalog.setId(2);
        catalog.setType(MenuTypeEnum.CATALOG);
        when(menuService.listMenusByUserId(23, MenuTypeEnum.BUTTON.getCode()))
                .thenReturn(List.of(catalog, menu, menu));
        when(roleService.listRoleCodesByUserId(23))
                .thenReturn(List.of("member", "member"));
        when(menuService.listPermissionsByUserId(23, MenuTypeEnum.BUTTON.getCode()))
                .thenReturn(List.of("file:list", "file:list", "mail:list"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE))
                    .thenReturn(false);
            security.when(SecurityUtils::getLoginIdAsInt).thenReturn(23);

            WorkbenchSummaryVo result = service.summary();

            assertThat(result.getAdministrator()).isFalse();
            assertThat(result.getAccessibleFeatureCount()).isEqualTo(1);
            assertThat(result.getRoleCount()).isEqualTo(1);
            assertThat(result.getPermissionCount()).isEqualTo(2);
            assertThat(result.getUserCount()).isNull();
            assertThat(result.getMenuCount()).isNull();
            verify(userService, never()).count();
            verify(roleService, never()).count();
        }
    }
}
