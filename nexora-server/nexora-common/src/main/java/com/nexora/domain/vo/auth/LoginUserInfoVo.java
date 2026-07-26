package com.nexora.domain.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "登录用户信息")
public class LoginUserInfoVo implements Serializable {
    @Schema(description = "用户ID")
    private Integer id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "权限标识列表")
    private List<String> permissions;

    @Schema(description = "角色编码列表")
    private List<String> roles;
}
