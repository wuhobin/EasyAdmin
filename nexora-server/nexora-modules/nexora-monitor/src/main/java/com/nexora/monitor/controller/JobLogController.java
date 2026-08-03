package com.nexora.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.monitor.annotation.OperationLogger;
import com.nexora.monitor.biz.JobLogBizService;
import com.nexora.monitor.domain.form.QuartzJobLogQueryForm;
import com.nexora.monitor.domain.vo.QuartzJobLogVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Scheduled job logs")
@RestController
@RequestMapping("/monitor/jobLog")
@RequiredArgsConstructor
public class JobLogController {
    private final JobLogBizService jobLogBizService;

    @GetMapping("/list")
    @Operation(summary = "获取定时任务日志列表")
    public Result<IPage<QuartzJobLogVo>> list(QuartzJobLogQueryForm form, PageParam pageParam) {
        return Result.data(jobLogBizService.list(form, pageParam));
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "删除定时任务日志")
    @OperationLogger("删除定时任务日志")
    @SaCheckPermission("sys:jobLog:delete")
    public Result<Void> delete(@PathVariable String ids) {
        jobLogBizService.delete(ids);
        return Result.success();
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清空定时任务日志")
    @OperationLogger("清空定时任务日志")
    @SaCheckPermission("sys:jobLog:clean")
    public Result<Void> clean() {
        jobLogBizService.clean();
        return Result.success();
    }
}
