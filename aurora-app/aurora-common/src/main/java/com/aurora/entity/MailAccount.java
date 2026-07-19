package com.aurora.entity;

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
@TableName("mail_account")
public class MailAccount extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String accountName;

    private String provider;

    private String email;

    private String authCodeCiphertext;

    private Integer enabled;

    private Integer sort;

    private Long lastUid;

    private Long uidValidity;

    private LocalDateTime lastConnectTime;

    private String lastError;
}
