package com.nexora.identity.domain.form.query.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "角色查询表单")
public class SysRoleQueryForm {
    @Schema(description = "角色名称关键字")
    private String name;
}
