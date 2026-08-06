package com.nexora.monitor.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServerFingerprintForm {

    @NotBlank(message = "主机指纹不能为空")
    @Size(max = 255, message = "主机指纹格式不正确")
    private String fingerprint;
}
