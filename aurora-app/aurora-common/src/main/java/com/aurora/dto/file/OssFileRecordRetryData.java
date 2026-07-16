package com.aurora.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssFileRecordRetryData implements Serializable {

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
}
