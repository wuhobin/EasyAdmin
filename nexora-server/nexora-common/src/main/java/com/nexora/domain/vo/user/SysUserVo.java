package com.nexora.domain.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Data
@Schema(description = "用户视图对象")
public class SysUserVo {
    @Schema(description = "用户ID")
    private Integer id;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "登录IP")
    private String ip;

    @Schema(description = "IP归属地")
    private String ipLocation;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "登录类型")
    private Integer loginType;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @Schema(description = "扩展参数")
    private Map<String, Object> params;
}
