package com.nexora.mail.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailProviderEnumTest {

    @Test
    void matchesEmailDomainForEachProvider() {
        assertThat(MailProviderEnum.QQ.matchesEmail("user@qq.com")).isTrue();
        assertThat(MailProviderEnum.NETEASE_163.matchesEmail("user@163.com")).isTrue();
        assertThat(MailProviderEnum.NETEASE_126.matchesEmail("user@126.com")).isTrue();
        assertThat(MailProviderEnum.YEAH.matchesEmail("user@yeah.net")).isTrue();
        assertThat(MailProviderEnum.GMAIL.matchesEmail("user@gmail.com")).isTrue();
        assertThat(MailProviderEnum.GMAIL.getHost()).isEqualTo("imap.gmail.com");
        assertThat(MailProviderEnum.GMAIL.getPort()).isEqualTo(993);
    }

    @Test
    void rejectsBlankLocalPartAndMismatchedDomain() {
        assertThat(MailProviderEnum.QQ.matchesEmail("@qq.com")).isFalse();
        assertThat(MailProviderEnum.QQ.matchesEmail("user@163.com")).isFalse();
        assertThat(MailProviderEnum.QQ.matchesEmail(null)).isFalse();
    }
}
