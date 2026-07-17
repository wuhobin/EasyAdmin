package com.aurora.domain.vo.server;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "服务器信息视图对象")
public class ServerInfoVo {
    @Schema(description = "CPU信息")
    private CpuInfoVo cpu;

    @Schema(description = "内存信息")
    private MemInfoVo mem;

    @Schema(description = "操作系统信息")
    private SysInfoVo sys;

    @Schema(description = "JVM信息")
    private JvmInfoVo jvm;

    @Schema(description = "磁盘信息列表")
    private List<SysFileVo> sysFiles;
}
