package com.nexora.message.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "通知管理视图对象")
public class NoticeAdminVo {
    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "内容格式")
    private String contentFormat;

    @Schema(description = "通知类型")
    private Integer noticeType;

    @Schema(description = "目标类型")
    private Integer targetType;

    @Schema(description = "目标用户ID")
    private Object targetUserIds;

    @Schema(description = "通知状态")
    private Integer status;

    @Schema(description = "创建人ID")
    private Integer createBy;

    @Schema(description = "创建人名称")
    private String createName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "发布时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "接收人数")
    private Long recipientCount;

    @Schema(description = "已读人数")
    private Long readCount;

    @Schema(description = "未读人数")
    private Long unreadCount;
}
