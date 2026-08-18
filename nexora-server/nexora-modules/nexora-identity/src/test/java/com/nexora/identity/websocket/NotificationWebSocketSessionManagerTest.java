package com.nexora.identity.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationWebSocketSessionManagerTest {
    @Test
    void closingOneTabKeepsTheOtherTabRegistered() throws Exception {
        NotificationWebSocketSessionManager manager = new NotificationWebSocketSessionManager(new ObjectMapper());
        WebSocketSession first = session("first");
        WebSocketSession second = session("second");
        manager.open(first, 7);
        manager.open(second, 7);

        manager.close(first);
        manager.push(7, Map.of("event", "notice-published"));

        verify(second).sendMessage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotRegisterAConnectionThatClosedBeforeOpen() throws Exception {
        NotificationWebSocketSessionManager manager = new NotificationWebSocketSessionManager(new ObjectMapper());
        WebSocketSession session = session("closed");
        when(session.isOpen()).thenReturn(false);

        manager.open(session, 7);
        manager.push(7, Map.of("event", "notice-published"));

        org.mockito.Mockito.verify(session, org.mockito.Mockito.never())
                .sendMessage(org.mockito.ArgumentMatchers.any());
    }

    private static WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
