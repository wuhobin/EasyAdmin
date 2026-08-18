package com.nexora.monitor.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "服务器连接测试结果视图对象")
public class ServerConnectionTestVo {

    @Schema(description = "连接测试状态")
    private String status;

    @Schema(description = "服务器指纹")
    private String fingerprint;

    @Schema(description = "已信任的指纹")
    private String trustedFingerprint;

    @Schema(description = "指纹算法")
    private String algorithm;
}
