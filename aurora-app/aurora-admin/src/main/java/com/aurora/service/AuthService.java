package com.aurora.service;

import com.aurora.domain.dto.user.LoginUserInfo;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 用户密码登录
     *
     * @param loginDTO 登录参数
     * @return 登录用户信息（含 token）
     */
    LoginUserInfo login(String username, String password, boolean rememberMe);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 获取当前登录用户信息（含角色、权限）
     *
     * @return 当前登录用户信息
     */
    LoginUserInfo getLoginUserInfo();
}
