package com.nexora.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.monitor.biz.OnlineSessionBizService;
import com.nexora.monitor.domain.form.OnlineSessionQueryForm;
import com.nexora.monitor.domain.vo.OnlineSessionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Online session management")
@RestController
@RequestMapping("/monitor/online")
@RequiredArgsConstructor
public class OnlineSessionController {

    private final OnlineSessionBizService onlineSessionBizService;

    @GetMapping("/list")
    @Operation(summary = "分页查询在线会话")
    @SaCheckPermission("sys:online")
    public Result<IPage<OnlineSessionVo>> list(@Valid OnlineSessionQueryForm form) {
        return Result.data(onlineSessionBizService.list(form));
    }
}
