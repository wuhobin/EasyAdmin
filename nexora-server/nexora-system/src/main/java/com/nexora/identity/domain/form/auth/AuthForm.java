package com.nexora.identity.domain.form.auth;

import com.aurora.starter.common.utils.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
@Schema(description = "认证操作表单")
public class AuthForm {

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "邮箱验证码")
    private String code;

    @Schema(description = "图片验证成功后的一次性凭证 ID")
    private String captchaId;

    @Schema(description = "是否记住登录状态")
    private boolean rememberMe;

    @Schema(description = "认证来源")
    private String source;

    public void setEmail(String email) {
        this.email = StringUtils.normalizeEmail(email);
    }
}
