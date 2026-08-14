package com.nexora.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "匿名可见的系统配置")
public record SysConfigPublicVo(
        @Schema(description = "系统基础配置") SystemConfig system,
        @Schema(description = "注册配置") RegisterConfig register,
        @Schema(description = "登录配置") LoginConfig login,
        @Schema(description = "密码策略配置") PasswordConfig password,
        @Schema(description = "微信登录配置") WechatConfig wechat) {

    @Schema(description = "系统基础配置")
    public record SystemConfig(
            @Schema(description = "站点名称") String siteName,
            @Schema(description = "站点简称") String shortTitle,
            @Schema(description = "站点描述") String siteDescription,
            @Schema(description = "站点Logo") String siteLogo,
            @Schema(description = "版权信息") String copyright,
            @Schema(description = "ICP备案号") String icp,
            @Schema(description = "是否启用水印") Boolean watermarkEnabled,
            @Schema(description = "水印类型") String watermarkType,
            @Schema(description = "自定义水印文本") String watermarkCustomText,
            @Schema(description = "水印透明度") Double watermarkOpacity) {
    }

    @Schema(description = "注册配置")
    public record RegisterConfig(
            @Schema(description = "是否启用验证码") Boolean captchaEnabled,
            @Schema(description = "是否验证邮箱") Boolean verifyEmail,
            @Schema(description = "是否需要审核") Boolean needAudit) {
    }

    @Schema(description = "匿名可见的微信登录配置")
    public record WechatConfig(
            @Schema(description = "是否启用微信登录") Boolean enabled,
            @Schema(description = "公众号二维码图片地址") String qrCodeUrl) {
    }

    @Schema(description = "登录配置")
    public record LoginConfig(@Schema(description = "是否启用记住我") Boolean rememberMeEnabled) {
    }

    @Schema(description = "密码策略配置")
    public record PasswordConfig(
            @Schema(description = "最小长度") Integer minLength,
            @Schema(description = "最大长度") Integer maxLength,
            @Schema(description = "是否要求大写字母") Boolean requireUppercase,
            @Schema(description = "是否要求小写字母") Boolean requireLowercase,
            @Schema(description = "是否要求数字") Boolean requireNumber,
            @Schema(description = "是否要求特殊字符") Boolean requireSpecial) {
    }
}
