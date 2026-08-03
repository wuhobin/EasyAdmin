package com.nexora.mail.infrastructure;

import com.nexora.mail.domain.vo.MailMessageSummaryVo;

import java.util.List;

public record MailMessagePage(List<MailMessageSummaryVo> items, long anchorUid, boolean hasMore) {
}
