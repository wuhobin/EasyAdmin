package com.nexora.message.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class NotificationTicketVo {
    private String ticket;
    private Instant expiresAt;
}
