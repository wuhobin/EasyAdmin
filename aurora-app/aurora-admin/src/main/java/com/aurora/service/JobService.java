package com.aurora.service;

import com.aurora.domain.query.monitor.QuartzJobQuery;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.quartz.domain.QuartzJob;
import com.aurora.starter.quartz.exception.TaskException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.quartz.SchedulerException;
import java.util.List;

public interface JobService {
    IPage<QuartzJob> list(QuartzJobQuery query, PageParam pageParam);
    QuartzJob get(Long jobId);
    QuartzJob add(QuartzJob job) throws SchedulerException, TaskException;
    void update(QuartzJob job) throws SchedulerException, TaskException;
    void delete(List<Long> ids) throws SchedulerException;
    boolean changeStatus(Long jobId, String status) throws SchedulerException;
    boolean run(Long jobId) throws SchedulerException;
}
