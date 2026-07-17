package com.aurora.domain.form.query.monitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "在线用户查询表单")
public class OnlineUserQueryForm {
    @Schema(description = "用户名关键字")
    private String username;
}
