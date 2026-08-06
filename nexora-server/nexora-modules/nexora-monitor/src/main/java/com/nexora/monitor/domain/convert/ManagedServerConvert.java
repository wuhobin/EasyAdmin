package com.nexora.monitor.domain.convert;

import com.nexora.monitor.domain.form.ManagedServerQueryForm;
import com.nexora.monitor.domain.query.ManagedServerQuery;
import com.nexora.monitor.domain.vo.ManagedServerVo;
import com.nexora.monitor.entity.ManagedServer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ManagedServerConvert {

    ManagedServerConvert INSTANCE = Mappers.getMapper(ManagedServerConvert.class);

    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerIds", ignore = true)
    ManagedServerQuery toQuery(ManagedServerQueryForm form);

    @Mapping(target = "hasSavedPassword", ignore = true)
    ManagedServerVo toVo(ManagedServer entity);
}
