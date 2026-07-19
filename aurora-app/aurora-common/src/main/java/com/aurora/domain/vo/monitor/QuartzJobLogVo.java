package com.aurora.domain.vo.monitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "定时任务日志视图对象")
public class QuartzJobLogVo {
    @Schema(description = "日志ID")
    private Long logId;

    @Schema(description = "任务ID")
    private Long jobId;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "调用目标")
    private String invokeTarget;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime stopTime;

    @Schema(description = "执行耗时，单位为毫秒")
    private Long costMillis;

    @Schema(description = "执行消息")
    private String jobMessage;

    @Schema(description = "执行状态")
    private String status;

    @Schema(description = "异常信息")
    private String exceptionInfo;
}
