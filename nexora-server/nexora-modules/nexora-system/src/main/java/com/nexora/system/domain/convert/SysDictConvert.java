package com.nexora.system.domain.convert;

import com.nexora.system.domain.form.SysDictForm;
import com.nexora.system.domain.form.SysDictQueryForm;
import com.nexora.system.domain.query.SysDictQuery;
import com.nexora.system.domain.vo.SysDictVo;
import com.nexora.system.entity.SysDict;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysDictConvert {
    SysDictConvert INSTANCE = Mappers.getMapper(SysDictConvert.class);
    SysDictQuery toQuery(SysDictQueryForm form);
    SysDict toEntity(SysDictForm form);
    SysDictVo toVo(SysDict entity);
}
