package com.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.service.SysRoleService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.entity.SysRole;
import com.aurora.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public IPage<SysRole> listRoles(String name, PageParam pageParam) {

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.isNotBlank(name),SysRole::getName, name)
                .orderByDesc(SysRole::getCreateTime);

        return baseMapper.selectPage(PageUtils.buildPage(pageParam), wrapper);
    }

    @Override
    public void addRole(SysRole role) {
        // 检查角色编码是否已存在
        if (checkCodeExists(role.getCode(), null)) {
            throw new RuntimeException("角色编码已存在");
        }
        save(role);
    }

    @Override
    public void updateRole(SysRole role) {
        // 检查角色是否存在
        if (getById(role.getId()) == null) {
            throw new RuntimeException("角色不存在");
        }
        // 检查角色编码是否已存在
        if (checkCodeExists(role.getCode(), role.getId())) {
            throw new RuntimeException("角色编码已存在");
        }
        updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Integer> ids) {
        removeBatchByIds(ids);
        baseMapper.deleteMenuByRoleId(ids);
    }


    @Override
    public List<Integer> getRoleMenus(Integer id) {
        return baseMapper.getRoleMenus(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void updateRoleMenus(Integer id, List<Integer> menuIds) {
        baseMapper.deleteMenuByRoleId(Collections.singletonList(id));
        baseMapper.insertRoleMenus(id, menuIds);
        return null;
    }

    /**
     * 检查角色编码是否已存在
     *
     * @param code 角色编码
     * @param excludeId 排除的角色ID
     * @return true:已存在 false:不存在
     */
    private boolean checkCodeExists(String code, Integer excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        return baseMapper.selectCount(wrapper) > 0;
    }
}
