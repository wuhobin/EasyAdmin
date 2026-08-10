package com.nexora.message.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeUserVo {
    private Long id;
    private Long noticeId;
    private String title;
    private String content;
    private String contentPreview;
    private String contentFormat;
    private Integer noticeType;
    private Integer isRead;
    private LocalDateTime publishTime;
}
