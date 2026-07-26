package com.nexora.domain.vo.mail;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MailMessageSummaryVo {
    private Long accountId;
    private String accountName;
    private String provider;
    private Long uid;
    private Long uidValidity;
    private String fromName;
    private String fromAddress;
    private String subject;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receivedTime;
    private boolean read;
    private boolean hasAttachment;
}
