package com.nexora.monitor.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Schema(description = "服务器视图")
public class ManagedServerVo {

    private Long id;

    private String name;

    private String host;

    private Integer port;

    private String username;

    private String description;

    private Integer enabled;

    private Integer sort;

    private boolean hasSavedPassword;

    private String trustedFingerprint;

    private String fingerprintAlgorithm;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fingerprintVerifiedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastConnectTime;

    private String lastError;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
