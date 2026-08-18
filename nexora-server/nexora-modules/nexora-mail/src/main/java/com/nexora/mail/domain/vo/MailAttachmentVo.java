package com.nexora.mail.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "邮件附件视图对象")
public class MailAttachmentVo {
    @Schema(description = "附件分区ID")
    private String partId;

    @Schema(description = "附件文件名")
    private String fileName;

    @Schema(description = "附件MIME类型")
    private String contentType;

    @Schema(description = "附件大小，单位为字节")
    private long size;
}
