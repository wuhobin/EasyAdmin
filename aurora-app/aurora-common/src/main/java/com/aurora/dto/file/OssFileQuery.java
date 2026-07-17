package com.aurora.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "OSS 文件列表查询参数")
public class OssFileQuery {

    @Schema(description = "文件名称关键字")
    private String fileName;

    @Schema(description = "MIME 类型")
    private String contentType;

    @Schema(description = "上传人名称")
    private String uploaderName;
}
