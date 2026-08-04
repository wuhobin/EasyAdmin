package com.nexora.monitor.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;
import java.util.Map;

@Data
@Schema(description = "操作日志视图对象")
public class SysOperateLogVo {
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作用户ID")
    private Integer userId;

    @Schema(description = "请求地址")
    private String requestUrl;

    @Schema(description = "操作类型")
    private String type;

    @Schema(description = "操作名称")
    private String operationName;

    @Schema(description = "操作IP")
    private String ip;

    @Schema(description = "操作来源")
    private String source;

    @Schema(description = "执行耗时，单位为毫秒")
    private Long spendTime;

    @Schema(description = "请求参数JSON")
    private String paramsJson;

    @Schema(description = "类路径")
    private String classPath;

    @Schema(description = "方法名称")
    private String methodName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @Schema(description = "扩展参数")
    private Map<String, Object> params;
}
