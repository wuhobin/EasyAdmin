package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.identity.domain.form.AuthForm;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.infrastructure.InputValidator;
import com.nexora.identity.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final SysUserService sysUserService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final MailVerificationOrchestrator mailVerificationOrchestrator;
    private final OnlineSessionLifecycleService onlineSessionLifecycleService;

    public void sendResetPasswordCode(AuthForm form) {
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        requireExistingUser(email);
        mailVerificationOrchestrator.sendCode(email, CommonVerificationScene.RESET_PASSWORD);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AuthForm form) {
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        String code = InputValidator.requireText(form.getCode(), IdentityConstants.EMAIL_CODE_REQUIRED_MESSAGE);
        if (!code.matches(IdentityConstants.EMAIL_CODE_PATTERN)) {
            throw new BizException(IdentityConstants.EMAIL_CODE_FORMAT_INVALID_MESSAGE);
        }
        String password = passwordPolicyValidator.validateNewPassword(form.getPassword());
        SysUser user = requireExistingUser(email);
        mailVerificationOrchestrator.verifyCode(email, CommonVerificationScene.RESET_PASSWORD, code);

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        if (!sysUserService.updateById(update)) {
            throw new BizException(IdentityConstants.PASSWORD_RESET_FAILED_MESSAGE);
        }
        onlineSessionLifecycleService.invalidateUserSessions(user.getId());
    }

    public SysUser requireExistingUser(String email) {
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            throw new BizException(IdentityConstants.EMAIL_NOT_REGISTERED_MESSAGE);
        }
        return user;
    }
}
