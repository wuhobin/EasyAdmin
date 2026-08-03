package com.nexora.identity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.identity.entity.SysUser;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.nexora.identity.domain.query.SysUserQuery;
import com.nexora.identity.domain.vo.user.SysUserPageListVo;

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
     * 判断头像地址是否正在被用户使用
     */
    boolean existsByAvatar(String avatar);
}
