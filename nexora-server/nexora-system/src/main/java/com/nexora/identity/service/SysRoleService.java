package com.nexora.identity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.identity.entity.SysRole;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.nexora.identity.domain.query.SysRoleQuery;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {
    /**
     * 分页查询角色
     */
    IPage<SysRole> listRoles(SysRoleQuery query, PageParam pageParam);
    
    /**
     * 新增角色
     */
    boolean existsByCode(String code, Integer excludeId);

    SysRole getByCode(String code);
    
    /**
     * 更新角色
     */
    void deleteRoleMenus(List<Integer> roleIds);
    
    /**
     * 删除角色
     */
    void insertRoleMenus(Integer roleId, List<Integer> menuIds);


    /**
     * 获取角色所拥有的菜单权限
     * @param id
     * @return
     */
    List<Integer> getRoleMenus(Integer id);

    /**
     * 分配角色权限
     * @param id
     * @param menuIds
     * @return
     */
    List<String> listRoleNamesByUserId(Object userId);

    List<String> listRoleCodesByUserId(Object userId);

    void deleteUserRoles(List<Integer> userIds);

    void addUserRoles(Integer userId, List<Integer> roleIds);

    List<Integer> listUserIdsByRoleIds(List<Integer> roleIds);

}
