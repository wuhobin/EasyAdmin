package com.nexora.identity.biz;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.nexora.identity.domain.form.SysUserForm;
import com.nexora.identity.domain.form.SysUserQueryForm;
import com.nexora.identity.domain.vo.SysUserPageListVo;
import com.nexora.identity.domain.vo.SysUserProfileVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户业务 facade，委托给 {@link UserManagementService} 和 {@link UserProfileService}。
 */
@Service
@RequiredArgsConstructor
public class SysUserBizService {

    private final UserManagementService userManagementService;
    private final UserProfileService userProfileService;

    public IPage<SysUserPageListVo> list(SysUserQueryForm form, PageParam pageParam) {
        return userManagementService.list(form, pageParam);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(SysUserForm form) {
        userManagementService.add(form);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserForm form) {
        userManagementService.update(form);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Integer> ids) {
        userManagementService.delete(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Integer id) {
        userManagementService.audit(id);
    }

    public boolean resetPassword(SysUserForm form) {
        return userManagementService.resetPassword(form);
    }

    public void updatePassword(SysUserForm form) {
        userProfileService.updatePassword(form);
    }

    public SysUserProfileVo profile() {
        return userProfileService.profile();
    }

    public void updateProfile(SysUserForm form) {
        userProfileService.updateProfile(form);
    }

    public void sendEmailCode(SysUserForm form) {
        userProfileService.sendEmailCode(form);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeEmail(SysUserForm form) {
        userProfileService.changeEmail(form);
    }

    public boolean verifyPassword(String password) {
        return userProfileService.verifyPassword(password);
    }
}
