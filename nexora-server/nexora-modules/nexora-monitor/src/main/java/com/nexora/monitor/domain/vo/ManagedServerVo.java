package com.nexora.monitor.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Schema(description = "服务器视图")
public class ManagedServerVo {

    @Schema(description = "服务器ID")
    private Long id;

    @Schema(description = "服务器名称")
    private String name;

    @Schema(description = "主机地址")
    private String host;

    @Schema(description = "连接端口")
    private Integer port;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "服务器描述")
    private String description;

    @Schema(description = "是否启用")
    private Integer enabled;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否已保存密码")
    private boolean hasSavedPassword;

    @Schema(description = "已信任的指纹")
    private String trustedFingerprint;

    @Schema(description = "指纹算法")
    private String fingerprintAlgorithm;

    @Schema(description = "指纹验证时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fingerprintVerifiedTime;

    @Schema(description = "最近连接时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastConnectTime;

    @Schema(description = "最近连接错误信息")
    private String lastError;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
