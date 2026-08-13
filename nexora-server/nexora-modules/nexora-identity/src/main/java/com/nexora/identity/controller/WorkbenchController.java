package com.nexora.identity.controller;

import com.aurora.starter.webmvc.domain.response.Result;
import com.nexora.identity.biz.WorkbenchBizService;
import com.nexora.identity.domain.vo.WorkbenchSummaryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workbench")
@RequiredArgsConstructor
@Tag(name = "Workbench")
public class WorkbenchController {

    private final WorkbenchBizService workbenchBizService;

    @GetMapping("/summary")
    @Operation(summary = "获取当前用户可见的工作台统计")
    public Result<WorkbenchSummaryVo> summary() {
        return Result.data(workbenchBizService.summary());
    }
}
