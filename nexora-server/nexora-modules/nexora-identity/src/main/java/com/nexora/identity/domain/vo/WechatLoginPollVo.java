package com.nexora.identity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "微信登录轮询结果")
public record WechatLoginPollVo(
        @Schema(description = "状态：PENDING、SUCCESS、PENDING_AUDIT、FAILED、EXPIRED") String status,
        @Schema(description = "提示消息") String message,
        @Schema(description = "登录用户信息") LoginUserInfoVo user) {
}
