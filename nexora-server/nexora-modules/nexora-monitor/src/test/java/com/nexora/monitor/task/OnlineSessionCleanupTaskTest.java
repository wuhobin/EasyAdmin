package com.nexora.monitor.task;

import com.nexora.monitor.biz.OnlineSessionBizService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnlineSessionCleanupTaskTest {

    @Test
    void delegatesInvalidSessionCleanupToTheBusinessService() {
        OnlineSessionBizService service = mock(OnlineSessionBizService.class);
        when(service.cleanupInvalidSessions()).thenReturn(3);
        OnlineSessionCleanupTask task = new OnlineSessionCleanupTask(service);

        task.cleanupInvalidSessions();

        verify(service).cleanupInvalidSessions();
    }
}
