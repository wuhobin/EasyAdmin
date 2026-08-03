package com.nexora.monitor.domain.convert;

import com.nexora.monitor.domain.form.SysOperateLogQueryForm;
import com.nexora.monitor.domain.query.SysOperateLogQuery;
import com.nexora.monitor.domain.vo.SysOperateLogVo;
import com.nexora.monitor.entity.SysOperateLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysOperateLogConvert {
    SysOperateLogConvert INSTANCE = Mappers.getMapper(SysOperateLogConvert.class);
    SysOperateLogQuery toQuery(SysOperateLogQueryForm form);
    SysOperateLogVo toVo(SysOperateLog entity);
}
