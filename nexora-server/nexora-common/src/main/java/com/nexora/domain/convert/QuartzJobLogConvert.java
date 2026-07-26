package com.nexora.domain.convert;

import com.nexora.domain.form.query.monitor.QuartzJobLogQueryForm;
import com.nexora.domain.query.monitor.QuartzJobLogQuery;
import com.nexora.domain.vo.monitor.QuartzJobLogVo;
import com.aurora.starter.quartz.domain.QuartzJobLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface QuartzJobLogConvert {
    QuartzJobLogConvert INSTANCE = Mappers.getMapper(QuartzJobLogConvert.class);
    QuartzJobLogQuery toQuery(QuartzJobLogQueryForm form);
    QuartzJobLogVo toVo(QuartzJobLog entity);
}
