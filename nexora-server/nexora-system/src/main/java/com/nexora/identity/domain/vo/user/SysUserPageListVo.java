package com.nexora.identity.domain.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.aurora.starter.common.utils.DateUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "用户分页视图对象")
public class SysUserPageListVo {

    @Schema(description = "用户ID")
    private Integer id;

    @Schema(description = "状态")
    private Integer status;

    @JsonFormat(pattern = DateUtils.YYYY_MM_DD_HH_MM_SS, timezone = "GMT+8")
    @Schema(description = "上次登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "ip地址")
    private String ip;

    @Schema(description = "ip来源")
    private String ipLocation;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "角色id集合")
    private List<Integer> roleIds;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtils.YYYY_MM_DD_HH_MM_SS, timezone = "GMT+8")
    private Date createTime;

}
