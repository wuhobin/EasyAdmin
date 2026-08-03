package com.nexora.monitor.biz;

import com.nexora.monitor.domain.convert.QuartzJobLogConvert;
import com.nexora.monitor.domain.form.QuartzJobLogQueryForm;
import com.nexora.monitor.domain.vo.QuartzJobLogVo;
import com.nexora.monitor.service.IJobLogService;
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
        PageParam normalizedPage = normalize(pageParam);
        return jobLogService.list(QuartzJobLogConvert.INSTANCE.toQuery(form), normalizedPage)
                .convert(QuartzJobLogConvert.INSTANCE::toVo);
    }
    public void delete(String ids) {
        jobLogService.removeBatchByIds(Arrays.stream(ids.split(",")).map(String::trim).map(Long::parseLong).toList());
    }
    public void clean() { jobLogService.cleanJobLog(); }

    private static PageParam normalize(PageParam pageParam) {
        PageParam result = pageParam == null ? new PageParam() : pageParam;
        if (result.getPageNum() == null) {
            result.setPageNum(PageParam.DEFAULT_PAGE);
        }
        if (result.getPageSize() == null) {
            result.setPageSize(PageParam.DEFAULT_SIZE);
        }
        if (result.getOrderBy() == null || result.getOrderBy().isBlank()) {
            result.setOrderBy("start_time desc");
        }
        return result;
    }
}
