package com.nexora.monitor.domain.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TerminalTicketForm {

    @Size(max = 512, message = "SSH 密码不能超过 512 个字符")
    private String password;

    @Min(value = 20, message = "终端列数不能小于 20")
    @Max(value = 500, message = "终端列数不能超过 500")
    private Integer columns;

    @Min(value = 5, message = "终端行数不能小于 5")
    @Max(value = 200, message = "终端行数不能超过 200")
    private Integer rows;
}
