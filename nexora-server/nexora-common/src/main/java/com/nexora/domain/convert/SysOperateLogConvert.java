package com.nexora.domain.convert;

import com.nexora.domain.form.query.system.SysOperateLogQueryForm;
import com.nexora.domain.query.system.SysOperateLogQuery;
import com.nexora.domain.vo.system.SysOperateLogVo;
import com.nexora.entity.SysOperateLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysOperateLogConvert {
    SysOperateLogConvert INSTANCE = Mappers.getMapper(SysOperateLogConvert.class);
    SysOperateLogQuery toQuery(SysOperateLogQueryForm form);
    SysOperateLogVo toVo(SysOperateLog entity);
}
