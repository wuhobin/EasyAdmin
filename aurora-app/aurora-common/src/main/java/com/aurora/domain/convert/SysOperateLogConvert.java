package com.aurora.domain.convert;

import com.aurora.domain.form.query.system.SysOperateLogQueryForm;
import com.aurora.domain.query.system.SysOperateLogQuery;
import com.aurora.domain.vo.system.SysOperateLogVo;
import com.aurora.entity.SysOperateLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysOperateLogConvert {
    SysOperateLogConvert INSTANCE = Mappers.getMapper(SysOperateLogConvert.class);
    SysOperateLogQuery toQuery(SysOperateLogQueryForm form);
    SysOperateLogVo toVo(SysOperateLog entity);
}
