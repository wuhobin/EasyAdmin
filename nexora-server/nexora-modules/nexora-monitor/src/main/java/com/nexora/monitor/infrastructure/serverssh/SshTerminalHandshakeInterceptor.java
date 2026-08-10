package com.nexora.monitor.infrastructure.serverssh;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class SshTerminalHandshakeInterceptor implements HandshakeInterceptor {

    private final TerminalTicketStore ticketStore;

    public SshTerminalHandshakeInterceptor(TerminalTicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler webSocketHandler,
                                   Map<String, Object> attributes) {
        String ticketValue = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("ticket");
        TerminalTicketStore.TerminalTicket ticket = ticketStore.consume(ticketValue);
        if (ticket == null) {
            return false;
        }
        attributes.put(TerminalTicketStore.HANDSHAKE_TICKET_ATTRIBUTE, ticket);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler webSocketHandler, Exception exception) {
        // No post-handshake action is required.
    }
}
