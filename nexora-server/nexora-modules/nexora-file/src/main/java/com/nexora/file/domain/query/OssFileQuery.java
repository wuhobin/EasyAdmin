package com.nexora.file.domain.query;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "OSS 文件列表查询参数")
public class OssFileQuery {

    @Schema(description = "文件名称关键字")
    @QueryField(operator = Operator.LIKE)
    private String fileName;

    @Schema(description = "MIME 类型")
    @QueryField
    private String contentType;

    @Schema(description = "上传人ID")
    @QueryField
    private Long uploaderId;
}
