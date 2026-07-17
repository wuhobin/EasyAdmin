package com.aurora.service.impl;

import com.aurora.domain.query.monitor.QuartzJobQuery;
import com.aurora.service.JobService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.quartz.domain.QuartzJob;
import com.aurora.starter.quartz.exception.TaskException;
import com.aurora.starter.quartz.service.IQuartzJobService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final IQuartzJobService quartzJobService;

    @Override
    public IPage<QuartzJob> list(QuartzJobQuery query, PageParam pageParam) {
        return quartzJobService.page(PageUtils.buildPage(normalize(pageParam)), DynamicCondition.toWrapper(query));
    }
    @Override public QuartzJob get(Long jobId) { return quartzJobService.getById(jobId); }
    @Override public QuartzJob add(QuartzJob job) throws SchedulerException, TaskException {
        quartzJobService.createJob(job);
        return job;
    }
    @Override public void update(QuartzJob job) throws SchedulerException, TaskException { quartzJobService.updateJob(job); }
    @Override public void delete(List<Long> ids) throws SchedulerException {
        for (Long id : ids) {
            QuartzJob job = quartzJobService.getById(id);
            if (job != null) quartzJobService.deleteJob(id, job.getJobGroup());
        }
    }
    @Override public boolean changeStatus(Long jobId, String status) throws SchedulerException {
        QuartzJob job = quartzJobService.getById(jobId);
        if (job == null) return false;
        if ("0".equals(status)) quartzJobService.resumeJob(jobId, job.getJobGroup());
        else if ("1".equals(status)) quartzJobService.pauseJob(jobId, job.getJobGroup());
        return true;
    }
    @Override public boolean run(Long jobId) throws SchedulerException {
        QuartzJob job = quartzJobService.getById(jobId);
        if (job == null) return false;
        quartzJobService.triggerNow(job.getJobId(), job.getJobGroup());
        return true;
    }

    private static PageParam normalize(PageParam pageParam) {
        PageParam result = pageParam == null ? new PageParam() : pageParam;
        if (result.getPageNum() == null) result.setPageNum(PageParam.DEFAULT_PAGE);
        if (result.getPageSize() == null) result.setPageSize(PageParam.DEFAULT_SIZE);
        return result;
    }
}
