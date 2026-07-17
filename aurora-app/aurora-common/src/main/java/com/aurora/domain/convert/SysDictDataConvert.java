package com.aurora.domain.convert;

import com.aurora.domain.form.query.system.SysDictDataQueryForm;
import com.aurora.domain.form.system.SysDictDataForm;
import com.aurora.domain.query.system.SysDictDataQuery;
import com.aurora.domain.vo.system.SysDictDataVo;
import com.aurora.entity.SysDictData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysDictDataConvert {
    SysDictDataConvert INSTANCE = Mappers.getMapper(SysDictDataConvert.class);
    SysDictDataQuery toQuery(SysDictDataQueryForm form);
    SysDictData toEntity(SysDictDataForm form);
    SysDictDataVo toVo(SysDictData entity);
}
