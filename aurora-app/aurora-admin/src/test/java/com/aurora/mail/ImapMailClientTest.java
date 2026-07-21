package com.aurora.handler.mail;

import com.aurora.constants.MailProviderEnum;
import org.junit.jupiter.api.Test;
import org.eclipse.angus.mail.imap.IMAPStore;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ImapMailClientTest {

    @Test
    void sendsRfc2971ClientIdentificationForNetEaseBeforeMailboxAccess() throws Exception {
        IMAPStore store = mock(IMAPStore.class);

        ImapMailClient.identifyClient(store, MailProviderEnum.NETEASE_163);

        verify(store).id(argThat(parameters ->
                "EasyAdmin".equals(parameters.get("name"))
                        && "1.0".equals(parameters.get("version"))
                        && "Aurora".equals(parameters.get("vendor"))));
    }

    @Test
    void doesNotSendClientIdentificationForQqMail() throws Exception {
        IMAPStore store = mock(IMAPStore.class);

        ImapMailClient.identifyClient(store, MailProviderEnum.QQ);

        verify(store, never()).id(argThat(parameters -> true));
    }

    @Test
    void sanitizesScriptsButKeepsExternalAndInlineImages() {
        String html = """
                <html><head>
                  <style>.hero { background-image: url('https://cdn.example/hero.png'); }</style>
                </head><body>
                <div onclick="alert(1)">
                  <script>alert(1)</script>
                  <table width="600" bgcolor="#ffffff"><tr><td class="hero">content</td></tr></table>
                  <img src="https://tracker.example/pixel.png">
                  <img src="cid:logo">
                  <a href="javascript:alert(1)">bad link</a>
                  <a href="https://www.example.com/detail">safe link</a>
                  <form><p>visible form text</p><input value="hidden control"></form>
                </div>
                </body></html>
                """;

        String sanitized = ImapMailClient.sanitizeHtml(html,
                Map.of("logo", "data:image/png;base64,AAAA"));

        assertThat(sanitized).doesNotContain("script", "onclick", "javascript:");
        assertThat(sanitized).contains("https://tracker.example/pixel.png", "referrerpolicy=\"no-referrer\"");
        assertThat(sanitized).contains("data:image/png;base64,AAAA");
        assertThat(sanitized).contains("<style>", "background-image", "width=\"600\"", "bgcolor=\"#ffffff\"");
        assertThat(sanitized).contains("visible form text").doesNotContain("hidden control", "<form", "<input");
        assertThat(sanitized).contains("href=\"https://www.example.com/detail\"",
                "target=\"_blank\"", "rel=\"noopener noreferrer\"");
    }
}
