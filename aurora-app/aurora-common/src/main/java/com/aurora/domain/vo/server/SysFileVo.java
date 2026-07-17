package com.aurora.domain.vo.server;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "磁盘信息视图对象")
public class SysFileVo {
    @Schema(description = "盘符路径")
    private String dirName;

    @Schema(description = "文件系统类型")
    private String typeName;

    @Schema(description = "总容量")
    private long total;

    @Schema(description = "剩余容量")
    private long free;

    @Schema(description = "已用容量")
    private long used;

    @Schema(description = "使用率")
    private double usage;
}
