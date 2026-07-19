package com.aurora.domain.vo.mail;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MailAccountVo {
    private Long id;
    private String accountName;
    private String provider;
    private String email;
    private Integer enabled;
    private Integer sort;
    private LocalDateTime lastConnectTime;
    private String lastError;
}
