package com.nexora.monitor.biz;

import com.nexora.monitor.domain.convert.SysOperateLogConvert;
import com.nexora.monitor.domain.form.SysOperateLogQueryForm;
import com.nexora.monitor.domain.vo.SysOperateLogVo;
import com.nexora.monitor.entity.SysOperateLog;
import com.nexora.monitor.service.SysOperateLogService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysOperateLogBizService {
    private final SysOperateLogService sysOperateLogService;
    public IPage<SysOperateLogVo> list(SysOperateLogQueryForm form, PageParam pageParam) {
        IPage<SysOperateLog> page = sysOperateLogService.listSysOperateLog(SysOperateLogConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysOperateLogConvert.INSTANCE::toVo);
    }
    public void delete(List<Long> ids) { sysOperateLogService.removeBatchByIds(ids); }
}
