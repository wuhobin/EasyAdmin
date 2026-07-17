package com.aurora.biz;

import com.aurora.domain.convert.QuartzJobLogConvert;
import com.aurora.domain.form.query.monitor.QuartzJobLogQueryForm;
import com.aurora.domain.vo.monitor.QuartzJobLogVo;
import com.aurora.service.IJobLogService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class JobLogBizService {
    private final IJobLogService jobLogService;
    public IPage<QuartzJobLogVo> list(QuartzJobLogQueryForm form, PageParam pageParam) {
        return jobLogService.list(QuartzJobLogConvert.INSTANCE.toQuery(form), pageParam)
                .convert(QuartzJobLogConvert.INSTANCE::toVo);
    }
    public void delete(String ids) {
        jobLogService.removeBatchByIds(Arrays.stream(ids.split(",")).map(String::trim).map(Long::parseLong).toList());
    }
    public void clean() { jobLogService.cleanJobLog(); }
}
