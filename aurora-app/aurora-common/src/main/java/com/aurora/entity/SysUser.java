package com.aurora.entity;

import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.aurora.starter.common.utils.DateUtils;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user")
@Schema(description = "用户信息")
public class SysUser extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "ip地址")
    private String ip;

    @Schema(description = "ip来源")
    private String ipLocation;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "登录类型")
    private Integer loginType;

    @Schema(description = "上次登录时间")
    @JsonFormat(pattern = DateUtils.YYYY_MM_DD_HH_MM_SS, timezone = "GMT+8")
    private LocalDateTime lastLoginTime;
}
