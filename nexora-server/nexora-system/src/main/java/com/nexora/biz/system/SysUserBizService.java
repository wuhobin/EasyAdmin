package com.nexora.biz.system;

import cn.dev33.satoken.secure.BCrypt;
import com.nexora.domain.convert.SysUserConvert;
import com.nexora.domain.form.query.system.SysUserQueryForm;
import com.nexora.domain.form.system.ResetPasswordForm;
import com.nexora.domain.form.system.SysUserForm;
import com.nexora.domain.form.system.UpdatePasswordForm;
import com.nexora.domain.form.system.UserProfileForm;
import com.nexora.domain.vo.user.SysUserPageListVo;
import com.nexora.domain.vo.user.SysUserProfileVo;
import com.nexora.entity.SysUser;
import com.nexora.cache.SecurityAuthorizationCache;
import com.nexora.service.SysRoleService;
import com.nexora.service.SysUserService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserBizService {
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SecurityAuthorizationCache authorizationCache;

    public IPage<SysUserPageListVo> list(SysUserQueryForm form, PageParam pageParam) {
        return sysUserService.listUsers(SysUserConvert.INSTANCE.toQuery(form), pageParam);
    }
    @Transactional(rollbackFor = Exception.class)
    public void add(SysUserForm form) {
        SysUser user = SysUserConvert.INSTANCE.toEntity(form.getUser());
        if (sysUserService.getByUsername(user.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        sysUserService.save(user);
        sysRoleService.addUserRoles(user.getId(), form.getRoleIds());
        authorizationCache.evictUsersAfterCommit(List.of(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserForm form) {
        SysUser user = SysUserConvert.INSTANCE.toEntity(form.getUser());
        if (sysUserService.getById(user.getId()) == null) {
            throw new BizException("用户不存在");
        }
        sysUserService.updateById(user);
        sysRoleService.deleteUserRoles(List.of(user.getId()));
        sysRoleService.addUserRoles(user.getId(), form.getRoleIds());
        authorizationCache.evictUsersAfterCommit(List.of(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Integer> ids) {
        sysUserService.removeBatchByIds(ids);
        sysRoleService.deleteUserRoles(ids);
        authorizationCache.evictUsersAfterCommit(ids);
    }

    public void updatePassword(UpdatePasswordForm form) {
        SysUser user = getCurrentUser();
        if (!BCrypt.checkpw(form.getOldPassword(), user.getPassword())) {
            throw new BizException("旧密码错误");
        }
        user.setPassword(BCrypt.hashpw(form.getNewPassword(), BCrypt.gensalt()));
        sysUserService.updateById(user);
    }

    public SysUserProfileVo profile() {
        SysUser user = getCurrentUser();
        user.setPassword(null);
        List<String> roles = sysRoleService.listRoleNamesByUserId(user.getId());
        return new SysUserProfileVo(SysUserConvert.INSTANCE.toVo(user), roles);
    }

    public void updateProfile(UserProfileForm form) {
        SysUser user = SysUserConvert.INSTANCE.toEntity(form);
        user.setId(SecurityUtils.getLoginIdAsInt());
        sysUserService.updateById(user);
    }

    public boolean verifyPassword(String password) {
        return BCrypt.checkpw(password, getCurrentUser().getPassword());
    }

    public boolean resetPassword(ResetPasswordForm form) {
        SysUser user = SysUserConvert.INSTANCE.toEntity(form);
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        sysUserService.updateById(user);
        return true;
    }

    private SysUser getCurrentUser() {
        SysUser user = sysUserService.getById(SecurityUtils.getLoginIdAsInt());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }
}
