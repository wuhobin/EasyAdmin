package com.nexora.identity.domain.convert;

import com.nexora.identity.domain.form.SysRoleQueryForm;
import com.nexora.identity.domain.form.SysRoleForm;
import com.nexora.identity.domain.query.SysRoleQuery;
import com.nexora.identity.domain.vo.SysRoleVo;
import com.nexora.identity.entity.SysRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysRoleConvert {
    SysRoleConvert INSTANCE = Mappers.getMapper(SysRoleConvert.class);
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "excludeId", ignore = true)
    SysRoleQuery toQuery(SysRoleQueryForm form);
    SysRole toEntity(SysRoleForm form);
    SysRoleVo toVo(SysRole entity);
}
