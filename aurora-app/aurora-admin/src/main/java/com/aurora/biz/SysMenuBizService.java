package com.aurora.biz;

import com.aurora.common.Constants;
import com.aurora.domain.convert.SysMenuConvert;
import com.aurora.domain.form.system.SysMenuForm;
import com.aurora.domain.vo.menu.SysRouterVo;
import com.aurora.domain.vo.system.SysMenuVo;
import com.aurora.entity.SysMenu;
import com.aurora.enums.MenuTypeEnum;
import com.aurora.service.SysMenuService;
import com.aurora.starter.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuBizService {
    private final SysMenuService sysMenuService;

    public List<SysMenuVo> getMenuTree() { return SysMenuConvert.INSTANCE.toVoList(sysMenuService.getMenuTree()); }
    public void add(SysMenuForm form) { sysMenuService.addMenu(SysMenuConvert.INSTANCE.toEntity(form)); }
    public void update(SysMenuForm form) { sysMenuService.updateMenu(SysMenuConvert.INSTANCE.toEntity(form)); }
    public void delete(Integer id) { sysMenuService.deleteMenu(id); }
    public List<SysRouterVo> getCurrentUserMenu() { return buildRouterTree(sysMenuService.getCurrentUserMenus()); }

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
            component = Constants.PARENT_VIEW;
        }
        return SysRouterVo.builder()
                .id(menu.getId()).path(menu.getPath()).redirect(menu.getRedirect())
                .name(menu.getName()).component(component).sort(menu.getSort())
                .meta(new SysRouterVo.MetaVo(menu.getTitle(), menu.getIcon(),
                        menu.getHidden() != null && menu.getHidden() == 1,
                        menu.getIsExternal() != null && menu.getIsExternal() == 1))
                .build();
    }
}
