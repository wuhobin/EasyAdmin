package com.aurora.entity;

import cn.idev.excel.annotation.ExcelProperty;
import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@TableName("sys_role")
@Schema(description = "角色信息")
@EqualsAndHashCode
public class SysRole extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @Schema(description = "角色编码")
    @ExcelProperty(value = "角色编码")
    private String code;

    @Schema(description = "角色名称")
    @ExcelProperty(value = "角色名称")
    private String name;

    @Schema(description = "角色描述")
    private String remarks;
}