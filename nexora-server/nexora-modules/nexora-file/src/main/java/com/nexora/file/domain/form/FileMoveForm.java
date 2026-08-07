package com.nexora.file.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "文件移动表单")
public class FileMoveForm {

    @Schema(description = "文件记录ID")
    private List<Long> fileIds;

    @Schema(description = "目标分组ID，空值表示未分组")
    private Long groupId;

    @Schema(description = "管理员当前筛选的上传人ID")
    private Long uploaderId;
}
