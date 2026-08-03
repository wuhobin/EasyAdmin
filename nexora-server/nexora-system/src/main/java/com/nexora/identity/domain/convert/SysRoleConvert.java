package com.nexora.identity.domain.convert;

import com.nexora.identity.domain.form.query.role.SysRoleQueryForm;
import com.nexora.identity.domain.form.role.SysRoleForm;
import com.nexora.identity.domain.query.SysRoleQuery;
import com.nexora.identity.domain.vo.role.SysRoleVo;
import com.nexora.identity.entity.SysRole;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysRoleConvert {
    SysRoleConvert INSTANCE = Mappers.getMapper(SysRoleConvert.class);
    SysRoleQuery toQuery(SysRoleQueryForm form);
    SysRole toEntity(SysRoleForm form);
    SysRoleVo toVo(SysRole entity);
}
