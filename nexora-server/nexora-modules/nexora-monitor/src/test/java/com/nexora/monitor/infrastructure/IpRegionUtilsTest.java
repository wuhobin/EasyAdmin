package com.nexora.monitor.infrastructure;

import com.nexora.monitor.constants.MonitorConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpRegionUtilsTest {

    @Test
    void resolvesAddressFromClasspathDatabase() {
        assertThat(IpRegionUtils.resolve("58.20.50.137"))
                .isNotBlank()
                .isNotEqualTo(MonitorConstants.UNKNOWN_REGION);
    }

    @Test
    void returnsUnknownForInvalidIp() {
        assertThat(IpRegionUtils.resolve("not-an-ip"))
                .isEqualTo(MonitorConstants.UNKNOWN_REGION);
    }
}
