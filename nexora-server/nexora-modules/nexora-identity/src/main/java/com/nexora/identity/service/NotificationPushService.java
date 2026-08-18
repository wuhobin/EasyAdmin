package com.nexora.identity.service;

import com.nexora.identity.domain.vo.NoticeUserUnreadVo;
import com.nexora.identity.entity.SysNotice;
import com.nexora.identity.mapper.SysUserNoticeMapper;
import com.nexora.identity.websocket.NotificationWebSocketSessionManager;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationPushService {
    private final NotificationWebSocketSessionManager sessionManager;
    private final SysUserNoticeMapper userNoticeMapper;

    public NotificationPushService(NotificationWebSocketSessionManager sessionManager,
                                   SysUserNoticeMapper userNoticeMapper) {
        this.sessionManager = sessionManager;
        this.userNoticeMapper = userNoticeMapper;
    }

    public void pushPublished(SysNotice notice, List<Integer> userIds) {
        Map<Integer, NoticeUserUnreadVo> unreadCounts = userIds.isEmpty()
                ? Map.of()
                : userNoticeMapper.selectUnreadCounts(userIds);
        for (Integer userId : userIds) {
            NoticeUserUnreadVo unreadVo = unreadCounts.get(userId);
            long unread = unreadVo == null || unreadVo.getUnreadCount() == null
                    ? 0L : unreadVo.getUnreadCount();
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("event", "notice-published");
            event.put("noticeId", notice.getId());
            event.put("noticeType", notice.getNoticeType());
            event.put("title", notice.getTitle());
            event.put("publishTime", notice.getPublishTime());
            event.put("unreadCount", unread);
            sessionManager.push(userId, event);
        }
    }
}
