package com.nexora.service;

import com.aurora.starter.quartz.domain.QuartzJobLog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.domain.query.monitor.QuartzJobLogQuery;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface IJobLogService extends IService<QuartzJobLog> {
    IPage<QuartzJobLog> list(QuartzJobLogQuery query, PageParam pageParam);
    void cleanJobLog();
}
