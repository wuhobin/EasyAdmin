package com.nexora.domain.convert;

import com.nexora.domain.form.query.system.SysRoleQueryForm;
import com.nexora.domain.form.system.SysRoleForm;
import com.nexora.domain.query.system.SysRoleQuery;
import com.nexora.domain.vo.system.SysRoleVo;
import com.nexora.entity.SysRole;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysRoleConvert {
    SysRoleConvert INSTANCE = Mappers.getMapper(SysRoleConvert.class);
    SysRoleQuery toQuery(SysRoleQueryForm form);
    SysRole toEntity(SysRoleForm form);
    SysRoleVo toVo(SysRole entity);
}
