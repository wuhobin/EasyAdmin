package com.aurora.domain.convert;

import com.aurora.domain.form.monitor.QuartzJobForm;
import com.aurora.domain.form.query.monitor.QuartzJobQueryForm;
import com.aurora.domain.query.monitor.QuartzJobQuery;
import com.aurora.domain.vo.monitor.QuartzJobVo;
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
