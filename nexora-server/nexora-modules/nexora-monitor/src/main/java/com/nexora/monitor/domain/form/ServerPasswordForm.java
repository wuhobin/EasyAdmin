package com.nexora.monitor.domain.form;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServerPasswordForm {

    @Size(max = 512, message = "SSH 密码不能超过 512 个字符")
    private String password;
}
