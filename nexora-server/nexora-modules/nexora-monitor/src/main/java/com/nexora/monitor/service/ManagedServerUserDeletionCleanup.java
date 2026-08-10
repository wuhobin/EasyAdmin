package com.nexora.monitor.service;

import com.nexora.contract.UserDeletionCleanup;
import com.nexora.monitor.infrastructure.serverssh.SshTerminalSessionManager;
import com.nexora.monitor.infrastructure.serverssh.TerminalTicketStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ManagedServerUserDeletionCleanup implements UserDeletionCleanup {

    private final ManagedServerService serverService;
    private final SshTerminalSessionManager terminalSessionManager;
    private final TerminalTicketStore ticketStore;

    @Override
    public void cleanup(List<Integer> userIds) {
        userIds.forEach(ownerId -> {
            ticketStore.removeByOwnerId(ownerId);
            terminalSessionManager.closeByOwnerId(ownerId);
        });
        serverService.removeByOwnerIds(userIds);
    }
}
