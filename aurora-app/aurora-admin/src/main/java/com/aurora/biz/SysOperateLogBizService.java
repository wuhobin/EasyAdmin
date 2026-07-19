package com.aurora.biz;

import com.aurora.domain.convert.SysOperateLogConvert;
import com.aurora.domain.form.query.system.SysOperateLogQueryForm;
import com.aurora.domain.vo.system.SysOperateLogVo;
import com.aurora.entity.SysOperateLog;
import com.aurora.service.SysOperateLogService;
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
