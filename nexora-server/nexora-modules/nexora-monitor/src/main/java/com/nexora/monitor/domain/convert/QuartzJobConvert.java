package com.nexora.monitor.domain.convert;

import com.nexora.monitor.domain.form.QuartzJobForm;
import com.nexora.monitor.domain.form.QuartzJobQueryForm;
import com.nexora.monitor.domain.query.QuartzJobQuery;
import com.nexora.monitor.domain.vo.QuartzJobVo;
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
