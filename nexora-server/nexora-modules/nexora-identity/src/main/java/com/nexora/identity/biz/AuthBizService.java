package com.nexora.identity.biz;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.common.utils.bean.BeanUtils;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.exception.ImageVerificationException;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.security.NexoraPermissionProvider;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.constants.ResultCode;
import com.nexora.identity.domain.form.AuthForm;
import com.nexora.system.api.LoginSettings;
import com.nexora.system.api.SystemConfigReader;
import com.nexora.identity.domain.vo.LoginUserInfoVo;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.infrastructure.InputValidator;
import com.nexora.identity.service.SysUserService;
import com.nexora.system.api.RegistrationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthBizService {

    private final SysUserService sysUserService;
    private final NexoraPermissionProvider permissionProvider;
    private final SystemConfigReader configReader;
    private final LoginSecurityService loginSecurityService;
    private final ImageVerificationService imageVerificationService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final MailVerificationOrchestrator mailVerificationOrchestrator;
    private final OnlineSessionLifecycleService onlineSessionLifecycleService;

    public LoginUserInfoVo login(AuthForm form) {
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        String password = requireLoginPassword(form.getPassword());
        LoginSettings loginConfig = configReader.login();
        loginSecurityService.assertNotLocked(email, loginConfig);
        SysUser user = sysUserService.getByEmail(email);
        loginSecurityService.validateCredentials(email, password, user, loginConfig);
        loginSecurityService.clearFailures(email);
        loginSecurityService.validateUserStatus(user);
        return loginUser(user, form.isRememberMe());
    }

    public LoginUserInfoVo loginUser(SysUser user, boolean rememberMe) {
        loginSecurityService.validateUserStatus(user);
        LoginSettings loginConfig = configReader.login();
        String sessionId = onlineSessionLifecycleService.createSessionId();
        boolean singleLogin = Boolean.TRUE.equals(loginConfig.getSingleLogin());
        SecurityUtils.login(user.getId(), new SaLoginParameter()
                .setDeviceId(sessionId)
                .setIsConcurrent(!singleLogin)
                .setIsShare(true)
                .setTimeout(LoginSecurityService.tokenTimeout(loginConfig, rememberMe)));

        LoginUserInfoVo loginUserInfo;
        try {
            loginUserInfo = toLoginUserInfo(user);
            loginUserInfo.setToken(SecurityUtils.getTokenValue());
            SecurityUtils.setSessionAttribute(IdentityConstants.CURRENT_USER, loginUserInfo);
        } catch (RuntimeException exception) {
            onlineSessionLifecycleService.rollbackUnregisteredSession(
                    user.getId(), sessionId, exception);
            throw exception;
        }
        onlineSessionLifecycleService.register(user, sessionId);
        return loginUserInfo;
    }

    // ---- Delegated to RegistrationService ----

    public void sendRegisterCode(AuthForm form) {
        registrationService.sendRegisterCode(form);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(AuthForm form) {
        RegistrationSettings registerConfig = registrationService.requireRegistrationConfig();
        SysRole role = registrationService.requireRegistrationRole(registerConfig);
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        String captchaId = null;
        if (Boolean.TRUE.equals(registerConfig.getCaptchaEnabled())) {
            captchaId = InputValidator.requireText(
                    form.getCaptchaId(), IdentityConstants.IMAGE_CAPTCHA_REQUIRED_MESSAGE);
        }
        registrationService.ensureEmailAvailable(email);
        if (captchaId != null) {
            verifyImageCaptcha(captchaId);
        }
        if (Boolean.TRUE.equals(registerConfig.getVerifyEmail())) {
            mailVerificationOrchestrator.verifyCode(email, CommonVerificationScene.REGISTER,
                    InputValidator.requireText(form.getCode(), IdentityConstants.EMAIL_CODE_REQUIRED_MESSAGE));
        }
        registrationService.register(form, registerConfig, role);
    }

    // ---- Delegated to PasswordResetService ----

    public void sendResetPasswordCode(AuthForm form) {
        passwordResetService.sendResetPasswordCode(form);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AuthForm form) {
        passwordResetService.resetPassword(form);
    }

    // ---- Captcha ----

    public ImageCaptchaVO generateImageCaptcha() {
        return imageVerificationService.generate();
    }

    public boolean matchImageCaptcha(String captchaId, ImageCaptchaTrack track) {
        return imageVerificationService.match(captchaId, track);
    }

    // ---- Session ----

    public void logout() {
        onlineSessionLifecycleService.logoutCurrentSession();
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

    // ---- Private helpers ----

    private void verifyImageCaptcha(String captchaId) {
        boolean verified;
        try {
            verified = imageVerificationService.verifyAndConsume(captchaId);
        } catch (ImageVerificationException | IllegalArgumentException exception) {
            throw new BizException(IdentityConstants.IMAGE_CAPTCHA_VERIFY_FAILED_MESSAGE);
        }
        if (!verified) {
            throw new BizException(IdentityConstants.IMAGE_CAPTCHA_INVALID_MESSAGE);
        }
    }

    private static String requireLoginPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BizException(IdentityConstants.PASSWORD_REQUIRED_MESSAGE);
        }
        return password;
    }

    private static LoginUserInfoVo toLoginUserInfo(SysUser user) {
        LoginUserInfoVo loginUserInfo = new LoginUserInfoVo();
        BeanUtils.copyProperties(user, loginUserInfo);
        return loginUserInfo;
    }

}
