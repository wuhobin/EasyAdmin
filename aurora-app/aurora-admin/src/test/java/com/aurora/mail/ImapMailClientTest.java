package com.aurora.mail;

import com.aurora.enums.MailProviderEnum;
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
