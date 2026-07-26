package com.nexora.domain.convert;

import com.nexora.domain.form.query.system.SysDictQueryForm;
import com.nexora.domain.form.system.SysDictForm;
import com.nexora.domain.query.system.SysDictQuery;
import com.nexora.domain.vo.system.SysDictVo;
import com.nexora.entity.SysDict;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysDictConvert {
    SysDictConvert INSTANCE = Mappers.getMapper(SysDictConvert.class);
    SysDictQuery toQuery(SysDictQueryForm form);
    SysDict toEntity(SysDictForm form);
    SysDictVo toVo(SysDict entity);
}
