package com.aurora.domain.convert;

import com.aurora.domain.form.query.system.SysDictQueryForm;
import com.aurora.domain.form.system.SysDictForm;
import com.aurora.domain.query.system.SysDictQuery;
import com.aurora.domain.vo.system.SysDictVo;
import com.aurora.entity.SysDict;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysDictConvert {
    SysDictConvert INSTANCE = Mappers.getMapper(SysDictConvert.class);
    SysDictQuery toQuery(SysDictQueryForm form);
    SysDict toEntity(SysDictForm form);
    SysDictVo toVo(SysDict entity);
}
