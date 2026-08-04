package com.nexora.mail.biz;

import com.nexora.system.api.SystemMailSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMailSenderContractTest {

    @Test
    void systemMailImplementationProvidesTheSystemContract() {
        assertThat(SystemMailSender.class.isAssignableFrom(SystemMailBizService.class)).isTrue();
    }
}
