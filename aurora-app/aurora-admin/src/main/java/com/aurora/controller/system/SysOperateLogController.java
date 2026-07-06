package com.aurora.controller.system;

import java.util.List;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import com.aurora.entity.SysOperateLog;
import com.aurora.service.SysOperateLogService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.common.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/sys/operateLog")
@RequiredArgsConstructor
@Tag(name = "操作日志管理", description = "操作日志管理相关接口")
public class SysOperateLogController {

    private final SysOperateLogService sysOperateLogService;

    @GetMapping
    @Operation(description = "获取操作日志列表")
    public Result<IPage<SysOperateLog>> list(SysOperateLog sysOperateLog, PageParam pageParam) {
        return Result.success(sysOperateLogService.listSysOperateLog(sysOperateLog, pageParam));
    }

    @DeleteMapping("delete/{ids}")
    @Operation(description = "批量删除操作日志")
    @SaCheckPermission("sys:operateLog:delete")
    public Result<Void> delete(@PathVariable List<Long> ids) {
        sysOperateLogService.removeBatchByIds(ids);
        return Result.success();
    }
}
