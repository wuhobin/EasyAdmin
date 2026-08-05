package com.nexora.monitor.task;

import com.nexora.monitor.biz.OnlineSessionBizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("onlineSessionCleanupTask")
@RequiredArgsConstructor
public class OnlineSessionCleanupTask {

    private final OnlineSessionBizService onlineSessionBizService;

    public void cleanupInvalidSessions() {
        int removedCount = onlineSessionBizService.cleanupInvalidSessions();
        log.info("在线会话清理任务完成，已删除失效会话 {} 个", removedCount);
    }
}
