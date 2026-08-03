package com.nexora.identity.domain.convert;

import com.nexora.identity.domain.form.query.user.SysUserQueryForm;
import com.nexora.identity.domain.query.SysUserQuery;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.domain.vo.user.SysUserVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysUserConvert {
    SysUserConvert INSTANCE = Mappers.getMapper(SysUserConvert.class);
    SysUserQuery toQuery(SysUserQueryForm form);
    SysUserVo toVo(SysUser entity);
}
