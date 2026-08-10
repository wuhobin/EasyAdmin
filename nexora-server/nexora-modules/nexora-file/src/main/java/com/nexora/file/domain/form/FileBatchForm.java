package com.nexora.file.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "文件批量操作表单")
public class FileBatchForm {

    @Schema(description = "文件记录ID")
    private List<Long> fileIds;

    @Schema(description = "管理员当前筛选的上传人ID")
    private Long uploaderId;
}
