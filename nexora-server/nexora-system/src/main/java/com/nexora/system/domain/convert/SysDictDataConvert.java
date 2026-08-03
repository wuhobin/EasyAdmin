package com.nexora.system.domain.convert;

import com.nexora.system.domain.form.SysDictDataForm;
import com.nexora.system.domain.form.SysDictDataQueryForm;
import com.nexora.system.domain.query.SysDictDataQuery;
import com.nexora.system.domain.vo.SysDictDataVo;
import com.nexora.system.entity.SysDictData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysDictDataConvert {
    SysDictDataConvert INSTANCE = Mappers.getMapper(SysDictDataConvert.class);
    SysDictDataQuery toQuery(SysDictDataQueryForm form);
    SysDictData toEntity(SysDictDataForm form);
    SysDictDataVo toVo(SysDictData entity);
}
