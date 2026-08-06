package com.nexora.monitor.infrastructure.serverssh;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;

public final class AuthenticatedSshConnection implements AutoCloseable {

    private final SshClient client;
    private final ClientSession session;

    public AuthenticatedSshConnection(SshClient client, ClientSession session) {
        this.client = client;
        this.session = session;
    }

    public ClientSession session() {
        return session;
    }

    @Override
    public void close() {
        try {
            session.close(false);
        } finally {
            client.stop();
        }
    }
}
