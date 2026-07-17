package com.aurora.biz;

import com.aurora.domain.convert.SysUserConvert;
import com.aurora.domain.form.query.system.SysUserQueryForm;
import com.aurora.domain.form.system.ResetPasswordForm;
import com.aurora.domain.form.system.SysUserForm;
import com.aurora.domain.form.system.UpdatePasswordForm;
import com.aurora.domain.form.system.UserProfileForm;
import com.aurora.domain.vo.user.SysUserPageListVo;
import com.aurora.domain.vo.user.SysUserProfileVo;
import com.aurora.service.SysUserService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserBizService {
    private final SysUserService sysUserService;

    public IPage<SysUserPageListVo> list(SysUserQueryForm form, PageParam pageParam) {
        return sysUserService.listUsers(SysUserConvert.INSTANCE.toQuery(form), pageParam)
                .convert(SysUserConvert.INSTANCE::toVo);
    }
    public void add(SysUserForm form) {
        sysUserService.add(SysUserConvert.INSTANCE.toEntity(form.getUser()), form.getRoleIds());
    }
    public void update(SysUserForm form) {
        sysUserService.update(SysUserConvert.INSTANCE.toEntity(form.getUser()), form.getRoleIds());
    }
    public void delete(List<Integer> ids) { sysUserService.delete(ids); }
    public void updatePassword(UpdatePasswordForm form) {
        sysUserService.updatePwd(form.getOldPassword(), form.getNewPassword());
    }
    public SysUserProfileVo profile() { return SysUserConvert.INSTANCE.toVo(sysUserService.profile()); }
    public void updateProfile(UserProfileForm form) {
        sysUserService.updateProfile(SysUserConvert.INSTANCE.toEntity(form));
    }
    public boolean verifyPassword(String password) { return sysUserService.verifyPassword(password); }
    public boolean resetPassword(ResetPasswordForm form) {
        return sysUserService.resetPassword(SysUserConvert.INSTANCE.toEntity(form));
    }
}
