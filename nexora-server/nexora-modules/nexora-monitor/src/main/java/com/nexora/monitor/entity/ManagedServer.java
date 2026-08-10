package com.nexora.monitor.entity;

import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("monitor_server")
public class ManagedServer extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer ownerId;

    private String name;

    private String host;

    private Integer port;

    private String username;

    private String passwordCiphertext;

    private String description;

    private Integer enabled;

    private Integer sort;

    private String trustedFingerprint;

    private String fingerprintAlgorithm;

    private LocalDateTime fingerprintVerifiedTime;

    private LocalDateTime lastConnectTime;

    private String lastError;
}
