package com.nexora.domain.form.query.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户查询表单")
public class SysUserQueryForm {
    @Schema(description = "用户昵称关键字")
    private String nickname;

    @Schema(description = "用户状态")
    private Integer status;
}
