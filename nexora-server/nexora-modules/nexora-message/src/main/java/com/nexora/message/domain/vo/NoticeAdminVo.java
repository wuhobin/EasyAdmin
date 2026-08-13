package com.nexora.message.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeAdminVo {
    private Long id;
    private String title;
    private String content;
    private String contentFormat;
    private Integer noticeType;
    private Integer targetType;
    private Object targetUserIds;
    private Integer status;
    private Integer createBy;
    private String createName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private LocalDateTime updateTime;
    private Long recipientCount;
    private Long readCount;
    private Long unreadCount;
}
