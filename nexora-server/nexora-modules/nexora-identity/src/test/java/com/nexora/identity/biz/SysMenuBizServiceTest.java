package com.nexora.identity.biz;

import com.nexora.constants.SecurityConstants;
import com.nexora.identity.domain.vo.SysRouterVo;
import com.nexora.identity.entity.SysMenu;
import com.nexora.identity.cache.SecurityPermissionCache;
import com.nexora.identity.constants.MenuTypeEnum;
import com.nexora.identity.service.SysMenuService;
import com.aurora.starter.security.context.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysMenuBizServiceTest {

    @Test
    void excludesButtonPermissionsFromAdminRoutes() {
        SysMenuService menuService = mock(SysMenuService.class);
        when(menuService.listOrderedMenus()).thenReturn(List.of(
                menu(1, "/system", MenuTypeEnum.CATALOG),
                menu(2, null, MenuTypeEnum.BUTTON)
        ));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(true);

            List<SysRouterVo> routes = new SysMenuBizService(menuService,
                    mock(SecurityPermissionCache.class)).getCurrentUserMenu();

            assertThat(routes).extracting(SysRouterVo::getId).containsExactly(1);
        }
    }

    @Test
    void evictsAllAuthorizationWhenMenuIsDeleted() {
        SysMenuService menuService = mock(SysMenuService.class);
        SecurityPermissionCache authorizationCache = mock(SecurityPermissionCache.class);
        SysMenuBizService service = new SysMenuBizService(menuService, authorizationCache);

        service.delete(12);

        verify(menuService).removeById(12);
        verify(authorizationCache).evictAllAfterCommit();
    }

    private static SysMenu menu(int id, String path, MenuTypeEnum type) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(0);
        menu.setPath(path);
        menu.setTitle(type.getDesc());
        menu.setSort(id);
        menu.setType(type);
        return menu;
    }
}
