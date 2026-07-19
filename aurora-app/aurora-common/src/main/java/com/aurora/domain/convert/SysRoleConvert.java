package com.aurora.domain.convert;

import com.aurora.domain.form.query.system.SysRoleQueryForm;
import com.aurora.domain.form.system.SysRoleForm;
import com.aurora.domain.query.system.SysRoleQuery;
import com.aurora.domain.vo.system.SysRoleVo;
import com.aurora.entity.SysRole;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysRoleConvert {
    SysRoleConvert INSTANCE = Mappers.getMapper(SysRoleConvert.class);
    SysRoleQuery toQuery(SysRoleQueryForm form);
    SysRole toEntity(SysRoleForm form);
    SysRoleVo toVo(SysRole entity);
}
