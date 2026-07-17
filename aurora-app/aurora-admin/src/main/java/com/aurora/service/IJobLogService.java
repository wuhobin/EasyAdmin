package com.aurora.service;

import com.aurora.starter.quartz.domain.QuartzJobLog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IJobLogService extends IService<QuartzJobLog> {
    void cleanJobLog();
}
