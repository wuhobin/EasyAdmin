package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.identity.domain.convert.SysUserConvert;
import com.nexora.identity.domain.form.SysUserForm;
import com.nexora.identity.domain.vo.SysUserProfileVo;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.infrastructure.InputValidator;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final MailVerificationOrchestrator mailVerificationOrchestrator;

    public void updatePassword(SysUserForm form) {
        String oldPassword = requireCurrentPassword(form.getOldPassword());
        String newPassword = passwordPolicyValidator.validateNewPassword(form.getNewPassword());
        SysUser user = getCurrentUser();
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BizException(IdentityConstants.OLD_PASSWORD_INCORRECT_MESSAGE);
        }
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        sysUserService.updateById(user);
    }

    public SysUserProfileVo profile() {
        SysUser user = getCurrentUser();
        user.setPassword(null);
        List<String> roles = sysRoleService.listRoleNamesByUserId(user.getId());
        return new SysUserProfileVo(SysUserConvert.INSTANCE.toVo(user), roles);
    }

    public void updateProfile(SysUserForm form) {
        SysUser user = new SysUser();
        user.setId(SecurityUtils.getLoginIdAsInt());
        user.setNickname(InputValidator.requireText(form.getNickname(), IdentityConstants.NICKNAME_REQUIRED_MESSAGE));
        user.setAvatar(form.getAvatar());
        user.setMobile(form.getMobile());
        user.setSex(form.getSex());
        sysUserService.updateById(user);
    }

    public void sendEmailCode(SysUserForm form) {
        SysUser currentUser = getCurrentUser();
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        validateNewEmail(currentUser, email);
        mailVerificationOrchestrator.sendCode(email, CommonVerificationScene.CHANGE_EMAIL);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeEmail(SysUserForm form) {
        SysUser currentUser = getCurrentUser();
        String email = StringUtils.normalizeEmail(
                InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        String code = InputValidator.requireText(form.getCode(), IdentityConstants.EMAIL_CODE_REQUIRED_MESSAGE);
        validateNewEmail(currentUser, email);

        mailVerificationOrchestrator.verifyCode(email, CommonVerificationScene.CHANGE_EMAIL, code);

        SysUser update = new SysUser();
        update.setId(currentUser.getId());
        update.setEmail(email);
        try {
            sysUserService.updateById(update);
        } catch (DuplicateKeyException exception) {
            throw new BizException(IdentityConstants.EMAIL_IN_USE_MESSAGE);
        }
    }

    public boolean verifyPassword(String password) {
        return BCrypt.checkpw(password, getCurrentUser().getPassword());
    }

    // --- helpers ---

    private String requireCurrentPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BizException(IdentityConstants.PASSWORD_REQUIRED_MESSAGE);
        }
        return password;
    }

    private void validateNewEmail(SysUser currentUser, String email) {
        if (Objects.equals(StringUtils.normalizeEmail(currentUser.getEmail()), email)) {
            throw new BizException(IdentityConstants.EMAIL_UNCHANGED_MESSAGE);
        }
        SysUser existing = sysUserService.getByEmail(email);
        if (existing != null && !Objects.equals(existing.getId(), currentUser.getId())) {
            throw new BizException(IdentityConstants.EMAIL_IN_USE_MESSAGE);
        }
    }

    SysUser getCurrentUser() {
        SysUser user = sysUserService.getById(SecurityUtils.getLoginIdAsInt());
        if (user == null) {
            throw new BizException(IdentityConstants.USER_NOT_FOUND_MESSAGE);
        }
        return user;
    }
}
