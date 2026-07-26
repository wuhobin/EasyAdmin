package com.nexora.domain.form.query.monitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "定时任务日志查询表单")
public class QuartzJobLogQueryForm {
    @Schema(description = "任务名称关键字")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "执行状态")
    private String status;
}
