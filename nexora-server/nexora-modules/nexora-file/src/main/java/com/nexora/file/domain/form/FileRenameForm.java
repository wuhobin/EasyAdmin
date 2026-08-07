package com.nexora.file.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件重命名表单")
public class FileRenameForm {

    @Schema(description = "新的展示文件名")
    private String newName;
}
