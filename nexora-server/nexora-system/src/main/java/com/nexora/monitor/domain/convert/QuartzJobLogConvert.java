package com.nexora.monitor.domain.convert;

import com.nexora.monitor.domain.form.QuartzJobLogQueryForm;
import com.nexora.monitor.domain.query.QuartzJobLogQuery;
import com.nexora.monitor.domain.vo.QuartzJobLogVo;
import com.aurora.starter.quartz.domain.QuartzJobLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface QuartzJobLogConvert {
    QuartzJobLogConvert INSTANCE = Mappers.getMapper(QuartzJobLogConvert.class);
    QuartzJobLogQuery toQuery(QuartzJobLogQueryForm form);
    QuartzJobLogVo toVo(QuartzJobLog entity);
}
