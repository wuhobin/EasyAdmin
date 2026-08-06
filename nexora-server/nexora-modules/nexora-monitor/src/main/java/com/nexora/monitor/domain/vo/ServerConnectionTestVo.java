package com.nexora.monitor.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServerConnectionTestVo {

    private String status;

    private String fingerprint;

    private String trustedFingerprint;

    private String algorithm;
}
