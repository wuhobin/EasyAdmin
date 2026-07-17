package com.aurora.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aurora.entity.SysUser;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.domain.query.system.SysUserQuery;
import com.aurora.domain.query.monitor.OnlineUserQuery;
import com.aurora.domain.model.user.OnlineUserData;
import com.aurora.domain.model.user.SysUserPageData;
import com.aurora.domain.model.user.SysUserProfileData;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    /**
     * 分页查询用户
     */
    IPage<SysUserPageData> listUsers(SysUserQuery query, PageParam pageParam);

    /**
     * 新增用户
     */
    void add(SysUser user, List<Integer> roleIds);

    /**
     * 更新用户
     */
    void update(SysUser user, List<Integer> roleIds);

    /**
     * 删除用户
     */
    void delete(List<Integer> ids);


    /**
     * 修改密码
     *
     * @param updatePwdDTO 修改密码参数
     */
    void updatePwd(String oldPassword, String newPassword);

    /**
     * 获取个人信息
     * @return
     */
    SysUserProfileData profile();

    /**
     * 修改个人信息
     * @param user
     */
    void updateProfile(SysUser user);

    /**
     * 锁屏界面验证密码
     * @param password
     * @return
     */
    Boolean verifyPassword(String password);

    /**
     * 重置密码
     * @param user
     * @return
     */
    Boolean resetPassword(SysUser user);

    /**
     * 获取在线用户列表
     * @return
     */
    IPage<OnlineUserData> getOnlineUserList(OnlineUserQuery query, PageParam pageParam);


}
