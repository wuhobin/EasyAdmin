package com.nexora.monitor.infrastructure;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.monitor.infrastructure.serverssh.SshTargetValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SshTargetValidatorTest {

    private final SshTargetValidator validator = new SshTargetValidator();

    @Test
    void allowsPublicAndPrivateRemoteAddresses() {
        assertThat(validator.resolveAllowedAddress("8.8.8.8").getHostAddress())
                .isEqualTo("8.8.8.8");
        assertThat(validator.resolveAllowedAddress("10.203.0.1").isSiteLocalAddress())
                .isTrue();
    }

    @Test
    void rejectsLoopbackUnspecifiedAndLinkLocalAddresses() {
        assertThatThrownBy(() -> validator.resolveAllowedAddress("127.0.0.1"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> validator.resolveAllowedAddress("0.0.0.0"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> validator.resolveAllowedAddress("169.254.169.254"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> validator.resolveAllowedAddress("::1"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void rejectsDocumentationBenchmarkAndSharedAddressRanges() {
        assertThatThrownBy(() -> validator.resolveAllowedAddress("192.0.2.10"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> validator.resolveAllowedAddress("198.19.0.1"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> validator.resolveAllowedAddress("100.64.0.1"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> validator.resolveAllowedAddress("2001:db8::1"))
                .isInstanceOf(BizException.class);
    }
}
