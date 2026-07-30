package com.nexora.domain.form.query.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "操作日志查询表单")
public class SysOperateLogQueryForm {
    @Schema(description = "操作用户ID")
    private Integer userId;
}
