package com.aurora.domain.vo.server;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "内存信息视图对象")
public class MemInfoVo {
    @Schema(description = "内存总量")
    private long total;

    @Schema(description = "已用内存")
    private long used;

    @Schema(description = "剩余内存")
    private long free;

    @Schema(description = "内存使用率")
    private double usage;
}
