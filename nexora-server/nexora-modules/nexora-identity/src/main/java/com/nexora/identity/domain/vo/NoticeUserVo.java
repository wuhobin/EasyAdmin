package com.nexora.identity.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户通知视图对象")
public class NoticeUserVo {
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "通知ID")
    private Long noticeId;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "内容预览")
    private String contentPreview;

    @Schema(description = "内容格式")
    private String contentFormat;

    @Schema(description = "通知类型")
    private Integer noticeType;

    @Schema(description = "是否已读")
    private Integer isRead;

    @Schema(description = "发布时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
}
