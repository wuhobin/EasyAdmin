package com.aurora.domain.vo.mail;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailAttachmentVo {
    private String partId;
    private String fileName;
    private String contentType;
    private long size;
}
