package com.nexora.monitor.biz;

import com.nexora.monitor.domain.convert.QuartzJobConvert;
import com.nexora.monitor.domain.form.QuartzJobForm;
import com.nexora.monitor.domain.form.QuartzJobQueryForm;
import com.nexora.monitor.domain.form.QuartzJobRunForm;
import com.nexora.monitor.domain.form.QuartzJobStatusForm;
import com.nexora.monitor.domain.vo.QuartzJobVo;
import com.nexora.monitor.service.JobService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.quartz.domain.QuartzJob;
import com.aurora.starter.quartz.exception.TaskException;
import com.aurora.starter.quartz.service.IQuartzJobService;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobBizService {

    private final IQuartzJobService quartzJobService;
    private final JobService jobService;

    public IPage<QuartzJobVo> list(QuartzJobQueryForm form, PageParam pageParam) {
        PageParam normalizedPage = normalize(pageParam);
        return jobService.list(QuartzJobConvert.INSTANCE.toQuery(form), normalizedPage)
                .convert(QuartzJobConvert.INSTANCE::toVo);
    }

    public QuartzJobVo get(Long id) {
        return QuartzJobConvert.INSTANCE.toVo(jobService.get(id));
    }

    public QuartzJobVo add(QuartzJobForm form) throws SchedulerException, TaskException {
        QuartzJob job = QuartzJobConvert.INSTANCE.toEntity(form);
        quartzJobService.createJob(job);
        return QuartzJobConvert.INSTANCE.toVo(job);
    }

    public void update(QuartzJobForm form) throws SchedulerException, TaskException {
        quartzJobService.updateJob(QuartzJobConvert.INSTANCE.toEntity(form));
    }

    public void delete(String ids) throws SchedulerException {
        for (Long id : parseIds(ids)) {
            QuartzJob job = jobService.get(id);
            if (job != null) {
                quartzJobService.deleteJob(id, job.getJobGroup());
            }
        }
    }

    public void changeStatus(QuartzJobStatusForm form) throws SchedulerException {
        QuartzJob job = getRequiredJob(form.getJobId());
        if ("0".equals(form.getStatus())) {
            quartzJobService.resumeJob(job.getJobId(), job.getJobGroup());
        } else if ("1".equals(form.getStatus())) {
            quartzJobService.pauseJob(job.getJobId(), job.getJobGroup());
        }
    }

    public void run(QuartzJobRunForm form) throws SchedulerException {
        QuartzJob job = getRequiredJob(form.getJobId());
        quartzJobService.triggerNow(job.getJobId(), job.getJobGroup());
    }

    private QuartzJob getRequiredJob(Long jobId) {
        QuartzJob job = jobService.get(jobId);
        if (job == null) {
            throw new BizException("任务不存在");
        }
        return job;
    }

    private static List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }

    private static PageParam normalize(PageParam pageParam) {
        PageParam result = pageParam == null ? new PageParam() : pageParam;
        if (result.getPageNum() == null) {
            result.setPageNum(PageParam.DEFAULT_PAGE);
        }
        if (result.getPageSize() == null) {
            result.setPageSize(PageParam.DEFAULT_SIZE);
        }
        return result;
    }
}
