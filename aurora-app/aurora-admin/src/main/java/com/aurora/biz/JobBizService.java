package com.aurora.biz;

import com.aurora.domain.convert.QuartzJobConvert;
import com.aurora.domain.form.monitor.QuartzJobForm;
import com.aurora.domain.form.monitor.QuartzJobRunForm;
import com.aurora.domain.form.monitor.QuartzJobStatusForm;
import com.aurora.domain.form.query.monitor.QuartzJobQueryForm;
import com.aurora.domain.vo.monitor.QuartzJobVo;
import com.aurora.service.JobService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.quartz.exception.TaskException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class JobBizService {
    private final JobService jobService;
    public IPage<QuartzJobVo> list(QuartzJobQueryForm form, PageParam pageParam) {
        return jobService.list(QuartzJobConvert.INSTANCE.toQuery(form), pageParam).convert(QuartzJobConvert.INSTANCE::toVo);
    }
    public QuartzJobVo get(Long id) { return QuartzJobConvert.INSTANCE.toVo(jobService.get(id)); }
    public QuartzJobVo add(QuartzJobForm form) throws SchedulerException, TaskException {
        return QuartzJobConvert.INSTANCE.toVo(jobService.add(QuartzJobConvert.INSTANCE.toEntity(form)));
    }
    public void update(QuartzJobForm form) throws SchedulerException, TaskException {
        jobService.update(QuartzJobConvert.INSTANCE.toEntity(form));
    }
    public void delete(String ids) throws SchedulerException {
        jobService.delete(Arrays.stream(ids.split(",")).map(String::trim).map(Long::parseLong).toList());
    }
    public boolean changeStatus(QuartzJobStatusForm form) throws SchedulerException {
        return jobService.changeStatus(form.getJobId(), form.getStatus());
    }
    public boolean run(QuartzJobRunForm form) throws SchedulerException { return jobService.run(form.getJobId()); }
}
