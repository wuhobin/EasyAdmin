package com.nexora.service.impl;

import com.nexora.entity.SysMenu;
import com.nexora.mapper.SysMenuMapper;
import com.nexora.service.SysMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> listOrderedMenus() {
        return list(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
    }

    @Override
    public long countByParentId(Integer parentId) {
        return count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, parentId));
    }

    @Override
    public List<SysMenu> listMenusByUserId(Integer userId, String excludedType) {
        return baseMapper.getMenusByUserId(userId, excludedType);
    }

    @Override
    public List<String> listPermissionsByUserId(Integer userId, String type) {
        return baseMapper.getPermissionListByUserId(userId, type);
    }

    @Override
    public List<String> listPermissions(String type) {
        return baseMapper.getPermissionList(type);
    }
}
