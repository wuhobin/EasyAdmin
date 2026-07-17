package com.aurora.biz;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.aurora.common.Constants;
import com.aurora.common.ResultCode;
import com.aurora.domain.form.auth.LoginForm;
import com.aurora.domain.vo.auth.LoginUserInfoVo;
import com.aurora.entity.SysUser;
import com.aurora.enums.MenuTypeEnum;
import com.aurora.service.SysMenuService;
import com.aurora.service.SysRoleService;
import com.aurora.service.SysUserService;
import com.aurora.starter.common.utils.bean.BeanUtils;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthBizService {

    private static final long SESSION_TIMEOUT_SECONDS = 60 * 60;
    private static final long REMEMBER_ME_TIMEOUT_SECONDS = 3 * 24 * 60 * 60;

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;

    public LoginUserInfoVo login(LoginForm form) {
        SysUser user = sysUserService.getByUsername(form.getUsername());
        validateLogin(form.getPassword(), user);
        SecurityUtils.login(user.getId(), new SaLoginParameter().setTimeout(tokenTimeout(form.isRememberMe())));

        LoginUserInfoVo loginUserInfo = toLoginUserInfo(user);
        loginUserInfo.setToken(SecurityUtils.getTokenValue());
        SecurityUtils.setSessionAttribute(Constants.CURRENT_USER, loginUserInfo);
        return loginUserInfo;
    }

    public void logout() {
        SecurityUtils.logout();
    }

    public LoginUserInfoVo getLoginUserInfo() {
        Integer userId = Math.toIntExact(SecurityUtils.getLoginIdAsLong());
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.ERROR_USER_NOT_EXIST);
        }

        List<String> roles = sysRoleService.listRoleCodesByUserId(userId);
        String buttonType = MenuTypeEnum.BUTTON.getCode();
        List<String> permissions = roles.contains(Constants.ADMIN)
                ? sysMenuService.listPermissions(buttonType)
                : sysMenuService.listPermissionsByUserId(userId, buttonType);

        LoginUserInfoVo loginUserInfo = toLoginUserInfo(user);
        loginUserInfo.setRoles(roles);
        loginUserInfo.setPermissions(permissions);
        return loginUserInfo;
    }

    private static void validateLogin(String password, SysUser user) {
        if (user == null) {
            throw new BizException(ResultCode.ERROR_USER_NOT_EXIST);
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException(ResultCode.ERROR_PASSWORD);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.DISABLE_ACCOUNT);
        }
    }

    private static LoginUserInfoVo toLoginUserInfo(SysUser user) {
        LoginUserInfoVo loginUserInfo = new LoginUserInfoVo();
        BeanUtils.copyProperties(user, loginUserInfo);
        return loginUserInfo;
    }

    private static long tokenTimeout(boolean rememberMe) {
        return rememberMe ? REMEMBER_ME_TIMEOUT_SECONDS : SESSION_TIMEOUT_SECONDS;
    }
}
