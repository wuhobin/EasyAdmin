package com.nexora.identity.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "微信登录轮询表单")
public class WechatLoginPollForm {

    /** 登录事务ID。 */
    @NotBlank(message = "登录事务ID不能为空")
    @Schema(description = "登录事务ID")
    private String transactionId;

    /** 浏览器持有的高熵轮询凭证。 */
    @NotBlank(message = "轮询凭证不能为空")
    @Schema(description = "轮询凭证")
    private String pollToken;
}
