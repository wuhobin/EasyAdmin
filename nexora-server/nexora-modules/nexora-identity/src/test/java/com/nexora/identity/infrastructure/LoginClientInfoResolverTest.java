package com.nexora.identity.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginClientInfoResolverTest {

    private final LoginClientInfoResolver resolver = new LoginClientInfoResolver();

    @Test
    void resolvesForwardedIpBrowserAndOperatingSystemWithoutKeepingRawUserAgent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 Chrome/140.0.0.0 Safari/537.36";
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.8, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn(userAgent);

        LoginClientInfoResolver.ClientInfo clientInfo = resolver.resolve(request);

        assertThat(clientInfo.ip()).isEqualTo("203.0.113.8");
        assertThat(clientInfo.browser()).startsWith("Chrome 140.0.0.0");
        assertThat(clientInfo.os()).startsWith("Windows");
        assertThat(clientInfo.browser()).doesNotContain(userAgent);
        assertThat(clientInfo.os()).doesNotContain(userAgent);
    }

    @Test
    void returnsEmptyClientLabelsForAnUnknownUserAgent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("custom-client");

        LoginClientInfoResolver.ClientInfo clientInfo = resolver.resolve(request);

        assertThat(clientInfo.ip()).isEqualTo("127.0.0.1");
        assertThat(clientInfo.browser()).isNull();
        assertThat(clientInfo.os()).isNull();
    }
}
