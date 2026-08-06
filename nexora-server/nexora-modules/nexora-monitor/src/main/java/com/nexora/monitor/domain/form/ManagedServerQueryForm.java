package com.nexora.monitor.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "服务器查询表单")
public class ManagedServerQueryForm {

    private String name;

    private Integer enabled;
}
