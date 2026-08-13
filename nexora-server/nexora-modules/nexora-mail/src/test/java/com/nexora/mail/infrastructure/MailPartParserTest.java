package com.nexora.mail.infrastructure;

import jakarta.mail.Part;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailPartParserTest {

    @Test
    void skipsLargeInlineImagesWithoutDownloadingTheirContent() throws Exception {
        Part part = mock(Part.class);
        when(part.isMimeType("multipart/*")).thenReturn(false);
        when(part.getDisposition()).thenReturn(Part.INLINE);
        when(part.getHeader("Content-ID")).thenReturn(new String[]{"<large-image>"});
        when(part.isMimeType("image/*")).thenReturn(true);
        when(part.getSize()).thenReturn(3 * 1024 * 1024);

        MailPartParser.ParsedBody parsed = new MailPartParser().parse(part);

        assertThat(parsed.inlineImages).isEmpty();
        assertThat(parsed.attachments).singleElement()
                .satisfies(attachment -> assertThat(attachment.getFileName()).isEqualTo("inline-image"));
        verify(part, never()).getInputStream();
    }
}
