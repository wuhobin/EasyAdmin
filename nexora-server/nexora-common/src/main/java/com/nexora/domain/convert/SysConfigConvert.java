package com.nexora.domain.convert;

import com.nexora.domain.form.query.system.SysConfigQueryForm;
import com.nexora.domain.form.system.SysConfigForm;
import com.nexora.domain.query.system.SysConfigQuery;
import com.nexora.domain.vo.system.SysConfigVo;
import com.nexora.entity.SysConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysConfigConvert {

    SysConfigConvert INSTANCE = Mappers.getMapper(SysConfigConvert.class);

    SysConfigQuery toQuery(SysConfigQueryForm form);

    SysConfig toEntity(SysConfigForm form);

    SysConfigVo toVo(SysConfig entity);
}
