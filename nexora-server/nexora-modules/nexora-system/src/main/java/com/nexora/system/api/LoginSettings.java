package com.nexora.system.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "用户登录配置")
public class LoginSettings {

    @NotNull(message = "最大重试次数不能为空")
    @Min(value = 1, message = "最大重试次数不能小于1")
    @Max(value = 20, message = "最大重试次数不能大于20")
    private Integer maxRetryCount;

    @NotNull(message = "锁定时间不能为空")
    @Min(value = 1, message = "锁定时间不能小于1分钟")
    @Max(value = 1440, message = "锁定时间不能大于1440分钟")
    private Integer lockTimeMinutes;

    @NotNull(message = "记住我开关不能为空")
    private Boolean rememberMeEnabled;

    @NotNull(message = "普通会话时长不能为空")
    @Min(value = 300, message = "普通会话时长不能小于300秒")
    @Max(value = 86400, message = "普通会话时长不能大于86400秒")
    private Long sessionTimeoutSeconds;

    @NotNull(message = "记住我会话时长不能为空")
    @Min(value = 3600, message = "记住我会话时长不能小于3600秒")
    @Max(value = 31536000, message = "记住我会话时长不能大于31536000秒")
    private Long rememberMeTimeoutSeconds;

    @NotNull(message = "单点登录开关不能为空")
    private Boolean singleLogin;

    @JsonIgnore
    @AssertTrue(message = "记住我会话时长不能小于普通会话时长")
    public boolean isRememberMeTimeoutValid() {
        return sessionTimeoutSeconds == null || rememberMeTimeoutSeconds == null
                || rememberMeTimeoutSeconds >= sessionTimeoutSeconds;
    }
}
