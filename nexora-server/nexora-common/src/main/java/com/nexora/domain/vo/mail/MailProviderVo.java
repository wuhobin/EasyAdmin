package com.nexora.domain.vo.mail;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailProviderVo {
    private String label;
    private String value;
    private String domain;
    private String imapHost;
    private Integer imapPort;
    private boolean defaultProvider;
}
