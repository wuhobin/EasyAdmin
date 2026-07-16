package com.aurora.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.annotation.OperationLogger;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.service.IJobLogService;
import com.aurora.starter.quartz.domain.QuartzJobLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "定时任务日志", description = "定时任务日志相关接口")
@RestController
@RequestMapping("/monitor/jobLog")
@RequiredArgsConstructor
public class JobLogController {

    private final IJobLogService jobLogService;

    @Operation(summary = "获取定时任务日志列表")
    @GetMapping("/list")
    public Result<Page<QuartzJobLog>> list(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String jobGroup,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<QuartzJobLog> wrapper = new LambdaQueryWrapper<QuartzJobLog>()
                .like(StringUtils.isNotBlank(jobName), QuartzJobLog::getJobName, jobName)
                .eq(StringUtils.isNotBlank(jobGroup), QuartzJobLog::getJobGroup, jobGroup)
                .eq(StringUtils.isNotBlank(status), QuartzJobLog::getStatus, status)
                .orderByDesc(QuartzJobLog::getStartTime);

        return Result.data(jobLogService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "删除定时任务日志")
    @OperationLogger(value = "删除定时任务日志")
    @SaCheckPermission("sys:jobLog:delete")
    public Result<Void> delete(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        jobLogService.removeBatchByIds(idList);
        return Result.success();
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清空定时任务日志")
    @OperationLogger(value = "清空定时任务日志")
    @SaCheckPermission("sys:jobLog:clean")
    public Result<Void> clean() {
        jobLogService.cleanJobLog();
        return Result.success();
    }
}
