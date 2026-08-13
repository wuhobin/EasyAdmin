package com.nexora.mail.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "邮件服务商视图对象")
public class MailProviderVo {
    @Schema(description = "服务商显示名称")
    private String label;

    @Schema(description = "服务商标识")
    private String value;

    @Schema(description = "服务商域名")
    private String domain;

    @Schema(description = "IMAP服务器地址")
    private String imapHost;

    @Schema(description = "IMAP服务器端口")
    private Integer imapPort;

    @Schema(description = "是否为默认服务商")
    private boolean defaultProvider;
}
