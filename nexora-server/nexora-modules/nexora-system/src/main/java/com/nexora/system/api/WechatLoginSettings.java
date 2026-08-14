package com.nexora.system.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "微信公众号登录配置")
public class WechatLoginSettings {

    public static final String GROUP_CODE = "wechat";

    /** 是否启用微信公众号登录。 */
    @NotNull(message = "微信登录开关不能为空")
    @Schema(description = "是否启用微信公众号登录")
    private Boolean enabled;

    /** 固定公众号二维码图片地址。 */
    @Size(max = 1000, message = "公众号二维码地址不能超过1000个字符")
    @Schema(description = "固定公众号二维码图片地址")
    private String qrCodeUrl;

    /** 微信公众号 AppID。 */
    @Size(max = 64, message = "微信公众号 AppID 不能超过64个字符")
    @Schema(description = "微信公众号 AppID")
    private String appId;

    /** 微信公众号 AppSecret，保存时留空表示不修改。 */
    @Size(max = 512, message = "微信公众号 AppSecret 不能超过512个字符")
    @Schema(description = "微信公众号 AppSecret")
    private String appSecret;

    /** 微信公众号服务器校验 Token，保存时留空表示不修改。 */
    @Size(max = 512, message = "微信公众号 Token 不能超过512个字符")
    @Schema(description = "微信公众号服务器校验 Token")
    private String token;

    /** 微信公众号消息加解密 EncodingAESKey，保存时留空表示不修改。 */
    @Size(max = 512, message = "微信公众号 EncodingAESKey 不能超过512个字符")
    @Schema(description = "微信公众号消息加解密 EncodingAESKey")
    private String aesKey;
}
