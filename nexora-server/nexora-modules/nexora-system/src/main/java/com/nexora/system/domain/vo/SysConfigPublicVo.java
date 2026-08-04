package com.nexora.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "匿名可见的系统配置")
public record SysConfigPublicVo(
        SystemConfig system,
        RegisterConfig register,
        LoginConfig login,
        PasswordConfig password) {

    public record SystemConfig(
            String siteName,
            String shortTitle,
            String siteDescription,
            String siteLogo,
            String copyright,
            String icp,
            Boolean watermarkEnabled,
            String watermarkType,
            String watermarkCustomText,
            Double watermarkOpacity) {
    }

    public record RegisterConfig(
            Boolean enabled,
            Boolean captchaEnabled,
            Boolean verifyEmail,
            Boolean needAudit) {
    }

    public record LoginConfig(Boolean rememberMeEnabled) {
    }

    public record PasswordConfig(
            Integer minLength,
            Integer maxLength,
            Boolean requireUppercase,
            Boolean requireLowercase,
            Boolean requireNumber,
            Boolean requireSpecial) {
    }
}
