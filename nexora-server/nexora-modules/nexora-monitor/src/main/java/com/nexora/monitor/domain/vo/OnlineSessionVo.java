package com.nexora.monitor.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "在线会话")
public class OnlineSessionVo {

    @Schema(description = "公开会话编号")
    private String sessionId;

    @Schema(description = "登录邮箱")
    private String email;

    @Schema(description = "登录昵称")
    private String nickname;

    @Schema(description = "登录 IP")
    private String ip;

    @Schema(description = "登录地点")
    private String location;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    @Schema(description = "最后访问时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;

    @Schema(description = "是否为当前请求会话")
    private boolean currentSession;
}
