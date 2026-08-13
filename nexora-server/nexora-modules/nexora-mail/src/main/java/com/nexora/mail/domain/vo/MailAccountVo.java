package com.nexora.mail.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "邮箱账号视图对象")
public class MailAccountVo {
    @Schema(description = "账号ID")
    private Long id;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "邮件服务商")
    private String provider;

    @Schema(description = "邮箱地址")
    private String email;

    @Schema(description = "是否启用")
    private Integer enabled;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "最近连接时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastConnectTime;

    @Schema(description = "最近连接错误信息")
    private String lastError;
}
