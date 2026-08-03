package com.nexora.system.entity;

import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_config_group")
@Schema(description = "系统配置分组")
public class SysConfigGroup extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "分组编码")
    private String groupCode;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "JSON 配置值")
    private String configValue;

    @Schema(description = "展示顺序")
    private Integer sort;
}
