package com.nexora.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "字典数据表单")
public class SysDictDataForm {
    @Schema(description = "字典数据ID")
    private Long id;

    @Schema(description = "字典ID")
    private Long dictId;

    @Schema(description = "字典标签")
    private String label;

    @Schema(description = "字典值")
    private String value;

    @Schema(description = "回显样式")
    private String style;

    @Schema(description = "是否默认项")
    private String isDefault;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态")
    private Integer status;
}
