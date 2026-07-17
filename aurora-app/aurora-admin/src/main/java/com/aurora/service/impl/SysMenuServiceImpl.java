package com.aurora.service.impl;

import com.aurora.common.Constants;
import com.aurora.entity.SysMenu;
import com.aurora.enums.MenuTypeEnum;
import com.aurora.mapper.SysMenuMapper;
import com.aurora.service.SysMenuService;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> menus = list(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        Map<Integer, List<SysMenu>> childrenMap = menus.stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        menus.forEach(menu -> menu.setChildren(childrenMap.get(menu.getId())));
        return menus.stream().filter(menu -> menu.getParentId() != null && menu.getParentId() == 0).toList();
    }

    @Override
    public void addMenu(SysMenu menu) {
        normalizeComponent(menu);
        save(menu);
    }

    @Override
    public void updateMenu(SysMenu menu) {
        normalizeComponent(menu);
        updateById(menu);
    }

    @Override
    public void deleteMenu(Integer id) {
        if (count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)) > 0) {
            throw new BizException("存在子菜单，不能删除");
        }
        removeById(id);
    }

    @Override
    public List<SysMenu> getCurrentUserMenus() {
        if (SecurityUtils.hasRole(Constants.ADMIN)) {
            return baseMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .ne(SysMenu::getType, MenuTypeEnum.BUTTON.getCode()));
        }
        return baseMapper.getMenusByUserId(SecurityUtils.getLoginIdAsInt(), MenuTypeEnum.BUTTON.getCode());
    }

    private static void normalizeComponent(SysMenu menu) {
        if (MenuTypeEnum.CATALOG.equals(menu.getType())) {
            menu.setComponent("Layout");
        }
    }
}
