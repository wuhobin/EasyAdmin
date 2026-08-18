package com.nexora.file.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nexora.file.entity.SysOssFileGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@Schema(description = "OSS 文件分组视图")
public class SysOssFileGroupVo {

    @Schema(description = "分组ID")
    private Long id;

    @Schema(description = "分组所有者ID")
    private Long ownerId;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "文件数量")
    private Long fileCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    public static SysOssFileGroupVo from(SysOssFileGroup group) {
        return SysOssFileGroupVo.builder()
                .id(group.getId())
                .ownerId(group.getOwnerId())
                .name(group.getName())
                .fileCount(group.getFileCount())
                .createTime(group.getCreateTime())
                .updateTime(group.getUpdateTime())
                .build();
    }
}
