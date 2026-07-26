package com.nexora.domain.form.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "用户维护表单")
public class SysUserForm {
    @Schema(description = "用户信息")
    private SysUserDetailForm user;

    @Schema(description = "角色ID列表")
    private List<Integer> roleIds;
}
