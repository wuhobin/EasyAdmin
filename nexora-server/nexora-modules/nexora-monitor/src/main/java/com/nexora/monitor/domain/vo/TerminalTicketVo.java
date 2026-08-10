package com.nexora.monitor.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TerminalTicketVo {

    private String ticket;

    private Instant expiresAt;
}
