package com.aurora.domain.vo.server;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "操作系统信息视图对象")
public class SysInfoVo {
    @Schema(description = "服务器名称")
    private String computerName;

    @Schema(description = "服务器IP")
    private String computerIp;

    @Schema(description = "操作系统名称")
    private String osName;

    @Schema(description = "系统架构")
    private String osArch;

    @Schema(description = "项目路径")
    private String userDir;
}
