package com.nexora.domain.form.auth;

import com.aurora.starter.common.utils.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录表单")
public class LoginForm {
    @NotBlank
    @Email
    @Schema(description = "邮箱")
    private String email;

    @NotBlank
    @Schema(description = "密码")
    private String password;

    @Schema(description = "是否记住登录状态")
    private boolean rememberMe;

    public void setEmail(String email) {
        this.email = StringUtils.normalizeEmail(email);
    }
}
