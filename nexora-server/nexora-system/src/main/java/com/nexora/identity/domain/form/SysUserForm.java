package com.nexora.identity.domain.form;

import com.aurora.starter.common.utils.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Schema(description = "密码或重置后的新密码")
    private String password;

    @Schema(description = "旧密码")
    private String oldPassword;

    @Schema(description = "新密码")
    private String newPassword;

    @Pattern(regexp = "\\d{4,8}", message = "邮箱验证码格式不正确")
    @Schema(description = "邮箱验证码")
    private String code;

    @Min(value = 0, message = "用户状态不能小于0")
    @Max(value = 2, message = "用户状态不能大于2")
    @Schema(description = "用户状态：0禁用，1正常，2待审核")
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
