package com.nexora.identity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户未读通知数量视图对象")
public class NoticeUserUnreadVo {
    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "未读通知数量")
    private Long unreadCount;
}
