package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
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
import com.nexora.identity.constants.SysUserStatusEnum;
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

import java.util.List;

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

    public LoginUserInfoVo login(AuthForm form) {
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        String password = requireLoginPassword(form.getPassword());
        LoginSettings loginConfig = configReader.login();
        if (Boolean.TRUE.equals(loginConfig.getCaptchaEnabled())) {
            verifyImageCaptcha(InputValidator.requireText(
                    form.getCaptchaId(), IdentityConstants.IMAGE_CAPTCHA_REQUIRED_MESSAGE));
        }
        loginSecurityService.assertNotLocked(email, loginConfig);
        SysUser user = sysUserService.getByEmail(email);
        loginSecurityService.validateCredentials(email, password, user, loginConfig);
        loginSecurityService.clearFailures(email);
        loginSecurityService.validateUserStatus(user);
        if (Boolean.TRUE.equals(loginConfig.getSingleLogin())) {
            SecurityUtils.kickout(user.getId());
        }
        SecurityUtils.login(user.getId(), new SaLoginParameter()
                .setTimeout(LoginSecurityService.tokenTimeout(loginConfig, form.isRememberMe())));

        LoginUserInfoVo loginUserInfo = toLoginUserInfo(user);
        loginUserInfo.setToken(SecurityUtils.getTokenValue());
        SecurityUtils.setSessionAttribute(IdentityConstants.CURRENT_USER, loginUserInfo);
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
        String captchaId = InputValidator.requireText(
                form.getCaptchaId(), IdentityConstants.IMAGE_CAPTCHA_REQUIRED_MESSAGE);
        registrationService.ensureEmailAvailable(email);
        verifyImageCaptcha(captchaId);
        if (Boolean.TRUE.equals(registerConfig.getVerifyEmail())) {
            mailVerificationOrchestrator.verifyCode(email, CommonVerificationScene.REGISTER,
                    InputValidator.requireText(form.getCode(), IdentityConstants.EMAIL_CODE_REQUIRED_MESSAGE));
        }
        registrationService.register(form);
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
