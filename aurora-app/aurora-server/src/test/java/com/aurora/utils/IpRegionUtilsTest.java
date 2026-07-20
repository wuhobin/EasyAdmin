package com.aurora.utils;

import com.aurora.constants.Constants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpRegionUtilsTest {

    @Test
    void resolvesAddressFromClasspathDatabase() {
        assertThat(IpRegionUtils.resolve("58.20.50.137"))
                .isNotBlank()
                .isNotEqualTo(Constants.UNKNOWN);
    }

    @Test
    void returnsUnknownForInvalidIp() {
        assertThat(IpRegionUtils.resolve("not-an-ip")).isEqualTo(Constants.UNKNOWN);
    }
}
