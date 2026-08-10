package com.nexora.message.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {
    private final NotificationTicketStore ticketStore;

    public NotificationHandshakeInterceptor(NotificationTicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String value = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("ticket");
        NotificationTicketStore.Ticket ticket = ticketStore.consume(value);
        if (ticket == null || ticket.userId() == null) {
            return false;
        }
        attributes.put(NotificationTicketStore.HANDSHAKE_ATTRIBUTE, ticket);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
    }
}
