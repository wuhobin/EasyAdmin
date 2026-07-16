package com.aurora.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLoggerAspectTest {

    @Test
    void resolvesOneBasedOperationNamePlaceholders() {
        String name = OperationLoggerAspect.formatOperationName(
                "删除用户 {1}", new Object[]{List.of(1, 2)});

        assertThat(name).isEqualTo("删除用户 [1,2]");
    }

    @Test
    void filtersServletAndMultipartArgumentsFromParameters() {
        HttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", "text/plain", "a".getBytes());

        String json = OperationLoggerAspect.serializeParameters(
                new String[]{"name", "request", "file"},
                new Object[]{"admin", request, file});

        assertThat(json).isEqualTo("{\"name\":\"admin\"}");
    }

    @Test
    void doesNotStoreRequestTimingInAspectInstanceField() {
        boolean containsStartTimeField = Arrays.stream(OperationLoggerAspect.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("startTime"));

        assertThat(containsStartTimeField).isFalse();
    }
}
