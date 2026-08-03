package com.nexora.monitor.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "定时任务立即执行表单")
public class QuartzJobRunForm {
    @Schema(description = "任务ID")
    private Long jobId;
}
