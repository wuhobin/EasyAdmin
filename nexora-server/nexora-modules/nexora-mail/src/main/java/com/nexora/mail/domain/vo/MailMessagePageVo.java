package com.nexora.mail.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "邮件列表分页视图对象")
public class MailMessagePageVo {
    @Schema(description = "邮件摘要列表")
    private List<MailMessageSummaryVo> items;

    @Schema(description = "下一页游标")
    private String nextCursor;

    @Schema(description = "是否还有更多数据")
    private boolean hasMore;
}
