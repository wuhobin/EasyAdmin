package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.VerificationException;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.constants.SecurityConstants;
import com.nexora.identity.constants.SysUserStatusEnum;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.identity.domain.convert.SysUserConvert;
import com.nexora.identity.domain.form.SysUserQueryForm;
import com.nexora.identity.domain.form.SysUserForm;
import com.nexora.identity.domain.vo.SysUserPageListVo;
import com.nexora.identity.domain.vo.SysUserProfileVo;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.cache.SecurityAuthorizationCache;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import com.nexora.contract.UserDeletionCleanup;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SysUserBizService {
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SecurityAuthorizationCache authorizationCache;
    private final ObjectProvider<MailVerificationService> mailVerificationServiceProvider;
    private final List<UserDeletionCleanup> userDeletionCleanups;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public IPage<SysUserPageListVo> list(SysUserQueryForm form, PageParam pageParam) {
        return sysUserService.listUsers(SysUserConvert.INSTANCE.toQuery(form), pageParam);
    }
    @Transactional(rollbackFor = Exception.class)
    public void add(SysUserForm form) {
        int status = form.getStatus() == null
                ? SysUserStatusEnum.NORMAL.getCode()
                : requireSupportedStatus(form.getStatus());
        String nickname = requireNickname(form.getNickname());
        String email = requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE);
        String password = passwordPolicyValidator.validateNewPassword(form.getPassword());
        requireRoleIds(form.getRoleIds());

        SysUser user = new SysUser();
        user.setNickname(nickname);
        user.setEmail(StringUtils.normalizeEmail(email));
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setStatus(status);
        user.setAvatar(form.getAvatar());
        user.setMobile(form.getMobile());
        user.setSex(form.getSex());
        ensureEmailAvailable(user.getEmail(), null);
        try {
            sysUserService.save(user);
        } catch (DuplicateKeyException exception) {
            throw new BizException(IdentityConstants.EMAIL_IN_USE_MESSAGE);
        }
        sysRoleService.addUserRoles(user.getId(), form.getRoleIds());
        authorizationCache.evictUsersAfterCommit(List.of(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserForm form) {
        Integer userId = requireUserId(form.getId());
        Integer status = form.getStatus() == null ? null : requireSupportedStatus(form.getStatus());
        String nickname = requireNickname(form.getNickname());
        requireRoleIds(form.getRoleIds());

        SysUser existing = sysUserService.getById(userId);
        if (existing == null) {
            throw new BizException(IdentityConstants.USER_NOT_FOUND_MESSAGE);
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(nickname);
        user.setStatus(status);
        user.setAvatar(form.getAvatar());
        user.setMobile(form.getMobile());
        user.setSex(form.getSex());
        protectRootUser(user, form.getRoleIds());
        sysUserService.updateById(user);
        sysRoleService.deleteUserRoles(List.of(user.getId()));
        sysRoleService.addUserRoles(user.getId(), form.getRoleIds());
        authorizationCache.evictUsersAfterCommit(List.of(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Integer> ids) {
        if (ids.contains(IdentityConstants.ROOT_USER_ID)) {
            throw new BizException(IdentityConstants.ROOT_USER_DELETE_FORBIDDEN_MESSAGE);
        }
        userDeletionCleanups.forEach(cleanup -> cleanup.cleanup(ids));
        sysUserService.removeBatchByIds(ids);
        sysRoleService.deleteUserRoles(ids);
        authorizationCache.evictUsersAfterCommit(ids);
    }

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
        user.setNickname(requireNickname(form.getNickname()));
        user.setAvatar(form.getAvatar());
        user.setMobile(form.getMobile());
        user.setSex(form.getSex());
        sysUserService.updateById(user);
    }

    public void sendEmailCode(SysUserForm form) {
        SysUser currentUser = getCurrentUser();
        String email = StringUtils.normalizeEmail(requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        validateNewEmail(currentUser, email);

        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(IdentityConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
        try {
            verificationService.send(VerificationMailRequestFactory.createRequest(
                    email, CommonVerificationScene.CHANGE_EMAIL));
        } catch (VerificationCooldownException exception) {
            throw new BizException(IdentityConstants.EMAIL_CODE_SEND_TOO_FREQUENT_MESSAGE);
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(IdentityConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeEmail(SysUserForm form) {
        SysUser currentUser = getCurrentUser();
        String email = StringUtils.normalizeEmail(requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE));
        String code = requireText(form.getCode(), IdentityConstants.EMAIL_CODE_REQUIRED_MESSAGE);
        validateNewEmail(currentUser, email);

        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(IdentityConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }

        boolean verified;
        try {
            verified = verificationService.verifyAndConsume(new MailVerificationVerifyRequest(
                    email, CommonVerificationScene.CHANGE_EMAIL, code));
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(IdentityConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }
        if (!verified) {
            throw new BizException(IdentityConstants.EMAIL_CODE_INVALID_MESSAGE);
        }

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

    public boolean resetPassword(SysUserForm form) {
        Integer userId = requireUserId(form.getId());
        String password = passwordPolicyValidator.validateNewPassword(form.getPassword());
        if (sysUserService.getById(userId) == null) {
            throw new BizException(IdentityConstants.USER_NOT_FOUND_MESSAGE);
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        sysUserService.updateById(user);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Integer id) {
        Integer userId = requireUserId(id);
        SysUser current = sysUserService.getById(userId);
        if (current == null) {
            throw new BizException(IdentityConstants.USER_NOT_FOUND_MESSAGE);
        }
        if (!Integer.valueOf(SysUserStatusEnum.PENDING.getCode()).equals(current.getStatus())) {
            throw new BizException(IdentityConstants.USER_NOT_PENDING_MESSAGE);
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(SysUserStatusEnum.NORMAL.getCode());
        if (!sysUserService.updateById(update)) {
            throw new BizException(IdentityConstants.USER_AUDIT_FAILED_MESSAGE);
        }
        authorizationCache.evictUsersAfterCommit(List.of(userId));
    }

    private Integer requireUserId(Integer userId) {
        if (userId == null) {
            throw new BizException(IdentityConstants.USER_ID_REQUIRED_MESSAGE);
        }
        return userId;
    }

    private int requireSupportedStatus(Integer status) {
        if (!SysUserStatusEnum.supports(status)) {
            throw new BizException(IdentityConstants.USER_STATUS_INVALID_MESSAGE);
        }
        return status;
    }

    private String requireNickname(String nickname) {
        String value = requireText(nickname, IdentityConstants.NICKNAME_REQUIRED_MESSAGE);
        if (value.length() > 30) {
            throw new BizException(IdentityConstants.NICKNAME_TOO_LONG_MESSAGE);
        }
        return value;
    }

    private String requireCurrentPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BizException(IdentityConstants.PASSWORD_REQUIRED_MESSAGE);
        }
        return password;
    }

    private void requireRoleIds(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BizException(IdentityConstants.ROLE_REQUIRED_MESSAGE);
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(message);
        }
        return value.trim();
    }

    private void validateNewEmail(SysUser currentUser, String email) {
        if (Objects.equals(StringUtils.normalizeEmail(currentUser.getEmail()), email)) {
            throw new BizException(IdentityConstants.EMAIL_UNCHANGED_MESSAGE);
        }
        ensureEmailAvailable(email, currentUser.getId());
    }

    private void ensureEmailAvailable(String email, Integer currentUserId) {
        SysUser existing = sysUserService.getByEmail(email);
        if (existing != null && !Objects.equals(existing.getId(), currentUserId)) {
            throw new BizException(IdentityConstants.EMAIL_IN_USE_MESSAGE);
        }
    }

    private void protectRootUser(SysUser user, List<Integer> roleIds) {
        if (!Objects.equals(user.getId(), IdentityConstants.ROOT_USER_ID)) {
            return;
        }
        if (user.getStatus() != null
                && user.getStatus() != SysUserStatusEnum.NORMAL.getCode()) {
            throw new BizException(IdentityConstants.ROOT_USER_DISABLE_FORBIDDEN_MESSAGE);
        }
        boolean hasAdminRole = sysRoleService.listByIds(roleIds).stream()
                .anyMatch(role -> SecurityConstants.ADMIN_ROLE_CODE.equals(role.getCode()));
        if (!hasAdminRole) {
            throw new BizException(IdentityConstants.ROOT_USER_ADMIN_ROLE_REQUIRED_MESSAGE);
        }
    }

    private SysUser getCurrentUser() {
        SysUser user = sysUserService.getById(SecurityUtils.getLoginIdAsInt());
        if (user == null) {
            throw new BizException(IdentityConstants.USER_NOT_FOUND_MESSAGE);
        }
        return user;
    }
}
