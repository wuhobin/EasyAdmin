package com.nexora.biz.auth;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.cache.LoginRetryCache;
import com.nexora.constants.CommonConstants;
import com.nexora.constants.ResultCode;
import com.nexora.constants.SysUserStatusEnum;
import com.nexora.domain.form.system.config.LoginConfigForm;
import com.nexora.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginSecurityService {

    private final LoginRetryCache loginRetryCache;

    public void assertNotLocked(String email, LoginConfigForm config) {
        try {
            if (loginRetryCache.getFailureCount(email) >= config.getMaxRetryCount()) {
                throw locked(email);
            }
        } catch (BizException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw securityUnavailable();
        }
    }

    public void validateCredentials(String email, String password, SysUser user,
                                    LoginConfigForm config) {
        if (user != null && user.getPassword() != null
                && BCrypt.checkpw(password, user.getPassword())) {
            return;
        }
        int failureCount = recordFailure(email, config.getLockTimeMinutes());
        if (failureCount >= config.getMaxRetryCount()) {
            throw locked(email);
        }
        throw new BizException(ResultCode.ERROR_PASSWORD);
    }

    public void clearFailures(String email) {
        try {
            loginRetryCache.clear(email);
        } catch (RuntimeException exception) {
            throw securityUnavailable();
        }
    }

    public void validateUserStatus(SysUser user) {
        if (Integer.valueOf(SysUserStatusEnum.PENDING.getCode()).equals(user.getStatus())) {
            throw new BizException(CommonConstants.ACCOUNT_PENDING_MESSAGE);
        }
        if (!Integer.valueOf(SysUserStatusEnum.NORMAL.getCode()).equals(user.getStatus())) {
            throw new BizException(ResultCode.DISABLE_ACCOUNT);
        }
    }

    public static long tokenTimeout(LoginConfigForm config, boolean rememberMe) {
        return Boolean.TRUE.equals(config.getRememberMeEnabled()) && rememberMe
                ? config.getRememberMeTimeoutSeconds() : config.getSessionTimeoutSeconds();
    }

    private int recordFailure(String email, int lockTimeMinutes) {
        try {
            return loginRetryCache.recordFailure(email, lockTimeMinutes);
        } catch (RuntimeException exception) {
            throw securityUnavailable();
        }
    }

    private BizException locked(String email) {
        try {
            return new BizException(CommonConstants.LOGIN_LOCKED_MESSAGE.formatted(
                    loginRetryCache.getRemainingMinutes(email)));
        } catch (RuntimeException exception) {
            return securityUnavailable();
        }
    }

    private static BizException securityUnavailable() {
        return new BizException(CommonConstants.LOGIN_SECURITY_UNAVAILABLE_MESSAGE);
    }
}
