package com.nexora.biz.auth;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.common.utils.bean.BeanUtils;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.VerificationException;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.config.NexoraPermissionProvider;
import com.nexora.config.SysConfigReader;
import com.nexora.constants.CommonConstants;
import com.nexora.constants.ResultCode;
import com.nexora.domain.form.auth.AuthForm;
import com.nexora.domain.vo.auth.LoginUserInfoVo;
import com.nexora.entity.SysRole;
import com.nexora.entity.SysUser;
import com.nexora.service.SysRoleService;
import com.nexora.service.SysUserService;
import com.nexora.utils.VerificationMailTemplateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthBizService {

    private static final long SESSION_TIMEOUT_SECONDS = 60 * 60;
    private static final long REMEMBER_ME_TIMEOUT_SECONDS = 3 * 24 * 60 * 60;

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final NexoraPermissionProvider permissionProvider;
    private final SysConfigReader sysConfigReader;
    private final ObjectProvider<MailVerificationService> mailVerificationServiceProvider;

    public LoginUserInfoVo login(AuthForm form) {
        String email = StringUtils.normalizeEmail(
                requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        String password = requirePassword(form.getPassword());
        SysUser user = sysUserService.getByEmail(email);
        validateLogin(password, user);
        SecurityUtils.login(user.getId(), new SaLoginParameter().setTimeout(tokenTimeout(form.isRememberMe())));

        LoginUserInfoVo loginUserInfo = toLoginUserInfo(user);
        loginUserInfo.setToken(SecurityUtils.getTokenValue());
        SecurityUtils.setSessionAttribute(CommonConstants.CURRENT_USER, loginUserInfo);
        return loginUserInfo;
    }

    public void sendRegisterCode(AuthForm form) {
        requireRegistrationRole();
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

    @Transactional(rollbackFor = Exception.class)
    public void register(AuthForm form) {
        SysRole role = requireRegistrationRole();
        String email = StringUtils.normalizeEmail(
                requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        String password = requirePassword(form.getPassword());
        String code = requireVerificationCode(form.getCode());
        ensureEmailAvailable(email);
        verifyRegisterCode(email, code);

        SysUser user = new SysUser();
        user.setEmail(email);
        user.setNickname(createNickname(email));
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setStatus(CommonConstants.YES);
        try {
            if (!sysUserService.save(user)) {
                throw new BizException(CommonConstants.REGISTER_FAILED_MESSAGE);
            }
        } catch (DuplicateKeyException exception) {
            throw new BizException(CommonConstants.EMAIL_IN_USE_MESSAGE);
        }
        sysRoleService.addUserRoles(user.getId(), List.of(role.getId()));
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

    private SysRole requireRegistrationRole() {
        if (!CommonConstants.TRUE_VALUE.equals(sysConfigReader.getString(
                CommonConstants.REGISTER_ENABLED_CONFIG_KEY, null))) {
            throw new BizException(CommonConstants.REGISTER_DISABLED_MESSAGE);
        }
        String roleCode = sysConfigReader.getString(CommonConstants.REGISTER_ROLE_CODE_CONFIG_KEY, null);
        if (roleCode == null || roleCode.isBlank()) {
            throw new BizException(CommonConstants.REGISTER_CONFIG_INCOMPLETE_MESSAGE);
        }
        SysRole role = sysRoleService.getByCode(roleCode.strip());
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

    private static String requirePassword(String password) {
        String value = requireText(password, CommonConstants.PASSWORD_REQUIRED_MESSAGE);
        if (value.length() < 6 || value.length() > 20) {
            throw new BizException(CommonConstants.PASSWORD_LENGTH_INVALID_MESSAGE);
        }
        return value;
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

    private static void validateLogin(String password, SysUser user) {
        if (user == null || user.getPassword() == null
                || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException(ResultCode.ERROR_PASSWORD);
        }
        if (user.getStatus() == null || user.getStatus() != CommonConstants.YES) {
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
