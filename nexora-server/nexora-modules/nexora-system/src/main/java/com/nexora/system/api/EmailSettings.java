package com.nexora.system.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "系统邮箱配置")
public class EmailSettings {

    @NotNull(message = "邮件开关不能为空")
    private Boolean enabled;

    @NotNull(message = "SMTP服务器不能为空")
    @Size(max = 255, message = "SMTP服务器不能超过255个字符")
    private String host;

    @NotNull(message = "SMTP端口不能为空")
    @Min(value = 1, message = "SMTP端口不能小于1")
    @Max(value = 65535, message = "SMTP端口不能大于65535")
    private Integer port;

    @NotNull(message = "SMTP用户名不能为空")
    @Size(max = 255, message = "SMTP用户名不能超过255个字符")
    private String username;

    @NotNull(message = "SMTP密码不能为空")
    @Size(max = 255, message = "SMTP密码不能超过255个字符")
    private String password;

    @NotNull(message = "发件人名称不能为空")
    @Size(max = 100, message = "发件人名称不能超过100个字符")
    private String fromName;

    @NotNull(message = "SSL开关不能为空")
    private Boolean ssl;

    public void setHost(String host) {
        this.host = strip(host);
    }

    public void setUsername(String username) {
        this.username = strip(username);
    }

    public void setFromName(String fromName) {
        this.fromName = strip(fromName);
    }

    @JsonIgnore
    @AssertTrue(message = "启用邮件时必须填写SMTP服务器、用户名和密码")
    public boolean isConnectionCompleteWhenEnabled() {
        return !Boolean.TRUE.equals(enabled)
                || hasText(host) && hasText(username) && hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
