package com.aurora.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.aurora.common.Constants;
import com.aurora.common.ResultCode;
import com.aurora.domain.dto.user.LoginUserInfo;
import com.aurora.entity.SysUser;
import com.aurora.mapper.SysMenuMapper;
import com.aurora.mapper.SysRoleMapper;
import com.aurora.mapper.SysUserMapper;
import com.aurora.service.AuthService;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.common.utils.bean.BeanUtils;
import com.aurora.starter.webmvc.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long SESSION_TIMEOUT_SECONDS = 60 * 60;

    private static final long REMEMBER_ME_TIMEOUT_SECONDS = 3 * 24 * 60 * 60;

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysMenuMapper menuMapper;

    @Override
    public LoginUserInfo login(String username, String password, boolean rememberMe) {
        // 查询用户
        SysUser user = userMapper.selectByUsername(username);

        // 校验是否能够登录
        validateLogin(password, user);

        // 执行登录（sa-token 封装）
        SecurityUtils.login(user.getId(), new SaLoginParameter().setTimeout(tokenTimeout(rememberMe)));
        String tokenValue = SecurityUtils.getTokenValue();

        // 返回用户信息
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        BeanUtils.copyProperties(user, loginUserInfo);
        loginUserInfo.setToken(tokenValue);

        SecurityUtils.setSessionAttribute(Constants.CURRENT_USER, loginUserInfo);
        return loginUserInfo;
    }

    @Override
    public void logout() {
        SecurityUtils.logout();
    }

    @Override
    public LoginUserInfo getLoginUserInfo() {
        // 当前登录用户 ID
        long userIdLong = SecurityUtils.getLoginIdAsLong();
        Integer userId = (int) userIdLong;
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.ERROR_USER_NOT_EXIST);
        }

        // 角色与权限
        List<String> roles = roleMapper.selectRolesCodeByUserId(userId);
        List<String> permissions;
        if (roles.contains(Constants.ADMIN)) {
            permissions = menuMapper.getPermissionList("BUTTON");
        } else {
            permissions = menuMapper.getPermissionListByUserId(userId, "BUTTON");
        }

        LoginUserInfo loginUserInfo = new LoginUserInfo();
        BeanUtils.copyProperties(user, loginUserInfo);
        loginUserInfo.setRoles(roles);
        loginUserInfo.setPermissions(permissions);
        return loginUserInfo;
    }

    private static void validateLogin(String password, SysUser user) {
        if (user == null) {
            throw new BizException(ResultCode.ERROR_USER_NOT_EXIST);
        }
        // 验证密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException(ResultCode.ERROR_PASSWORD);
        }
        // 验证状态：1=启用，其它=禁用
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.DISABLE_ACCOUNT);
        }
    }

    static long tokenTimeout(boolean rememberMe) {
        return rememberMe ? REMEMBER_ME_TIMEOUT_SECONDS : SESSION_TIMEOUT_SECONDS;
    }
}
