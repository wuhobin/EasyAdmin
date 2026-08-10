package com.nexora.file.entity;

import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_file_group")
@Schema(description = "OSS 文件分组")
public class SysOssFileGroup extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "分组所有者ID")
    private Long ownerId;

    @Schema(description = "分组名称")
    private String name;

    @TableField(exist = false)
    @Schema(description = "分组文件数量")
    private Long fileCount;
}
