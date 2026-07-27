package com.nexora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.biz.system.SysOperateLogBizService;
import com.nexora.domain.form.query.system.SysOperateLogQueryForm;
import com.nexora.domain.vo.system.SysOperateLogVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/sys/operateLog")
@RequiredArgsConstructor
@Tag(name = "Operation log management")
public class SysOperateLogController {
    private final SysOperateLogBizService sysOperateLogBizService;

    @GetMapping
    @Operation(summary = "List operation logs")
    public Result<IPage<SysOperateLogVo>> list(SysOperateLogQueryForm form, PageParam pageParam) {
        return Result.data(sysOperateLogBizService.list(form, pageParam));
    }

    @DeleteMapping("delete/{ids}")
    @Operation(summary = "Delete operation logs")
    @SaCheckPermission("sys:operateLog:delete")
    public Result<Void> delete(@PathVariable List<Long> ids) {
        sysOperateLogBizService.delete(ids);
        return Result.success();
    }
}
