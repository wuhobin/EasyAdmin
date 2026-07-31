package com.nexora.controller.auth;

import com.nexora.biz.auth.AuthBizService;
import com.aurora.starter.webmvc.domain.response.Result;
import com.nexora.domain.form.auth.AuthForm;
import com.nexora.domain.vo.auth.LoginUserInfoVo;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public Result<LoginUserInfoVo> login(@Valid @RequestBody AuthForm form) {
        return Result.data(authBizService.login(form));
    }

    @Operation(summary = "发送注册邮箱验证码")
    @PostMapping("/auth/register/sendCode")
    public Result<Void> sendRegisterCode(@Valid @RequestBody AuthForm form) {
        authBizService.sendRegisterCode(form);
        return Result.success();
    }

    @Operation(summary = "生成注册图片验证码")
    @PostMapping("/auth/image")
    public Result<ImageCaptchaVO> generateImageCaptcha() {
        return Result.data(authBizService.generateImageCaptcha());
    }

    @Operation(summary = "匹配注册图片验证码轨迹")
    @PostMapping("/auth/image/{captchaId}/match")
    public Result<Boolean> matchImageCaptcha(
            @PathVariable String captchaId,
            @RequestBody ImageCaptchaTrack track) {
        return Result.data(authBizService.matchImageCaptcha(captchaId, track));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/auth/register")
    public Result<Void> register(@Valid @RequestBody AuthForm form) {
        authBizService.register(form);
        return Result.success();
    }

    @Operation(summary = "发送密码重置邮箱验证码")
    @PostMapping("/auth/password/reset/sendCode")
    public Result<Void> sendResetPasswordCode(@Valid @RequestBody AuthForm form) {
        authBizService.sendResetPasswordCode(form);
        return Result.success();
    }

    @Operation(summary = "通过邮箱验证码重置密码")
    @PostMapping("/auth/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody AuthForm form) {
        authBizService.resetPassword(form);
        return Result.success();
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
