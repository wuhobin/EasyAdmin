package com.nexora.identity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "微信登录事务")
public record WechatLoginTransactionVo(
        @Schema(description = "登录事务ID") String transactionId,
        @Schema(description = "浏览器轮询凭证") String pollToken,
        @Schema(description = "需要发送给公众号的六码") String code,
        @Schema(description = "有效秒数") long expiresInSeconds) {
}
