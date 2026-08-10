package com.nexora.monitor.service;

import com.nexora.monitor.infrastructure.serverssh.SshTerminalSessionManager;
import com.nexora.monitor.infrastructure.serverssh.TerminalTicketStore;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ManagedServerUserDisabledCleanupTest {

    @Test
    void removesTicketsAndClosesActiveTerminals() {
        SshTerminalSessionManager sessionManager = mock(SshTerminalSessionManager.class);
        TerminalTicketStore ticketStore = mock(TerminalTicketStore.class);
        ManagedServerUserDisabledCleanup cleanup =
                new ManagedServerUserDisabledCleanup(sessionManager, ticketStore);

        cleanup.cleanup(27);

        verify(ticketStore).removeByOwnerId(27);
        verify(sessionManager).closeByDisabledOwnerId(27);
    }
}
