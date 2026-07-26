package com.nexora.domain.form.query.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "OSS文件查询表单")
public class OssFileQueryForm {
    @Schema(description = "文件名称关键字")
    private String fileName;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "上传人ID")
    private Long uploaderId;
}
