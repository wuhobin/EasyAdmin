package com.nexora.domain.form.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录表单")
public class LoginForm {
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "是否记住登录状态")
    private boolean rememberMe;
}
