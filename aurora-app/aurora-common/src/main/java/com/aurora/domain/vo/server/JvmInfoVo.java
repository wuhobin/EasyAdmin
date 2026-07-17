package com.aurora.domain.vo.server;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "JVM信息视图对象")
public class JvmInfoVo {
    @Schema(description = "JVM内存总量")
    private String total;

    @Schema(description = "JVM名称")
    private String name;

    @Schema(description = "JVM内存使用率")
    private String usage;

    @Schema(description = "Java版本")
    private String version;

    @Schema(description = "JDK安装路径")
    private String home;

    @Schema(description = "JVM启动时间")
    private String startTime;

    @Schema(description = "JVM运行时长")
    private String runTime;
}
