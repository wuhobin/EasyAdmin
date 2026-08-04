package com.nexora.mail.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MailMessageDetailVo {
    private Long accountId;
    private Long uid;
    private Long uidValidity;
    private String fromName;
    private String fromAddress;
    private List<String> recipients;
    private String subject;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receivedTime;
    private String bodyHtml;
    private String bodyText;
    private List<MailAttachmentVo> attachments;
}
