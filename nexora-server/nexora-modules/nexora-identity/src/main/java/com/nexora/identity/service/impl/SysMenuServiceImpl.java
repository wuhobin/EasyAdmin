package com.nexora.identity.service.impl;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.nexora.identity.domain.query.SysMenuQuery;
import com.nexora.identity.entity.SysMenu;
import com.nexora.identity.mapper.SysMenuMapper;
import com.nexora.identity.service.SysMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> listOrderedMenus() {
        return baseMapper.selectOrdered(DynamicCondition.toWrapper(new SysMenuQuery()));
    }

    @Override
    public long countByParentId(Integer parentId) {
        if (parentId == null) {
            return 0;
        }
        SysMenuQuery query = new SysMenuQuery();
        query.setParentId(parentId);
        return count(DynamicCondition.toWrapper(query));
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
