package com.nexora.monitor.infrastructure;

import com.nexora.constants.CommonConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpRegionUtilsTest {

    @Test
    void resolvesAddressFromClasspathDatabase() {
        assertThat(IpRegionUtils.resolve("58.20.50.137"))
                .isNotBlank()
                .isNotEqualTo(CommonConstants.UNKNOWN);
    }

    @Test
    void returnsUnknownForInvalidIp() {
        assertThat(IpRegionUtils.resolve("not-an-ip")).isEqualTo(CommonConstants.UNKNOWN);
    }
}
