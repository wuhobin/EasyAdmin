package com.nexora.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.annotation.OperationLogger;
import com.nexora.monitor.biz.JobBizService;
import com.nexora.monitor.domain.form.QuartzJobForm;
import com.nexora.monitor.domain.form.QuartzJobQueryForm;
import com.nexora.monitor.domain.form.QuartzJobRunForm;
import com.nexora.monitor.domain.form.QuartzJobStatusForm;
import com.nexora.monitor.domain.vo.QuartzJobVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.quartz.exception.TaskException;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Scheduled jobs")
@RestController
@RequestMapping("/monitor/job")
@RequiredArgsConstructor
public class JobController {
    private final JobBizService jobBizService;

    @GetMapping("/list")
    @Operation(summary = "获取定时任务列表")
    public Result<IPage<QuartzJobVo>> list(QuartzJobQueryForm form, PageParam pageParam) {
        return Result.data(jobBizService.list(form, pageParam));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "获取定时任务详情")
    public Result<QuartzJobVo> getInfo(@PathVariable Long jobId) { return Result.data(jobBizService.get(jobId)); }

    @PostMapping
    @Operation(summary = "新增定时任务")
    @OperationLogger("新增定时任务")
    @SaCheckPermission("sys:job:add")
    public Result<QuartzJobVo> add(@RequestBody QuartzJobForm form) throws SchedulerException, TaskException {
        return Result.data(jobBizService.add(form));
    }

    @PutMapping
    @Operation(summary = "修改定时任务")
    @OperationLogger("修改定时任务")
    @SaCheckPermission("sys:job:update")
    public Result<Void> edit(@RequestBody QuartzJobForm form) throws SchedulerException, TaskException {
        jobBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "批量删除定时任务")
    @OperationLogger("批量删除定时任务")
    @SaCheckPermission("sys:job:delete")
    public Result<Void> delete(@PathVariable String ids) throws SchedulerException {
        jobBizService.delete(ids);
        return Result.success();
    }

    @PutMapping("/changeStatus")
    @Operation(summary = "修改任务状态")
    @OperationLogger("修改任务状态")
    @SaCheckPermission("sys:job:changeStatus")
    public Result<Void> changeStatus(@RequestBody QuartzJobStatusForm form) throws SchedulerException {
        jobBizService.changeStatus(form);
        return Result.success();
    }

    @PutMapping("/run")
    @Operation(summary = "定时任务立即执行一次")
    public Result<Void> run(@RequestBody QuartzJobRunForm form) throws SchedulerException {
        jobBizService.run(form);
        return Result.success();
    }
}
