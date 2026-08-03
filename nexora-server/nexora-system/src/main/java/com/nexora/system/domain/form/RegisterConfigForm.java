package com.nexora.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册配置")
public class RegisterConfigForm {

    @NotNull(message = "注册开关不能为空")
    private Boolean enabled;

    @NotNull(message = "邮箱验证开关不能为空")
    private Boolean verifyEmail;

    @NotBlank(message = "默认注册角色不能为空")
    @Size(max = 50, message = "默认注册角色编码不能超过50个字符")
    private String defaultRoleCode;

    @NotNull(message = "注册审核开关不能为空")
    private Boolean needAudit;

    public void setDefaultRoleCode(String defaultRoleCode) {
        this.defaultRoleCode = defaultRoleCode == null ? null : defaultRoleCode.strip();
    }
}
