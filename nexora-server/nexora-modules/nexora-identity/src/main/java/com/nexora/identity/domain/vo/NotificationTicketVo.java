package com.nexora.identity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Schema(description = "通知票据视图对象")
public class NotificationTicketVo {
    @Schema(description = "订阅票据")
    private String ticket;

    @Schema(description = "票据过期时间")
    private Instant expiresAt;
}
