package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.common.utils.bean.BeanUtils;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.ImageVerificationException;
import com.aurora.starter.verification.exception.VerificationException;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.security.NexoraPermissionProvider;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.system.config.SysConfigGroupReader;
import com.nexora.constants.CommonConstants;
import com.nexora.identity.constants.SysUserStatusEnum;
import com.nexora.constants.ResultCode;
import com.nexora.identity.domain.form.AuthForm;
import com.nexora.system.domain.form.LoginConfigForm;
import com.nexora.system.domain.form.RegisterConfigForm;
import com.nexora.identity.domain.vo.LoginUserInfoVo;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import com.nexora.identity.utils.VerificationMailTemplateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthBizService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final NexoraPermissionProvider permissionProvider;
    private final SysConfigGroupReader configReader;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final LoginSecurityService loginSecurityService;
    private final ObjectProvider<MailVerificationService> mailVerificationServiceProvider;
    private final ImageVerificationService imageVerificationService;

    public LoginUserInfoVo login(AuthForm form) {
        String email = StringUtils.normalizeEmail(
                requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        String password = requireLoginPassword(form.getPassword());
        LoginConfigForm loginConfig = configReader.login();
        if (Boolean.TRUE.equals(loginConfig.getCaptchaEnabled())) {
            verifyImageCaptcha(requireText(
                    form.getCaptchaId(), CommonConstants.IMAGE_CAPTCHA_REQUIRED_MESSAGE));
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
        SecurityUtils.setSessionAttribute(CommonConstants.CURRENT_USER, loginUserInfo);
        return loginUserInfo;
    }

    public void sendRegisterCode(AuthForm form) {
        RegisterConfigForm registerConfig = requireRegistrationConfig();
        requireRegistrationRole(registerConfig);
        if (!Boolean.TRUE.equals(registerConfig.getVerifyEmail())) {
            throw new BizException(CommonConstants.REGISTER_EMAIL_VERIFICATION_DISABLED_MESSAGE);
        }
        String email = StringUtils.normalizeEmail(
                requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        ensureEmailAvailable(email);

        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
        try {
            verificationService.send(VerificationMailTemplateUtils.createRequest(
                    email, CommonVerificationScene.REGISTER));
        } catch (VerificationCooldownException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_TOO_FREQUENT_MESSAGE);
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
    }

    public ImageCaptchaVO generateImageCaptcha() {
        return imageVerificationService.generate();
    }

    public boolean matchImageCaptcha(String captchaId, ImageCaptchaTrack track) {
        return imageVerificationService.match(captchaId, track);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(AuthForm form) {
        RegisterConfigForm registerConfig = requireRegistrationConfig();
        SysRole role = requireRegistrationRole(registerConfig);
        String email = StringUtils.normalizeEmail(
                requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        String password = passwordPolicyValidator.validateNewPassword(form.getPassword());
        String captchaId = requireText(
                form.getCaptchaId(), CommonConstants.IMAGE_CAPTCHA_REQUIRED_MESSAGE);
        ensureEmailAvailable(email);
        verifyImageCaptcha(captchaId);
        if (Boolean.TRUE.equals(registerConfig.getVerifyEmail())) {
            verifyRegisterCode(email, requireVerificationCode(form.getCode()));
        }

        SysUser user = new SysUser();
        user.setEmail(email);
        user.setNickname(createNickname(email));
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setStatus(Boolean.TRUE.equals(registerConfig.getNeedAudit())
                ? SysUserStatusEnum.PENDING.getCode() : SysUserStatusEnum.NORMAL.getCode());
        try {
            if (!sysUserService.save(user)) {
                throw new BizException(CommonConstants.REGISTER_FAILED_MESSAGE);
            }
        } catch (DuplicateKeyException exception) {
            throw new BizException(CommonConstants.EMAIL_IN_USE_MESSAGE);
        }
        sysRoleService.addUserRoles(user.getId(), List.of(role.getId()));
    }

    public void sendResetPasswordCode(AuthForm form) {
        String email = StringUtils.normalizeEmail(
                requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        requireExistingUser(email);

        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
        try {
            verificationService.send(VerificationMailTemplateUtils.createRequest(
                    email, CommonVerificationScene.RESET_PASSWORD));
        } catch (VerificationCooldownException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_TOO_FREQUENT_MESSAGE);
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AuthForm form) {
        String email = StringUtils.normalizeEmail(
                requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        String code = requireVerificationCode(form.getCode());
        String password = passwordPolicyValidator.validateNewPassword(form.getPassword());
        SysUser user = requireExistingUser(email);
        verifyResetPasswordCode(email, code);

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        if (!sysUserService.updateById(update)) {
            throw new BizException(CommonConstants.PASSWORD_RESET_FAILED_MESSAGE);
        }
        SecurityUtils.kickout(user.getId());
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

    private RegisterConfigForm requireRegistrationConfig() {
        RegisterConfigForm config = configReader.register();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new BizException(CommonConstants.REGISTER_DISABLED_MESSAGE);
        }
        return config;
    }

    private SysRole requireRegistrationRole(RegisterConfigForm config) {
        SysRole role = sysRoleService.getByCode(config.getDefaultRoleCode());
        if (role == null) {
            throw new BizException(CommonConstants.REGISTER_CONFIG_INCOMPLETE_MESSAGE);
        }
        return role;
    }

    private void verifyRegisterCode(String email, String code) {
        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(CommonConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }
        boolean verified;
        try {
            verified = verificationService.verifyAndConsume(new MailVerificationVerifyRequest(
                    email, CommonVerificationScene.REGISTER, code));
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }
        if (!verified) {
            throw new BizException(CommonConstants.EMAIL_CODE_INVALID_MESSAGE);
        }
    }

    private void verifyImageCaptcha(String captchaId) {
        boolean verified;
        try {
            verified = imageVerificationService.verifyAndConsume(captchaId);
        } catch (ImageVerificationException | IllegalArgumentException exception) {
            throw new BizException(CommonConstants.IMAGE_CAPTCHA_VERIFY_FAILED_MESSAGE);
        }
        if (!verified) {
            throw new BizException(CommonConstants.IMAGE_CAPTCHA_INVALID_MESSAGE);
        }
    }

    private void verifyResetPasswordCode(String email, String code) {
        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(CommonConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }
        boolean verified;
        try {
            verified = verificationService.verifyAndConsume(new MailVerificationVerifyRequest(
                    email, CommonVerificationScene.RESET_PASSWORD, code));
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }
        if (!verified) {
            throw new BizException(CommonConstants.EMAIL_CODE_INVALID_MESSAGE);
        }
    }

    private SysUser requireExistingUser(String email) {
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            throw new BizException(CommonConstants.EMAIL_NOT_REGISTERED_MESSAGE);
        }
        return user;
    }

    private void ensureEmailAvailable(String email) {
        if (sysUserService.getByEmail(email) != null) {
            throw new BizException(CommonConstants.EMAIL_IN_USE_MESSAGE);
        }
    }

    private static String createNickname(String email) {
        int separatorIndex = email.indexOf('@');
        String nickname = separatorIndex > 0 ? email.substring(0, separatorIndex) : email;
        return nickname.substring(0, Math.min(nickname.length(), CommonConstants.MAX_NICKNAME_LENGTH));
    }

    private static String requireLoginPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BizException(CommonConstants.PASSWORD_REQUIRED_MESSAGE);
        }
        return password;
    }

    private static String requireVerificationCode(String code) {
        String value = requireText(code, CommonConstants.EMAIL_CODE_REQUIRED_MESSAGE);
        if (!value.matches(CommonConstants.EMAIL_CODE_PATTERN)) {
            throw new BizException(CommonConstants.EMAIL_CODE_FORMAT_INVALID_MESSAGE);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(message);
        }
        return value.trim();
    }

    private static LoginUserInfoVo toLoginUserInfo(SysUser user) {
        LoginUserInfoVo loginUserInfo = new LoginUserInfoVo();
        BeanUtils.copyProperties(user, loginUserInfo);
        return loginUserInfo;
    }

}
