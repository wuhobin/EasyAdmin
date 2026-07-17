package com.aurora.domain.vo.file;

import com.aurora.starter.common.utils.DateUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@Schema(description = "OSS 文件列表视图对象")
public class SysOssFileVo {

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

    private String uploaderName;

    @JsonFormat(pattern = DateUtils.YYYY_MM_DD_HH_MM_SS, timezone = "GMT+8")
    private Date createTime;
}
