package com.aurora.service.impl;

import com.aurora.domain.dto.LoginDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceImplTest {

    @Test
    void selectsOneHourTimeoutWhenRememberMeIsFalse() {
        assertThat(AuthServiceImpl.tokenTimeout(false)).isEqualTo(3_600L);
    }

    @Test
    void selectsThreeDayTimeoutWhenRememberMeIsTrue() {
        assertThat(AuthServiceImpl.tokenTimeout(true)).isEqualTo(259_200L);
    }

    @Test
    void defaultsRememberMeToFalseWhenTheFieldIsMissing() throws Exception {
        LoginDTO loginDTO = new ObjectMapper().readValue(
                "{\"username\":\"admin\",\"password\":\"secret\"}",
                LoginDTO.class
        );

        assertThat(loginDTO.isRememberMe()).isFalse();
    }

    @Test
    void bindsRememberMeWhenTheFieldIsProvided() throws Exception {
        LoginDTO loginDTO = new ObjectMapper().readValue(
                "{\"username\":\"admin\",\"password\":\"secret\",\"rememberMe\":true}",
                LoginDTO.class
        );

        assertThat(loginDTO.isRememberMe()).isTrue();
    }

    @Test
    void onlyLoginRemainsInTheApplicationAuthenticationAllowList() throws Exception {
        String config = Files.readString(Path.of(
                "..", "aurora-server", "src", "main", "resources", "config", "platform.yml"
        ));

        assertThat(config).contains("exclude-paths:\n      - /auth/login");
        assertThat(config).doesNotContain("- /auth/info", "- /auth/logout", "- /auth/verify");
    }
}
