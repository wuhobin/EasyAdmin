package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.identity.constants.SysUserStatusEnum;
import com.nexora.identity.domain.form.AuthForm;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.infrastructure.InputValidator;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SystemConfigReader configReader;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final MailVerificationOrchestrator mailVerificationOrchestrator;

    public void sendRegisterCode(AuthForm form) {
        RegistrationSettings registerConfig = requireRegistrationConfig();
        requireRegistrationRole(registerConfig);
        if (!Boolean.TRUE.equals(registerConfig.getVerifyEmail())) {
            throw new BizException(IdentityConstants.REGISTER_EMAIL_VERIFICATION_DISABLED_MESSAGE);
        }
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        ensureEmailAvailable(email);
        mailVerificationOrchestrator.sendCode(email, CommonVerificationScene.REGISTER);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(AuthForm form, RegistrationSettings registerConfig, SysRole role) {
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        String password = passwordPolicyValidator.validateNewPassword(form.getPassword());
        ensureEmailAvailable(email);
        // captcha verification is handled by AuthBizService caller

        SysUser user = new SysUser();
        user.setEmail(email);
        user.setNickname(createNickname(email));
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setStatus(Boolean.TRUE.equals(registerConfig.getNeedAudit())
                ? SysUserStatusEnum.PENDING.getCode() : SysUserStatusEnum.NORMAL.getCode());
        try {
            if (!sysUserService.save(user)) {
                throw new BizException(IdentityConstants.REGISTER_FAILED_MESSAGE);
            }
        } catch (DuplicateKeyException exception) {
            throw new BizException(IdentityConstants.EMAIL_IN_USE_MESSAGE);
        }
        sysRoleService.addUserRoles(user.getId(), List.of(role.getId()));
    }

    public RegistrationSettings requireRegistrationConfig() {
        RegistrationSettings config = configReader.register();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new BizException(IdentityConstants.REGISTER_DISABLED_MESSAGE);
        }
        return config;
    }

    public SysRole requireRegistrationRole(RegistrationSettings config) {
        SysRole role = sysRoleService.getByCode(config.getDefaultRoleCode());
        if (role == null) {
            throw new BizException(IdentityConstants.REGISTER_CONFIG_INCOMPLETE_MESSAGE);
        }
        return role;
    }

    public void ensureEmailAvailable(String email) {
        if (sysUserService.getByEmail(email) != null) {
            throw new BizException(IdentityConstants.EMAIL_IN_USE_MESSAGE);
        }
    }

    static String createNickname(String email) {
        int separatorIndex = email.indexOf('@');
        String nickname = separatorIndex > 0 ? email.substring(0, separatorIndex) : email;
        return nickname.substring(0, Math.min(nickname.length(), IdentityConstants.MAX_NICKNAME_LENGTH));
    }
}
