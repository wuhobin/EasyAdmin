package com.nexora.monitor.infrastructure;

import com.nexora.monitor.entity.ManagedServer;
import com.nexora.monitor.infrastructure.serverssh.SshConnectionService;
import com.nexora.monitor.infrastructure.serverssh.SshTargetValidator;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SshConnectionServiceTest {

    @TempDir
    private Path tempDirectory;

    private SshServer sshServer;

    @AfterEach
    void stopSshServer() throws IOException {
        if (sshServer != null) {
            sshServer.stop(true);
        }
    }

    @Test
    void waitsForKeyExchangeBeforeReadingTheServerHostKey() throws Exception {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setHost("127.0.0.1");
        sshServer.setPort(0);
        sshServer.setKeyPairProvider(
                new SimpleGeneratorHostKeyProvider(tempDirectory.resolve("host-key")));
        sshServer.setPasswordAuthenticator((username, password, session) -> true);
        sshServer.addSessionListener(new SessionListener() {
            @Override
            public void sessionNegotiationStart(
                    Session session,
                    Map<org.apache.sshd.common.kex.KexProposalOption, String> clientProposal,
                    Map<org.apache.sshd.common.kex.KexProposalOption, String> serverProposal) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        sshServer.start();

        SshTargetValidator targetValidator = mock(SshTargetValidator.class);
        when(targetValidator.resolveAllowedAddress("ssh.example"))
                .thenReturn(InetAddress.getLoopbackAddress());
        ManagedServer server = new ManagedServer();
        server.setHost("ssh.example");
        server.setPort(sshServer.getPort());
        server.setUsername("nexora");

        SshConnectionService.SshHostKey hostKey =
                new SshConnectionService(targetValidator).probeHostKey(server);

        assertThat(hostKey.fingerprint()).startsWith("SHA256:");
        assertThat(hostKey.algorithm()).isNotBlank();
    }
}
