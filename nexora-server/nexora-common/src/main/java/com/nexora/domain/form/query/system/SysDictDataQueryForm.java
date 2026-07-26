package com.nexora.domain.form.query.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "字典数据查询表单")
public class SysDictDataQueryForm {
    @Schema(description = "字典ID")
    private Long dictId;
}
