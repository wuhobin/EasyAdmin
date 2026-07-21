package com.aurora.domain.vo.mail;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MailMessagePageVo {
    private List<MailMessageSummaryVo> items;
    private String nextCursor;
    private boolean hasMore;
}
