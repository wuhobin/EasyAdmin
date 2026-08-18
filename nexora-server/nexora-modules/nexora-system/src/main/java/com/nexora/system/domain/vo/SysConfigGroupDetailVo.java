package com.nexora.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@Schema(description = "系统配置分组详情")
public class SysConfigGroupDetailVo {

    @Schema(description = "分组ID")
    private Long id;

    @Schema(description = "分组编码")
    private String groupCode;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "配置值")
    private Object configValue;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
