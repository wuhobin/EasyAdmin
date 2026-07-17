package com.aurora.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.common.RedisConstants;
import com.aurora.domain.query.system.SysUserQuery;
import com.aurora.domain.query.monitor.OnlineUserQuery;
import com.aurora.mapper.SysRoleMapper;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.entity.SysUser;
import com.aurora.mapper.SysUserMapper;
import com.aurora.service.SysUserService;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.common.utils.JsonUtil;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.aurora.domain.model.user.OnlineUserData;
import com.aurora.domain.model.user.SysUserPageData;
import com.aurora.domain.model.user.SysUserProfileData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.secure.BCrypt;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleMapper roleMapper;
    private final RedisCache redisCache;
    private final SysUserMapper sysUserMapper;

    @Override
    public IPage<SysUserPageData> listUsers(SysUserQuery query, PageParam pageParam) {
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
    public SysUserProfileData profile() {

        SysUser sysUser = baseMapper.selectById(SecurityUtils.getLoginIdAsInt());
        sysUser.setPassword(null);
        //获取角色
        List<String> roles = roleMapper.selectRolesByUserId(sysUser.getId());

        return new SysUserProfileData(sysUser, roles);
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

    @Override
    public IPage<OnlineUserData> getOnlineUserList(OnlineUserQuery query, PageParam pageParam) {
        Integer pageNum = pageParam.getPageNum() != null ? pageParam.getPageNum() : 1;
        Integer pageSize = pageParam.getPageSize() != null ? pageParam.getPageSize() : 10;

        // 返回数据对象
        Collection<String> keys = redisCache.scan(RedisConstants.LOGIN_TOKEN.concat("*"));

        List<OnlineUserData> totalList = new ArrayList<>();
        for (String key : keys) {
            Object cachedUser = redisCache.getCacheObject(key);
            if (cachedUser == null) {
                continue;
            }
            OnlineUserData onlineUser = JsonUtil.parse(JsonUtil.toJson(cachedUser), OnlineUserData.class);
            if (query != null && StringUtils.isNotBlank(query.getUsername())) {
                if (onlineUser.getUsername().contains(query.getUsername())) {
                    totalList.add(onlineUser);
                }
                continue;
            }
            totalList.add(onlineUser);
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = totalList.size() - fromIndex > pageSize ? fromIndex + pageSize : totalList.size();
        List<OnlineUserData> records = totalList.subList(fromIndex, toIndex);

        //根据时间排序
        records.sort((o1, o2) -> o2.getLastLoginTime().compareTo(o1.getLastLoginTime()));

        IPage<OnlineUserData> page = new Page<>(pageNum, pageSize);
        page.setRecords(records);
        page.setTotal(totalList.size());
        return page;
    }
}
