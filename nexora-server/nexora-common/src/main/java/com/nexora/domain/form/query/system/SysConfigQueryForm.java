package com.nexora.domain.form.query.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "系统配置查询表单")
public class SysConfigQueryForm {

    @Schema(description = "配置键关键字")
    private String configKey;

    public void setConfigKey(String configKey) {
        String stripped = configKey == null ? null : configKey.strip();
        this.configKey = stripped == null || stripped.isEmpty() ? null : stripped;
    }
}
