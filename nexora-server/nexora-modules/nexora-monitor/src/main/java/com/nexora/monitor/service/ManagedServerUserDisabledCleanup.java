package com.nexora.monitor.service;

import com.nexora.contract.UserDisabledCleanup;
import com.nexora.monitor.infrastructure.serverssh.SshTerminalSessionManager;
import com.nexora.monitor.infrastructure.serverssh.TerminalTicketStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManagedServerUserDisabledCleanup implements UserDisabledCleanup {

    private final SshTerminalSessionManager terminalSessionManager;
    private final TerminalTicketStore ticketStore;

    @Override
    public void cleanup(Integer userId) {
        ticketStore.removeByOwnerId(userId);
        terminalSessionManager.closeByDisabledOwnerId(userId);
    }
}
