package com.nexora.mail.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "邮件详情视图对象")
public class MailMessageDetailVo {
    @Schema(description = "账号ID")
    private Long accountId;

    @Schema(description = "邮件唯一标识")
    private Long uid;

    @Schema(description = "邮件唯一标识有效期")
    private Long uidValidity;

    @Schema(description = "发件人名称")
    private String fromName;

    @Schema(description = "发件人地址")
    private String fromAddress;

    @Schema(description = "收件人地址列表")
    private List<String> recipients;

    @Schema(description = "邮件主题")
    private String subject;

    @Schema(description = "接收时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receivedTime;

    @Schema(description = "HTML格式正文")
    private String bodyHtml;

    @Schema(description = "纯文本正文")
    private String bodyText;

    @Schema(description = "附件列表")
    private List<MailAttachmentVo> attachments;
}
