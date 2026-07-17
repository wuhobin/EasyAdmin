package com.aurora.controller.auth;

import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.dto.LoginDTO;
import com.aurora.dto.user.LoginUserInfo;
import com.aurora.service.AuthService;
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

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/auth/login")
    public Result<LoginUserInfo> login(@RequestBody LoginDTO loginDTO) {
        return Result.data(authService.login(loginDTO));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/auth/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/auth/info")
    public Result<LoginUserInfo> info() {
        return Result.data(authService.getLoginUserInfo());
    }
}
