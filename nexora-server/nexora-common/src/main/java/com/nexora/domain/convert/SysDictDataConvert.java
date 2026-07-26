package com.nexora.domain.convert;

import com.nexora.domain.form.query.system.SysDictDataQueryForm;
import com.nexora.domain.form.system.SysDictDataForm;
import com.nexora.domain.query.system.SysDictDataQuery;
import com.nexora.domain.vo.system.SysDictDataVo;
import com.nexora.entity.SysDictData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysDictDataConvert {
    SysDictDataConvert INSTANCE = Mappers.getMapper(SysDictDataConvert.class);
    SysDictDataQuery toQuery(SysDictDataQueryForm form);
    SysDictData toEntity(SysDictDataForm form);
    SysDictDataVo toVo(SysDictData entity);
}
