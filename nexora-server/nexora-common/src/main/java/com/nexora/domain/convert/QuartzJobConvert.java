package com.nexora.domain.convert;

import com.nexora.domain.form.monitor.QuartzJobForm;
import com.nexora.domain.form.query.monitor.QuartzJobQueryForm;
import com.nexora.domain.query.monitor.QuartzJobQuery;
import com.nexora.domain.vo.monitor.QuartzJobVo;
import com.aurora.starter.quartz.domain.QuartzJob;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface QuartzJobConvert {
    QuartzJobConvert INSTANCE = Mappers.getMapper(QuartzJobConvert.class);
    QuartzJobQuery toQuery(QuartzJobQueryForm form);
    QuartzJob toEntity(QuartzJobForm form);
    QuartzJobVo toVo(QuartzJob entity);
}
