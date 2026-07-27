package com.nexora.controller.auth;

import com.nexora.biz.auth.AuthBizService;
import com.aurora.starter.webmvc.domain.response.Result;
import com.nexora.domain.form.auth.LoginForm;
import com.nexora.domain.vo.auth.LoginUserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "用户登录登出接口")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthBizService authBizService;

    @Operation(summary = "用户登录")
    @PostMapping("/auth/login")
    public Result<LoginUserInfoVo> login(@RequestBody LoginForm form) {
        return Result.data(authBizService.login(form));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/auth/logout")
    public Result<Void> logout() {
        authBizService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/auth/info")
    public Result<LoginUserInfoVo> info() {
        return Result.data(authBizService.getLoginUserInfo());
    }
}
