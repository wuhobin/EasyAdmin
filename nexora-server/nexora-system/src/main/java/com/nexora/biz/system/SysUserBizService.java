package com.nexora.biz.system;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.VerificationException;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.nexora.constants.CommonConstants;
import com.nexora.domain.convert.SysUserConvert;
import com.nexora.domain.form.query.system.SysUserQueryForm;
import com.nexora.domain.form.system.SysUserForm;
import com.nexora.domain.vo.user.SysUserPageListVo;
import com.nexora.domain.vo.user.SysUserProfileVo;
import com.nexora.entity.SysUser;
import com.nexora.cache.SecurityAuthorizationCache;
import com.nexora.service.SysRoleService;
import com.nexora.service.SysUserService;
import com.nexora.utils.VerificationMailTemplateUtils;
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

    public IPage<SysUserPageListVo> list(SysUserQueryForm form, PageParam pageParam) {
        return sysUserService.listUsers(SysUserConvert.INSTANCE.toQuery(form), pageParam);
    }
    @Transactional(rollbackFor = Exception.class)
    public void add(SysUserForm form) {
        String nickname = requireNickname(form.getNickname());
        String email = requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE);
        String password = requirePassword(form.getPassword());
        requireRoleIds(form.getRoleIds());

        SysUser user = new SysUser();
        user.setNickname(nickname);
        user.setEmail(StringUtils.normalizeEmail(email));
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setStatus(form.getStatus() == null ? CommonConstants.YES : form.getStatus());
        user.setAvatar(form.getAvatar());
        user.setMobile(form.getMobile());
        user.setSex(form.getSex());
        ensureEmailAvailable(user.getEmail(), null);
        try {
            sysUserService.save(user);
        } catch (DuplicateKeyException exception) {
            throw new BizException(CommonConstants.EMAIL_IN_USE_MESSAGE);
        }
        sysRoleService.addUserRoles(user.getId(), form.getRoleIds());
        authorizationCache.evictUsersAfterCommit(List.of(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserForm form) {
        Integer userId = requireUserId(form.getId());
        String nickname = requireNickname(form.getNickname());
        requireRoleIds(form.getRoleIds());

        SysUser existing = sysUserService.getById(userId);
        if (existing == null) {
            throw new BizException(CommonConstants.USER_NOT_FOUND_MESSAGE);
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(nickname);
        user.setStatus(form.getStatus());
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
        if (ids.contains(CommonConstants.ROOT_USER_ID)) {
            throw new BizException(CommonConstants.ROOT_USER_DELETE_FORBIDDEN_MESSAGE);
        }
        sysUserService.removeBatchByIds(ids);
        sysRoleService.deleteUserRoles(ids);
        authorizationCache.evictUsersAfterCommit(ids);
    }

    public void updatePassword(SysUserForm form) {
        String oldPassword = requirePassword(form.getOldPassword());
        String newPassword = requirePassword(form.getNewPassword());
        SysUser user = getCurrentUser();
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BizException(CommonConstants.OLD_PASSWORD_INCORRECT_MESSAGE);
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
        String email = StringUtils.normalizeEmail(requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        validateNewEmail(currentUser, email);

        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
        try {
            verificationService.send(VerificationMailTemplateUtils.createRequest(
                    email, CommonVerificationScene.CHANGE_EMAIL));
        } catch (VerificationCooldownException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_TOO_FREQUENT_MESSAGE);
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeEmail(SysUserForm form) {
        SysUser currentUser = getCurrentUser();
        String email = StringUtils.normalizeEmail(requireText(form.getEmail(), CommonConstants.EMAIL_REQUIRED_MESSAGE));
        String code = requireText(form.getCode(), CommonConstants.EMAIL_CODE_REQUIRED_MESSAGE);
        validateNewEmail(currentUser, email);

        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(CommonConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }

        boolean verified;
        try {
            verified = verificationService.verifyAndConsume(new MailVerificationVerifyRequest(
                    email, CommonVerificationScene.CHANGE_EMAIL, code));
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(CommonConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }
        if (!verified) {
            throw new BizException(CommonConstants.EMAIL_CODE_INVALID_MESSAGE);
        }

        SysUser update = new SysUser();
        update.setId(currentUser.getId());
        update.setEmail(email);
        try {
            sysUserService.updateById(update);
        } catch (DuplicateKeyException exception) {
            throw new BizException(CommonConstants.EMAIL_IN_USE_MESSAGE);
        }
    }

    public boolean verifyPassword(String password) {
        return BCrypt.checkpw(password, getCurrentUser().getPassword());
    }

    public boolean resetPassword(SysUserForm form) {
        Integer userId = requireUserId(form.getId());
        String password = requirePassword(form.getPassword());
        if (sysUserService.getById(userId) == null) {
            throw new BizException(CommonConstants.USER_NOT_FOUND_MESSAGE);
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        sysUserService.updateById(user);
        return true;
    }

    private Integer requireUserId(Integer userId) {
        if (userId == null) {
            throw new BizException(CommonConstants.USER_ID_REQUIRED_MESSAGE);
        }
        return userId;
    }

    private String requireNickname(String nickname) {
        String value = requireText(nickname, CommonConstants.NICKNAME_REQUIRED_MESSAGE);
        if (value.length() > 30) {
            throw new BizException(CommonConstants.NICKNAME_TOO_LONG_MESSAGE);
        }
        return value;
    }

    private String requirePassword(String password) {
        String value = requireText(password, CommonConstants.PASSWORD_REQUIRED_MESSAGE);
        if (value.length() < 6 || value.length() > 20) {
            throw new BizException(CommonConstants.PASSWORD_LENGTH_INVALID_MESSAGE);
        }
        return value;
    }

    private void requireRoleIds(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BizException(CommonConstants.ROLE_REQUIRED_MESSAGE);
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
            throw new BizException(CommonConstants.EMAIL_UNCHANGED_MESSAGE);
        }
        ensureEmailAvailable(email, currentUser.getId());
    }

    private void ensureEmailAvailable(String email, Integer currentUserId) {
        SysUser existing = sysUserService.getByEmail(email);
        if (existing != null && !Objects.equals(existing.getId(), currentUserId)) {
            throw new BizException(CommonConstants.EMAIL_IN_USE_MESSAGE);
        }
    }

    private void protectRootUser(SysUser user, List<Integer> roleIds) {
        if (!Objects.equals(user.getId(), CommonConstants.ROOT_USER_ID)) {
            return;
        }
        if (user.getStatus() != null && user.getStatus() != CommonConstants.YES) {
            throw new BizException(CommonConstants.ROOT_USER_DISABLE_FORBIDDEN_MESSAGE);
        }
        boolean hasAdminRole = sysRoleService.listByIds(roleIds).stream()
                .anyMatch(role -> CommonConstants.ADMIN.equals(role.getCode()));
        if (!hasAdminRole) {
            throw new BizException(CommonConstants.ROOT_USER_ADMIN_ROLE_REQUIRED_MESSAGE);
        }
    }

    private SysUser getCurrentUser() {
        SysUser user = sysUserService.getById(SecurityUtils.getLoginIdAsInt());
        if (user == null) {
            throw new BizException(CommonConstants.USER_NOT_FOUND_MESSAGE);
        }
        return user;
    }
}
