package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.constants.SecurityConstants;
import com.nexora.contract.UserDeletionCleanup;
import com.nexora.contract.UserDisabledCleanup;
import com.nexora.identity.cache.SecurityPermissionCache;
import com.nexora.identity.infrastructure.PasswordPolicyValidator;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.identity.constants.SysUserStatusEnum;
import com.nexora.identity.domain.convert.SysUserConvert;
import com.nexora.identity.domain.form.SysUserForm;
import com.nexora.identity.domain.form.SysUserQueryForm;
import com.nexora.identity.domain.vo.SysUserPageListVo;
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
public class UserManagementService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SecurityPermissionCache authorizationCache;
    private final List<UserDeletionCleanup> userDeletionCleanups;
    private final List<UserDisabledCleanup> userDisabledCleanups;
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
        String email = InputValidator.requireText(form.getEmail(), IdentityConstants.EMAIL_REQUIRED_MESSAGE);
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
        if (Integer.valueOf(SysUserStatusEnum.DISABLED.getCode()).equals(status)
                && !Objects.equals(existing.getStatus(), status)) {
            userDisabledCleanups.forEach(cleanup -> cleanup.cleanup(userId));
        }
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

    // --- helpers ---

    Integer requireUserId(Integer userId) {
        if (userId == null) {
            throw new BizException(IdentityConstants.USER_ID_REQUIRED_MESSAGE);
        }
        return userId;
    }

    int requireSupportedStatus(Integer status) {
        if (!SysUserStatusEnum.supports(status)) {
            throw new BizException(IdentityConstants.USER_STATUS_INVALID_MESSAGE);
        }
        return status;
    }

    String requireNickname(String nickname) {
        String value = InputValidator.requireText(nickname, IdentityConstants.NICKNAME_REQUIRED_MESSAGE);
        if (value.length() > 30) {
            throw new BizException(IdentityConstants.NICKNAME_TOO_LONG_MESSAGE);
        }
        return value;
    }

    void requireRoleIds(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BizException(IdentityConstants.ROLE_REQUIRED_MESSAGE);
        }
    }

    void ensureEmailAvailable(String email, Integer currentUserId) {
        SysUser existing = sysUserService.getByEmail(email);
        if (existing != null && !Objects.equals(existing.getId(), currentUserId)) {
            throw new BizException(IdentityConstants.EMAIL_IN_USE_MESSAGE);
        }
    }

    void protectRootUser(SysUser user, List<Integer> roleIds) {
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
}
