package com.nexora.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.identity.domain.vo.NoticeUserUnreadVo;
import com.nexora.identity.entity.SysNotice;
import com.nexora.identity.mapper.SysUserNoticeMapper;
import com.nexora.identity.websocket.NotificationWebSocketSessionManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationPushServiceTest {
    @Test
    void loadsUnreadCountsInOneBatchBeforePushingToRecipients() {
        NotificationWebSocketSessionManager sessionManager = mock(NotificationWebSocketSessionManager.class);
        SysUserNoticeMapper mapper = mock(SysUserNoticeMapper.class);
        NoticeUserUnreadVo first = new NoticeUserUnreadVo();
        first.setUserId(1);
        first.setUnreadCount(4L);
        when(mapper.selectUnreadCounts(List.of(1, 2))).thenReturn(Map.of(1, first));
        NotificationPushService service = new NotificationPushService(sessionManager, mapper);
        SysNotice notice = new SysNotice();
        notice.setId(9L);
        notice.setNoticeType(1);
        notice.setTitle("系统维护");

        service.pushPublished(notice, List.of(1, 2));

        verify(mapper).selectUnreadCounts(List.of(1, 2));
        var firstEvent = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(sessionManager).push(eq(1), firstEvent.capture());
        assertThat(firstEvent.getValue()).containsEntry("unreadCount", 4L);
        var secondEvent = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(sessionManager).push(eq(2), secondEvent.capture());
        assertThat(secondEvent.getValue()).containsEntry("unreadCount", 0L);
    }
}
