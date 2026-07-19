package com.aurora.domain.form.monitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "定时任务状态表单")
public class QuartzJobStatusForm {
    @Schema(description = "任务ID")
    private Long jobId;

    @Schema(description = "任务状态")
    private String status;
}
