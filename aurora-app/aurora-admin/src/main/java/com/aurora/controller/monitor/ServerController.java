package com.aurora.controller.monitor;

import com.aurora.biz.ServerBizService;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.domain.vo.server.ServerInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor/server")
@Tag(name = "服务器监控", description = "服务器监控相关接口")
@RequiredArgsConstructor
public class ServerController {

    private final ServerBizService serverBizService;

    @GetMapping
    @Operation(summary = "获取服务器信息")
    public Result<ServerInfoVo> getServerInfo() {
        return Result.data(serverBizService.getServerInfo());
    }
}
