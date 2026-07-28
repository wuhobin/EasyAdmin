package com.nexora.entity;

import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@TableName("sys_operate_log")
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "操作日志")
public class SysOperateLog extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "操作用户ID")
    private Integer userId;

    @Schema(description = "请求接口")
    private String requestUrl;

    @Schema(description = "请求方式")
    private String type;

    @Schema(description = "操作名称")
    private String operationName;

    @Schema(description = "ip")
    private String ip;

    @Schema(description = "ip来源")
    private String source;

    @Schema(description = "请求接口耗时")
    private Long spendTime;

    @Schema(description = "请求参数")
    private String paramsJson;

    @Schema(description = "类地址")
    private String classPath;

    @Schema(description = "方法名")
    private String methodName;
}
