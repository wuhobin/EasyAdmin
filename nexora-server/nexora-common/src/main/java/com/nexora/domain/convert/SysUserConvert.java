package com.nexora.domain.convert;

import com.nexora.domain.form.query.system.SysUserQueryForm;
import com.nexora.domain.form.system.ResetPasswordForm;
import com.nexora.domain.form.system.SysUserDetailForm;
import com.nexora.domain.form.system.UserProfileForm;
import com.nexora.domain.query.system.SysUserQuery;
import com.nexora.entity.SysUser;
import com.nexora.domain.vo.user.SysUserVo;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysUserConvert {
    SysUserConvert INSTANCE = Mappers.getMapper(SysUserConvert.class);
    SysUserQuery toQuery(SysUserQueryForm form);
    @BeanMapping(builder = @Builder(disableBuilder = true))
    SysUser toEntity(SysUserDetailForm form);
    @BeanMapping(builder = @Builder(disableBuilder = true))
    SysUser toEntity(UserProfileForm form);
    @BeanMapping(builder = @Builder(disableBuilder = true))
    SysUser toEntity(ResetPasswordForm form);
    SysUserVo toVo(SysUser entity);
}
