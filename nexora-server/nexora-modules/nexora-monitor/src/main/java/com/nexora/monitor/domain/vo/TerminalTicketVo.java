package com.nexora.monitor.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(description = "终端票据视图对象")
public class TerminalTicketVo {

    @Schema(description = "终端票据")
    private String ticket;

    @Schema(description = "票据过期时间")
    private Instant expiresAt;
}
