package com.nexora.monitor.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "服务器表单")
public class ManagedServerForm {

    @Schema(description = "服务器 ID，修改时必填")
    private Long id;

    @NotBlank(message = "服务器名称不能为空")
    @Size(max = 100, message = "服务器名称不能超过 100 个字符")
    private String name;

    @NotBlank(message = "服务器地址不能为空")
    @Size(max = 255, message = "服务器地址不能超过 255 个字符")
    private String host;

    @Min(value = 1, message = "SSH 端口必须大于 0")
    @Max(value = 65535, message = "SSH 端口不能超过 65535")
    private Integer port;

    @NotBlank(message = "SSH 用户名不能为空")
    @Size(max = 100, message = "SSH 用户名不能超过 100 个字符")
    private String username;

    @Size(max = 512, message = "SSH 密码不能超过 512 个字符")
    @Schema(description = "SSH 密码；接口不会回显")
    private String password;

    @Schema(description = "是否保存本次填写的密码")
    private Boolean savePassword;

    @Schema(description = "是否清除已经保存的密码")
    private Boolean clearSavedPassword;

    @Size(max = 500, message = "服务器描述不能超过 500 个字符")
    private String description;

    @Min(value = 0, message = "启用状态不正确")
    @Max(value = 1, message = "启用状态不正确")
    private Integer enabled;

    private Integer sort;
}
