package com.nexora.file.entity;

import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("sys_oss_file")
@Schema(description = "OSS file record")
public class SysOssFile extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileId;

    private String fileUrl;

    private String fileName;

    private String originalFilename;

    private String contentType;

    private Long fileSize;

    private String platform;

    private String thumbnailUrl;

    private Long uploaderId;

    private Long groupId;

    @TableField(exist = false)
    private String groupName;
}
