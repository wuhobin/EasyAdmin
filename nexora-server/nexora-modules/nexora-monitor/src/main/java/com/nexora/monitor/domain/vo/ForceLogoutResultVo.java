package com.nexora.monitor.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "强退在线会话结果")
public class ForceLogoutResultVo {

    @Schema(description = "强退结果")
    private final Outcome outcome;

    @Schema(description = "目标是否为当前请求会话")
    private final boolean currentSession;

    public enum Outcome {
        LOGGED_OUT,
        ALREADY_OFFLINE
    }
}
