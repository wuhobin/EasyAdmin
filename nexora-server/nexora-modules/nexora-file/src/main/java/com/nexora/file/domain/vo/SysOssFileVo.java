package com.nexora.file.domain.vo;

import com.aurora.starter.common.utils.DateUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@Schema(description = "OSS文件视图对象")
public class SysOssFileVo {

    @Schema(description = "文件记录ID")
    private Long id;

    @Schema(description = "存储文件ID")
    private String fileId;

    @Schema(description = "文件访问地址")
    private String fileUrl;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "原始文件名称")
    private String originalFilename;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "文件大小，单位为字节")
    private Long fileSize;

    @Schema(description = "存储平台")
    private String platform;

    @Schema(description = "缩略图地址")
    private String thumbnailUrl;

    @Schema(description = "上传人ID")
    private Long uploaderId;

    @Schema(description = "分组ID")
    private Long groupId;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtils.YYYY_MM_DD_HH_MM_SS, timezone = "GMT+8")
    private Date createTime;
}
