package com.nexora.domain.vo.mail;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastConnectTime;
    private String lastError;
}
