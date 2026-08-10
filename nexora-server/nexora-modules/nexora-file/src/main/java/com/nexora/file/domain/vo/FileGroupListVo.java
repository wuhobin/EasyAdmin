package com.nexora.file.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "文件分组列表")
public class FileGroupListVo {

    @Schema(description = "分组列表")
    private List<SysOssFileGroupVo> groups;

    @Schema(description = "未分组文件数量")
    private Long ungroupedCount;

    @Schema(description = "是否需要先选择上传人范围")
    private boolean scopeRequired;
}
