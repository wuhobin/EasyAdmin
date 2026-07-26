package com.nexora.domain.vo.monitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "定时任务视图对象")
public class QuartzJobVo {
    @Schema(description = "任务ID")
    private Long jobId;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "Cron表达式")
    private String cronExpression;

    @Schema(description = "调用目标")
    private String invokeTarget;

    @Schema(description = "是否允许并发执行")
    private String concurrent;

    @Schema(description = "计划执行错误策略")
    private String misfirePolicy;

    @Schema(description = "任务状态")
    private String status;
}
