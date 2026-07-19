package com.aurora.domain.convert;

import com.aurora.domain.form.system.SysMenuForm;
import com.aurora.domain.vo.system.SysMenuVo;
import com.aurora.entity.SysMenu;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface SysMenuConvert {
    SysMenuConvert INSTANCE = Mappers.getMapper(SysMenuConvert.class);
    SysMenu toEntity(SysMenuForm form);
    SysMenuVo toVo(SysMenu entity);
    List<SysMenuVo> toVoList(List<SysMenu> entities);
}
