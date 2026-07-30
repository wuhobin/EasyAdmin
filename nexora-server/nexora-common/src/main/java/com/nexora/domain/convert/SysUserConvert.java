package com.nexora.domain.convert;

import com.nexora.domain.form.query.system.SysUserQueryForm;
import com.nexora.domain.query.system.SysUserQuery;
import com.nexora.entity.SysUser;
import com.nexora.domain.vo.user.SysUserVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysUserConvert {
    SysUserConvert INSTANCE = Mappers.getMapper(SysUserConvert.class);
    SysUserQuery toQuery(SysUserQueryForm form);
    SysUserVo toVo(SysUser entity);
}
