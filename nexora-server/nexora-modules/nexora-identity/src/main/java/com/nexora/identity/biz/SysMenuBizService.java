package com.nexora.identity.biz;

import com.nexora.identity.constants.IdentityConstants;
import com.nexora.constants.SecurityConstants;
import com.nexora.identity.domain.convert.SysMenuConvert;
import com.nexora.identity.domain.form.SysMenuForm;
import com.nexora.identity.domain.vo.SysRouterVo;
import com.nexora.identity.domain.vo.SysMenuVo;
import com.nexora.identity.entity.SysMenu;
import com.nexora.identity.cache.SecurityAuthorizationCache;
import com.nexora.identity.constants.MenuTypeEnum;
import com.nexora.identity.service.SysMenuService;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuBizService {
    private final SysMenuService sysMenuService;
    private final SecurityAuthorizationCache authorizationCache;

    public List<SysMenuVo> getMenuTree() {
        List<SysMenu> menus = sysMenuService.listOrderedMenus();
        Map<Integer, List<SysMenu>> childrenMap = menus.stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        menus.forEach(menu -> menu.setChildren(childrenMap.get(menu.getId())));
        return SysMenuConvert.INSTANCE.toVoList(menus.stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId() == 0)
                .toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(SysMenuForm form) {
        SysMenu menu = SysMenuConvert.INSTANCE.toEntity(form);
        normalizeComponent(menu);
        sysMenuService.save(menu);
        authorizationCache.evictAllAfterCommit();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysMenuForm form) {
        SysMenu menu = SysMenuConvert.INSTANCE.toEntity(form);
        normalizeComponent(menu);
        sysMenuService.updateById(menu);
        authorizationCache.evictAllAfterCommit();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        if (sysMenuService.countByParentId(id) > 0) {
            throw new BizException("存在子菜单，不能删除");
        }
        sysMenuService.removeById(id);
        authorizationCache.evictAllAfterCommit();
    }

    public List<SysRouterVo> getCurrentUserMenu() {
        String buttonType = MenuTypeEnum.BUTTON.getCode();
        List<SysMenu> menus;
        if (SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)) {
            menus = sysMenuService.listOrderedMenus().stream()
                    .filter(menu -> !MenuTypeEnum.BUTTON.equals(menu.getType()))
                    .toList();
        } else {
            menus = sysMenuService.listMenusByUserId(SecurityUtils.getLoginIdAsInt(), buttonType);
        }
        return buildRouterTree(menus);
    }

    private List<SysRouterVo> buildRouterTree(List<SysMenu> menus) {
        List<SysRouterVo> roots = menus.stream()
                .filter(menu -> menu.getParentId() == null || menu.getParentId() == 0)
                .map(this::toRouter)
                .sorted(Comparator.comparingInt(SysRouterVo::getSort))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        roots.forEach(root -> root.setChildren(getChildren(root.getId(), menus)));
        return roots;
    }

    private List<SysRouterVo> getChildren(Integer parentId, List<SysMenu> menus) {
        List<SysRouterVo> children = menus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .map(this::toRouter)
                .sorted(Comparator.comparingInt(SysRouterVo::getSort))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        children.forEach(child -> child.setChildren(getChildren(child.getId(), menus)));
        return children;
    }

    private SysRouterVo toRouter(SysMenu menu) {
        String component = menu.getComponent();
        if (StringUtils.isEmpty(component) && menu.getParentId() != null && menu.getParentId() != 0
                && MenuTypeEnum.MENU.equals(menu.getType())) {
            component = IdentityConstants.PARENT_VIEW;
        }
        return SysRouterVo.builder()
                .id(menu.getId()).path(menu.getPath()).redirect(menu.getRedirect())
                .name(menu.getName()).component(component).sort(menu.getSort())
                .meta(new SysRouterVo.MetaVo(menu.getTitle(), menu.getIcon(),
                        menu.getHidden() != null && menu.getHidden() == 1,
                        menu.getIsExternal() != null && menu.getIsExternal() == 1))
                .build();
    }

    private static void normalizeComponent(SysMenu menu) {
        if (MenuTypeEnum.CATALOG.equals(menu.getType())) {
            menu.setComponent("Layout");
        }
    }
}
