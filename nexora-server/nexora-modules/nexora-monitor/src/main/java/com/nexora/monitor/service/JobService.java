package com.nexora.monitor.service;

import com.nexora.monitor.domain.query.QuartzJobQuery;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.quartz.domain.QuartzJob;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface JobService {
    IPage<QuartzJob> list(QuartzJobQuery query, PageParam pageParam);
    QuartzJob get(Long jobId);
}
