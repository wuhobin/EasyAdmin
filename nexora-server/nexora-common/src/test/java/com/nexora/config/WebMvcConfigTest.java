package com.nexora.config;

import com.nexora.handler.onlineuser.OnlineSessionTouchInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebMvcConfigTest {

    @Test
    void registersOnlineSessionTouchForAllMvcRequests() {
        OnlineSessionTouchInterceptor interceptor =
                mock(OnlineSessionTouchInterceptor.class);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        new WebMvcConfig(interceptor).addInterceptors(registry);

        verify(registry).addInterceptor(interceptor);
    }
}
