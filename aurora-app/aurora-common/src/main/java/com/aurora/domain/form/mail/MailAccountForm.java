package com.aurora.domain.form.mail;

import com.aurora.enums.MailProviderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "邮箱账户表单")
public class MailAccountForm {

    @Schema(description = "账户ID，修改时必填")
    private Long id;

    @NotBlank(message = "账户名称不能为空")
    @Schema(description = "账户名称")
    private String accountName;

    @NotNull(message = "邮箱类型不能为空")
    @Schema(description = "邮箱类型")
    private MailProviderEnum provider;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱地址不能为空")
    @Schema(description = "邮箱地址")
    private String email;

    @Schema(description = "邮箱授权码；修改时留空表示不变")
    private String authCode;

    @Schema(description = "是否启用：0否，1是")
    private Integer enabled;

    @Schema(description = "排序")
    private Integer sort;
}
