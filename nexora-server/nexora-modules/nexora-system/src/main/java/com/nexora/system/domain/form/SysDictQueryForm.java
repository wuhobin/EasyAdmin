package com.nexora.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "字典查询表单")
public class SysDictQueryForm {
    @Schema(description = "字典名称关键字")
    private String name;

    @Schema(description = "字典状态")
    private Integer status;
}
