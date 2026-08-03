package com.nexora.identity.domain.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "个人信息")
public class SysUserProfileVo {

    @Schema(description = "用户信息")
    private SysUserVo sysUser;

    @Schema(description = "角色")
    private List<String> roles;
}
