package com.nexora.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.entity.SysUser;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.nexora.domain.query.system.SysUserQuery;
import com.nexora.domain.vo.user.SysUserPageListVo;

public interface SysUserService extends IService<SysUser> {
    /**
     * 分页查询用户
     */
    IPage<SysUserPageListVo> listUsers(SysUserQuery query, PageParam pageParam);

    /**
     * 根据规范化邮箱查询用户
     */
    SysUser getByEmail(String email);

    /**
     * 更新用户
     */
}
