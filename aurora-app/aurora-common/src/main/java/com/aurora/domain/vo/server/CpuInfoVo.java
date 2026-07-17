package com.aurora.domain.vo.server;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "CPU信息视图对象")
public class CpuInfoVo {
    @Schema(description = "CPU核心数")
    private int cpuNum;

    @Schema(description = "用户使用率")
    private double used;

    @Schema(description = "系统使用率")
    private double sys;

    @Schema(description = "空闲率")
    private double free;
}
