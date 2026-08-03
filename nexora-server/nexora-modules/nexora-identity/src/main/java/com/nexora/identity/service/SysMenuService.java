package com.nexora.identity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.identity.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {
    /**
     * 获取菜单树
     */
    List<SysMenu> listOrderedMenus();
    
    /**
     * 新增菜单
     */
    long countByParentId(Integer parentId);
    
    /**
     * 更新菜单
     */
    List<SysMenu> listMenusByUserId(Integer userId, String excludedType);
    
    /**
     * 删除菜单
     */
    List<String> listPermissionsByUserId(Integer userId, String type);

    /**
     * 获取当前登录用户所拥有的菜单
     * @return
     */
    List<String> listPermissions(String type);

}
