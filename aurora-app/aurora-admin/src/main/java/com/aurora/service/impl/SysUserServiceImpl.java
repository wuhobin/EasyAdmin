package com.aurora.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.domain.convert.SysUserConvert;
import com.aurora.domain.query.system.SysUserQuery;
import com.aurora.domain.vo.user.SysUserPageListVo;
import com.aurora.domain.vo.user.SysUserProfileVo;
import com.aurora.mapper.SysRoleMapper;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.entity.SysUser;
import com.aurora.mapper.SysUserMapper;
import com.aurora.service.SysUserService;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.secure.BCrypt;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleMapper roleMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public IPage<SysUserPageListVo> listUsers(SysUserQuery query, PageParam pageParam) {
        return baseMapper.selectUserPage(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SysUser user, List<Integer> roleIds) {
        // 检查用户名是否已存在
        if (baseMapper.selectByUsername(user.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword(),BCrypt.gensalt()));
        save(user);

        //保存角色信息
        roleMapper.addRoleUser(user.getId(), roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user, List<Integer> roleIds) {
        // 检查用户是否存在
        if (getById(user.getId()) == null) {
            throw new BizException("用户不存在");
        }
        updateById(user);

        //修改角色 先删除角色再新增
        roleMapper.deleteRoleByUserId(Collections.singletonList(user.getId()));
        roleMapper.addRoleUser(user.getId(), roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Integer> ids) {
        removeBatchByIds(ids);
        roleMapper.deleteRoleByUserId(ids);
    }


    @Override
    public void updatePwd(String oldPassword, String newPassword) {

        SysUser user = this.getById(SecurityUtils.getLoginIdAsInt());
        if (user == null) {
            throw new BizException("用户不存在");
        }

        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BizException("旧密码错误");
        }

        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        this.updateById(user);
    }

    @Override
    public SysUserProfileVo profile() {

        SysUser sysUser = baseMapper.selectById(SecurityUtils.getLoginIdAsInt());
        sysUser.setPassword(null);
        //获取角色
        List<String> roles = roleMapper.selectRolesByUserId(sysUser.getId());

        return new SysUserProfileVo(SysUserConvert.INSTANCE.toVo(sysUser), roles);
    }

    @Override
    public void updateProfile(SysUser user) {
        baseMapper.updateById(user);
    }

    @Override
    public Boolean verifyPassword(String password) {
        SysUser user = baseMapper.selectById(SecurityUtils.getLoginIdAsInt());
        return BCrypt.checkpw(password, user.getPassword());
    }

    @Override
    public Boolean resetPassword(SysUser user) {
        user.setPassword(BCrypt.hashpw(user.getPassword(),BCrypt.gensalt()));
        baseMapper.updateById(user);
        return true;
    }

}
