package com.aurora.domain.form.query.monitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "缓存键查询表单")
public class CacheKeyQueryForm {
    @Schema(description = "缓存键关键字")
    private String key;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页数量")
    private Integer pageSize = 10;
}
