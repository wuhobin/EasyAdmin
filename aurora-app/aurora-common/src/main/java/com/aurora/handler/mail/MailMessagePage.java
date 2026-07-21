package com.aurora.handler.mail;

import com.aurora.domain.vo.mail.MailMessageSummaryVo;

import java.util.List;

public record MailMessagePage(List<MailMessageSummaryVo> items, long anchorUid, boolean hasMore) {
}
