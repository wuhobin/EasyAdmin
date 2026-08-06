package com.nexora.monitor.infrastructure.serverssh;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TerminalTicketStoreTest {

    @Test
    void consumesATicketExactlyOnce() {
        TerminalTicketStore store = new TerminalTicketStore();
        try {
            TerminalTicketStore.TerminalTicket ticket =
                    store.issue(7, 11L, "secret", 100, 30);

            assertThat(store.consume(ticket.value()))
                    .isNotNull()
                    .extracting(TerminalTicketStore.TerminalTicket::ownerId,
                            TerminalTicketStore.TerminalTicket::serverId)
                    .containsExactly(7, 11L);
            assertThat(store.consume(ticket.value())).isNull();
        } finally {
            store.shutdown();
        }
    }

    @Test
    void removesPendingTicketsForAnOwner() {
        TerminalTicketStore store = new TerminalTicketStore();
        try {
            TerminalTicketStore.TerminalTicket ticket =
                    store.issue(7, 11L, "secret", 80, 24);
            store.removeByOwnerId(7);

            assertThat(store.consume(ticket.value())).isNull();
        } finally {
            store.shutdown();
        }
    }
}
