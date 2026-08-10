package com.nexora.identity.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.identity.service.SysRoleService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.nexora.identity.domain.query.SysRoleQuery;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public IPage<SysRole> listRoles(SysRoleQuery query, PageParam pageParam) {
        return baseMapper.selectPage(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean existsByCode(String code, Integer excludeId) {
        if (code == null) {
            return false;
        }
        SysRoleQuery query = new SysRoleQuery();
        query.setCode(code);
        query.setExcludeId(excludeId);
        return baseMapper.selectCount(DynamicCondition.toWrapper(query)) > 0;
    }

    @Override
    public SysRole getByCode(String code) {
        if (code == null) {
            return null;
        }
        SysRoleQuery query = new SysRoleQuery();
        query.setCode(code);
        return baseMapper.selectOne(DynamicCondition.toWrapper(query));
    }

    @Override
    public void deleteRoleMenus(List<Integer> roleIds) {
        baseMapper.deleteMenuByRoleId(roleIds);
    }

    @Override
    public void insertRoleMenus(Integer roleId, List<Integer> menuIds) {
        baseMapper.insertRoleMenus(roleId, menuIds);
    }


    @Override
    public List<Integer> getRoleMenus(Integer id) {
        return baseMapper.getRoleMenus(id);
    }

    @Override
    public List<String> listRoleNamesByUserId(Object userId) {
        return baseMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<String> listRoleCodesByUserId(Object userId) {
        return baseMapper.selectRolesCodeByUserId(userId);
    }

    @Override
    public void deleteUserRoles(List<Integer> userIds) {
        baseMapper.deleteRoleByUserId(userIds);
    }

    @Override
    public void addUserRoles(Integer userId, List<Integer> roleIds) {
        baseMapper.addRoleUser(userId, roleIds);
    }

    @Override
    public List<Integer> listUserIdsByRoleIds(List<Integer> roleIds) {
        return baseMapper.selectUserIdsByRoleIds(roleIds);
    }
}
