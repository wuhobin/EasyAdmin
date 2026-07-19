package com.aurora.domain.convert;

import com.aurora.domain.form.query.monitor.QuartzJobLogQueryForm;
import com.aurora.domain.query.monitor.QuartzJobLogQuery;
import com.aurora.domain.vo.monitor.QuartzJobLogVo;
import com.aurora.starter.quartz.domain.QuartzJobLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface QuartzJobLogConvert {
    QuartzJobLogConvert INSTANCE = Mappers.getMapper(QuartzJobLogConvert.class);
    QuartzJobLogQuery toQuery(QuartzJobLogQueryForm form);
    QuartzJobLogVo toVo(QuartzJobLog entity);
}
