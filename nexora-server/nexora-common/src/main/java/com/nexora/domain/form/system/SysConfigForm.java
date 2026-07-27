package com.nexora.domain.form.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "系统配置表单")
public class SysConfigForm {

    @Schema(description = "配置ID，修改时必填")
    private Long id;

    @NotBlank(message = "配置键不能为空")
    @Size(min = 2, max = 128, message = "配置键长度必须在2到128个字符之间")
    @Pattern(regexp = "^[a-z]+(?:[._-][a-z]+)*$",
            message = "配置键只能包含小写字母及点、短横线、下划线，且不能使用数字")
    @Schema(description = "全局唯一配置键")
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    @Size(max = 512, message = "配置值不能超过512个字符")
    @Schema(description = "配置值")
    private String configValue;

    @Size(max = 255, message = "备注不能超过255个字符")
    @Schema(description = "备注")
    private String remark;

    public void setConfigKey(String configKey) {
        this.configKey = strip(configKey);
    }

    public void setConfigValue(String configValue) {
        this.configValue = strip(configValue);
    }

    public void setRemark(String remark) {
        String stripped = strip(remark);
        this.remark = stripped == null || stripped.isEmpty() ? null : stripped;
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
