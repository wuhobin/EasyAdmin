package com.nexora.identity.domain.convert;

import com.nexora.identity.domain.form.SysMenuForm;
import com.nexora.identity.domain.vo.SysMenuVo;
import com.nexora.identity.entity.SysMenu;
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
