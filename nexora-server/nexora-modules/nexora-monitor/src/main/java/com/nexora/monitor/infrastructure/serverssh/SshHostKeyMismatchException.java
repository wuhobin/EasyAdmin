package com.nexora.monitor.infrastructure.serverssh;

public class SshHostKeyMismatchException extends RuntimeException {

    private final String trustedFingerprint;
    private final String presentedFingerprint;
    private final String algorithm;

    public SshHostKeyMismatchException(String trustedFingerprint, String presentedFingerprint,
                                       String algorithm) {
        super("SSH host key mismatch");
        this.trustedFingerprint = trustedFingerprint;
        this.presentedFingerprint = presentedFingerprint;
        this.algorithm = algorithm;
    }

    public String getTrustedFingerprint() {
        return trustedFingerprint;
    }

    public String getPresentedFingerprint() {
        return presentedFingerprint;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
