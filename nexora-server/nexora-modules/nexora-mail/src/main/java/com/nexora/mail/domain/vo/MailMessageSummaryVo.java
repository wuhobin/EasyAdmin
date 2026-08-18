package com.nexora.mail.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "邮件摘要视图对象")
public class MailMessageSummaryVo {
    @Schema(description = "账号ID")
    private Long accountId;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "邮件服务商")
    private String provider;

    @Schema(description = "邮件唯一标识")
    private Long uid;

    @Schema(description = "邮件唯一标识有效期")
    private Long uidValidity;

    @Schema(description = "发件人名称")
    private String fromName;

    @Schema(description = "发件人地址")
    private String fromAddress;

    @Schema(description = "邮件主题")
    private String subject;

    @Schema(description = "接收时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receivedTime;

    @Schema(description = "是否已读")
    private boolean read;

    @Schema(description = "是否包含附件")
    private boolean hasAttachment;
}
