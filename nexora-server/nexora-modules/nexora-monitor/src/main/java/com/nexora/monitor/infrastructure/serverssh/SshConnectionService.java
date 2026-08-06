package com.nexora.monitor.infrastructure.serverssh;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.monitor.constants.ServerConstants;
import com.nexora.monitor.entity.ManagedServer;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.ClientSession.ClientSessionEvent;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.security.PublicKey;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SshConnectionService {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(10);

    private final SshTargetValidator targetValidator;

    public SshConnectionService(SshTargetValidator targetValidator) {
        this.targetValidator = targetValidator;
    }

    public SshHostKey probeHostKey(ManagedServer server) {
        AtomicReference<SshHostKey> presented = new AtomicReference<>();
        AtomicReference<Throwable> handshakeFailure = new AtomicReference<>();
        ServerKeyVerifier verifier = (session, remoteAddress, serverKey) -> {
            presented.set(toHostKey(serverKey));
            return true;
        };
        SshClient client = startClient(verifier);
        client.addSessionListener(new SessionListener() {
            @Override
            public void sessionException(Session session, Throwable throwable) {
                handshakeFailure.compareAndSet(null, throwable);
            }
        });
        try {
            InetAddress target = targetValidator.resolveAllowedAddress(server.getHost());
            try (ClientSession session = client.connect(
                    server.getUsername(), target.getHostAddress(), server.getPort())
                    .verify(CONNECTION_TIMEOUT)
                    .getSession()) {
                Set<ClientSessionEvent> state = session.waitFor(
                        EnumSet.of(ClientSessionEvent.WAIT_AUTH, ClientSessionEvent.CLOSED),
                        HANDSHAKE_TIMEOUT);
                if (!state.contains(ClientSessionEvent.WAIT_AUTH)) {
                    Throwable cause = handshakeFailure.get();
                    if (cause == null) {
                        cause = new IllegalStateException(
                                "SSH handshake did not reach authentication phase: " + state);
                    }
                    throw new BizException(ServerConstants.SSH_CONNECTION_FAILED_MESSAGE, cause);
                }
                SshHostKey hostKey = presented.get();
                if (hostKey == null) {
                    throw new BizException(
                            ServerConstants.SSH_CONNECTION_FAILED_MESSAGE,
                            new IllegalStateException("SSH server did not present a host key"));
                }
                return hostKey;
            }
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ServerConstants.SSH_CONNECTION_FAILED_MESSAGE, exception);
        } finally {
            client.stop();
        }
    }

    public AuthenticatedSshConnection authenticate(ManagedServer server, String password) {
        AtomicReference<SshHostKey> presented = new AtomicReference<>();
        String trustedFingerprint = server.getTrustedFingerprint();
        ServerKeyVerifier verifier = (session, remoteAddress, serverKey) -> {
            SshHostKey hostKey = toHostKey(serverKey);
            presented.set(hostKey);
            return hostKey.fingerprint().equals(trustedFingerprint);
        };
        SshClient client = startClient(verifier);
        try {
            InetAddress target = targetValidator.resolveAllowedAddress(server.getHost());
            ClientSession session = client.connect(
                    server.getUsername(), target.getHostAddress(), server.getPort())
                    .verify(CONNECTION_TIMEOUT)
                    .getSession();
            try {
                session.addPasswordIdentity(password);
                session.auth().verify(AUTH_TIMEOUT);
                return new AuthenticatedSshConnection(client, session);
            } catch (Exception exception) {
                session.close(false);
                throw exception;
            }
        } catch (BizException exception) {
            client.stop();
            throw exception;
        } catch (Exception exception) {
            client.stop();
            SshHostKey actual = presented.get();
            if (actual != null && !actual.fingerprint().equals(trustedFingerprint)) {
                throw new SshHostKeyMismatchException(
                        trustedFingerprint, actual.fingerprint(), actual.algorithm());
            }
            throw new BizException(ServerConstants.SSH_CONNECTION_FAILED_MESSAGE, exception);
        }
    }

    private static SshClient startClient(ServerKeyVerifier verifier) {
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier(verifier);
        client.start();
        return client;
    }

    private static SshHostKey toHostKey(PublicKey serverKey) {
        return new SshHostKey(KeyUtils.getFingerPrint(serverKey), serverKey.getAlgorithm());
    }

    public record SshHostKey(String fingerprint, String algorithm) {
    }
}
