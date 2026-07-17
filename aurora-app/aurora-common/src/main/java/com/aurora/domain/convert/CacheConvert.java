package com.aurora.domain.convert;

import com.aurora.domain.form.query.monitor.CacheKeyQueryForm;
import com.aurora.domain.query.monitor.CacheKeyQuery;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CacheConvert {
    CacheConvert INSTANCE = Mappers.getMapper(CacheConvert.class);
    CacheKeyQuery toQuery(CacheKeyQueryForm form);
}
