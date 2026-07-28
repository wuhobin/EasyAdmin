package com.nexora.domain.form.system;

import com.aurora.starter.common.utils.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户操作表单")
public class SysUserForm {

    @Schema(description = "用户ID")
    private Integer id;

    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    @Schema(description = "用户昵称")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Size(min = 6, max = 20, message = "密码长度必须在6到20个字符之间")
    @Schema(description = "密码或重置后的新密码")
    private String password;

    @Size(min = 6, max = 20, message = "旧密码长度必须在6到20个字符之间")
    @Schema(description = "旧密码")
    private String oldPassword;

    @Size(min = 6, max = 20, message = "新密码长度必须在6到20个字符之间")
    @Schema(description = "新密码")
    private String newPassword;

    @Pattern(regexp = "\\d{4,8}", message = "邮箱验证码格式不正确")
    @Schema(description = "邮箱验证码")
    private String code;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "角色ID列表")
    private List<Integer> roleIds;

    public void setEmail(String email) {
        this.email = StringUtils.normalizeEmail(email);
    }
}
