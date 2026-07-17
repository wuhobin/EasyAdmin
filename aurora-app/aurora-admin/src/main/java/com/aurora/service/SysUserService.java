package com.aurora.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aurora.entity.SysUser;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.domain.query.system.SysUserQuery;
import com.aurora.domain.vo.user.SysUserPageListVo;

public interface SysUserService extends IService<SysUser> {
    /**
     * 分页查询用户
     */
    IPage<SysUserPageListVo> listUsers(SysUserQuery query, PageParam pageParam);

    /**
     * 新增用户
     */
    SysUser getByUsername(String username);

    /**
     * 更新用户
     */
}
