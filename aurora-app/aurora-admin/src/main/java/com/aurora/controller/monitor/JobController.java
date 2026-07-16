package com.aurora.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.annotation.OperationLogger;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.starter.quartz.domain.QuartzJob;
import com.aurora.starter.quartz.exception.TaskException;
import com.aurora.starter.quartz.service.IQuartzJobService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "定时任务", description = "定时任务管理接口")
@RestController
@RequestMapping("/monitor/job")
@RequiredArgsConstructor
public class JobController {

    private final IQuartzJobService quartzJobService;

    @Operation(summary = "获取定时任务列表")
    @GetMapping("/list")
    public Result<IPage<QuartzJob>> list(
            String jobName, String jobGroup, String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<QuartzJob> wrapper = new LambdaQueryWrapper<QuartzJob>()
                .like(StringUtils.isNotBlank(jobName), QuartzJob::getJobName, jobName)
                .eq(StringUtils.isNotBlank(jobGroup), QuartzJob::getJobGroup, jobGroup)
                .eq(StringUtils.isNotBlank(status), QuartzJob::getStatus, status);
        return Result.data(quartzJobService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "获取定时任务详情")
    @GetMapping("/{jobId}")
    public Result<QuartzJob> getInfo(@PathVariable Long jobId) {
        return Result.data(quartzJobService.getById(jobId));
    }

    @PostMapping
    @Operation(summary = "新增定时任务")
    @OperationLogger(value = "新增定时任务")
    @SaCheckPermission("sys:job:add")
    public Result<QuartzJob> add(@RequestBody QuartzJob job) throws SchedulerException, TaskException {
        quartzJobService.createJob(job);
        return Result.data(job);
    }

    @PutMapping
    @Operation(summary = "修改定时任务")
    @OperationLogger(value = "修改定时任务")
    @SaCheckPermission("sys:job:update")
    public Result<Void> edit(@RequestBody QuartzJob job) throws SchedulerException, TaskException {
        quartzJobService.updateJob(job);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "批量删除定时任务")
    @OperationLogger(value = "批量删除定时任务")
    @SaCheckPermission("sys:job:delete")
    public Result<Void> delete(@PathVariable String ids) throws SchedulerException {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        for (Long jobId : idList) {
            QuartzJob job = quartzJobService.getById(jobId);
            if (job != null) {
                quartzJobService.deleteJob(jobId, job.getJobGroup());
            }
        }
        return Result.success();
    }

    @PutMapping("/changeStatus")
    @Operation(summary = "修改任务状态")
    @OperationLogger(value = "修改任务状态")
    @SaCheckPermission("sys:job:changeStatus")
    public Result<Void> changeStatus(@RequestBody QuartzJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String status = job.getStatus();
        QuartzJob existing = quartzJobService.getById(jobId);
        if (existing == null) {
            return Result.error("任务不存在");
        }
        if ("0".equals(status)) {
            quartzJobService.resumeJob(jobId, existing.getJobGroup());
        } else if ("1".equals(status)) {
            quartzJobService.pauseJob(jobId, existing.getJobGroup());
        }
        return Result.success();
    }

    @Operation(summary = "定时任务立即执行一次")
    @PutMapping("/run")
    public Result<Void> run(@RequestBody QuartzJob job) throws SchedulerException {
        QuartzJob existing = quartzJobService.getById(job.getJobId());
        if (existing == null) {
            return Result.error("任务不存在");
        }
        quartzJobService.triggerNow(existing.getJobId(), existing.getJobGroup());
        return Result.success();
    }
}
