package com.nexora.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.annotation.OperationLogger;
import com.nexora.monitor.biz.OnlineSessionBizService;
import com.nexora.monitor.constants.OnlineSessionConstants;
import com.nexora.monitor.domain.form.OnlineSessionQueryForm;
import com.nexora.monitor.domain.vo.ForceLogoutResultVo;
import com.nexora.monitor.domain.vo.OnlineSessionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Online session management")
@RestController
@RequestMapping("/monitor/online")
@RequiredArgsConstructor
@Validated
public class OnlineSessionController {

    private final OnlineSessionBizService onlineSessionBizService;

    @GetMapping("/list")
    @Operation(summary = "分页查询在线会话")
    @SaCheckPermission("sys:online")
    public Result<IPage<OnlineSessionVo>> list(@Valid OnlineSessionQueryForm form) {
        return Result.data(onlineSessionBizService.list(form));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "强退指定在线会话")
    @SaCheckPermission("sys:online:forceLogout")
    @OperationLogger("强退在线会话 {1}")
    public Result<ForceLogoutResultVo> forceLogout(
            @PathVariable
            @Pattern(
                    regexp = OnlineSessionConstants.SESSION_ID_PATTERN,
                    message = OnlineSessionConstants.INVALID_SESSION_ID_MESSAGE)
            String sessionId) {
        return Result.data(onlineSessionBizService.forceLogout(sessionId));
    }
}
