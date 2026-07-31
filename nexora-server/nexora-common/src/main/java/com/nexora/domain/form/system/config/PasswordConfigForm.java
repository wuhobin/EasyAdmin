package com.nexora.domain.form.system.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "密码策略配置")
public class PasswordConfigForm {

    @NotNull(message = "密码最小长度不能为空")
    @Min(value = 6, message = "密码最小长度不能小于6")
    @Max(value = 32, message = "密码最小长度不能大于32")
    private Integer minLength;

    @NotNull(message = "密码最大长度不能为空")
    @Min(value = 6, message = "密码最大长度不能小于6")
    @Max(value = 64, message = "密码最大长度不能大于64")
    private Integer maxLength;

    @NotNull(message = "大写字母规则不能为空")
    private Boolean requireUppercase;

    @NotNull(message = "小写字母规则不能为空")
    private Boolean requireLowercase;

    @NotNull(message = "数字规则不能为空")
    private Boolean requireNumber;

    @NotNull(message = "特殊字符规则不能为空")
    private Boolean requireSpecial;

    @JsonIgnore
    @AssertTrue(message = "密码最大长度不能小于最小长度")
    public boolean isLengthRangeValid() {
        return minLength == null || maxLength == null || maxLength >= minLength;
    }
}
