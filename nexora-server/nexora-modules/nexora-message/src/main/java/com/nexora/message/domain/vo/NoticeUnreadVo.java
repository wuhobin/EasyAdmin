package com.nexora.message.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "未读通知数量视图对象")
public class NoticeUnreadVo {
    @Schema(description = "未读通知数量")
    private long unreadCount;
}
