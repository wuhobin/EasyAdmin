package com.nexora.message.domain.vo;

import lombok.Data;

@Data
public class NoticeUserUnreadVo {
    private Integer userId;
    private Long unreadCount;
}
