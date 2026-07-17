package com.aurora.domain.convert;

import com.aurora.domain.form.query.monitor.OnlineUserQueryForm;
import com.aurora.domain.form.query.system.SysUserQueryForm;
import com.aurora.domain.form.system.ResetPasswordForm;
import com.aurora.domain.form.system.SysUserDetailForm;
import com.aurora.domain.form.system.UserProfileForm;
import com.aurora.domain.query.monitor.OnlineUserQuery;
import com.aurora.domain.query.system.SysUserQuery;
import com.aurora.entity.SysUser;
import com.aurora.domain.model.user.OnlineUserData;
import com.aurora.domain.model.user.SysUserPageData;
import com.aurora.domain.model.user.SysUserProfileData;
import com.aurora.domain.vo.user.OnlineUserVo;
import com.aurora.domain.vo.user.SysUserPageListVo;
import com.aurora.domain.vo.user.SysUserProfileVo;
import com.aurora.domain.vo.user.SysUserVo;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysUserConvert {
    SysUserConvert INSTANCE = Mappers.getMapper(SysUserConvert.class);
    SysUserQuery toQuery(SysUserQueryForm form);
    OnlineUserQuery toQuery(OnlineUserQueryForm form);
    @BeanMapping(builder = @Builder(disableBuilder = true))
    SysUser toEntity(SysUserDetailForm form);
    @BeanMapping(builder = @Builder(disableBuilder = true))
    SysUser toEntity(UserProfileForm form);
    @BeanMapping(builder = @Builder(disableBuilder = true))
    SysUser toEntity(ResetPasswordForm form);
    SysUserVo toVo(SysUser entity);
    SysUserPageListVo toVo(SysUserPageData data);
    SysUserProfileVo toVo(SysUserProfileData data);
    OnlineUserVo toVo(OnlineUserData data);
}
