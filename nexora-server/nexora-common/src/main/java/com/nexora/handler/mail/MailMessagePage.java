package com.nexora.handler.mail;

import com.nexora.domain.vo.mail.MailMessageSummaryVo;

import java.util.List;

public record MailMessagePage(List<MailMessageSummaryVo> items, long anchorUid, boolean hasMore) {
}
