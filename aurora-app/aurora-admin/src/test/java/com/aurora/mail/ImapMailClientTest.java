package com.aurora.mail;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImapMailClientTest {

    @Test
    void sanitizesScriptsAndTrackingImagesButKeepsInlineCidImages() {
        String html = """
                <div onclick="alert(1)">
                  <script>alert(1)</script>
                  <img src="https://tracker.example/pixel.png">
                  <img src="cid:logo">
                  <a href="javascript:alert(1)">bad link</a>
                </div>
                """;

        String sanitized = ImapMailClient.sanitizeHtml(html,
                Map.of("logo", "data:image/png;base64,AAAA"));

        assertThat(sanitized).doesNotContain("script", "onclick", "tracker.example", "javascript:");
        assertThat(sanitized).contains("data:image/png;base64,AAAA");
    }
}
