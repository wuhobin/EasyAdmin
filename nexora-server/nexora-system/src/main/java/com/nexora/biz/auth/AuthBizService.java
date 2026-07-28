package com.nexora.biz.auth;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.security.account.AccountType;
import com.nexora.config.NexoraPermissionProvider;
import com.nexora.constants.CommonConstants;
import com.nexora.constants.ResultCode;
import com.nexora.domain.form.auth.LoginForm;
import com.nexora.domain.vo.auth.LoginUserInfoVo;
import com.nexora.entity.SysUser;
import com.nexora.service.SysUserService;
import com.aurora.starter.common.utils.bean.BeanUtils;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthBizService {

    private static final long SESSION_TIMEOUT_SECONDS = 60 * 60;
    private static final long REMEMBER_ME_TIMEOUT_SECONDS = 3 * 24 * 60 * 60;

    private final SysUserService sysUserService;
    private final NexoraPermissionProvider permissionProvider;

    public LoginUserInfoVo login(LoginForm form) {
        SysUser user = sysUserService.getByEmail(StringUtils.normalizeEmail(form.getEmail()));
        validateLogin(form.getPassword(), user);
        SecurityUtils.login(user.getId(), new SaLoginParameter().setTimeout(tokenTimeout(form.isRememberMe())));

        LoginUserInfoVo loginUserInfo = toLoginUserInfo(user);
        loginUserInfo.setToken(SecurityUtils.getTokenValue());
        SecurityUtils.setSessionAttribute(CommonConstants.CURRENT_USER, loginUserInfo);
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

        var authorization = permissionProvider.getAuthorization(userId, AccountType.LOGIN);

        LoginUserInfoVo loginUserInfo = toLoginUserInfo(user);
        loginUserInfo.setRoles(authorization.roles());
        loginUserInfo.setPermissions(authorization.permissions());
        return loginUserInfo;
    }

    private static void validateLogin(String password, SysUser user) {
        if (user == null || user.getPassword() == null
                || !BCrypt.checkpw(password, user.getPassword())) {
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

    static long tokenTimeout(boolean rememberMe) {
        return rememberMe ? REMEMBER_ME_TIMEOUT_SECONDS : SESSION_TIMEOUT_SECONDS;
    }
}
