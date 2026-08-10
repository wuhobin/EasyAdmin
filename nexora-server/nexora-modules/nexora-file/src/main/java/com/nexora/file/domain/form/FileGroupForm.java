package com.nexora.file.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件分组表单")
public class FileGroupForm {

    @Schema(description = "分组所有者ID，管理员创建时必填")
    private Long ownerId;

    @Schema(description = "分组名称")
    private String name;
}
