package com.nexora.mail.infrastructure;

import com.nexora.mail.domain.vo.MailAttachmentVo;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MIME 邮件 Part 解析器。
 * 递归遍历 MIME 树提取 HTML、纯文本、内联图片和附件。
 */
@Component
public class MailPartParser {

    private static final int MAX_INLINE_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_INLINE_IMAGE_TOTAL_BYTES = 10 * 1024 * 1024;

    /**
     * 递归解析邮件 Part 树，提取 body 内容和附件信息。
     *
     * @param rootPart 邮件根 Part
     * @return 解析结果
     */
    public ParsedBody parse(Part rootPart) throws Exception {
        ParsedBody body = new ParsedBody();
        parsePart(rootPart, "", body);
        return body;
    }

    private void parsePart(Part part, String path, ParsedBody parsed) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                String childPath = path.isEmpty() ? String.valueOf(index + 1) : path + "." + (index + 1);
                parsePart(multipart.getBodyPart(index), childPath, parsed);
            }
            return;
        }

        String disposition = part.getDisposition();
        String fileName = decodedFilename(part.getFileName());
        String contentId = firstHeader(part, "Content-ID");
        boolean inline = Part.INLINE.equalsIgnoreCase(disposition) || contentId != null;
        if (inline && part.isMimeType("image/*") && contentId != null) {
            int remaining = MAX_INLINE_IMAGE_TOTAL_BYTES - parsed.inlineImageBytes;
            byte[] bytes = remaining <= 0 ? null
                    : readLimited(part.getInputStream(), Math.min(MAX_INLINE_IMAGE_BYTES, remaining));
            if (bytes != null) {
                parsed.inlineImageBytes += bytes.length;
                String normalizedCid = contentId.replace("<", "").replace(">", "").trim();
                parsed.inlineImages.put(normalizedCid, "data:" + baseContentType(part.getContentType())
                        + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes));
            }
            return;
        }

        if (Part.ATTACHMENT.equalsIgnoreCase(disposition) || fileName != null) {
            parsed.attachments.add(MailAttachmentVo.builder()
                    .partId(path)
                    .fileName(fileName == null ? "attachment" : fileName)
                    .contentType(baseContentType(part.getContentType()))
                    .size(Math.max(part.getSize(), 0))
                    .build());
            return;
        }

        if (part.isMimeType("text/html") && parsed.html == null) {
            parsed.html = String.valueOf(part.getContent());
        } else if (part.isMimeType("text/plain")) {
            String value = String.valueOf(part.getContent());
            parsed.text = parsed.text == null ? value : parsed.text + "\n" + value;
        }
    }

    /**
     * 沿 MIME Part 路径定位指定附件。
     *
     * @param root   邮件根 Part
     * @param partId 点号分隔的 Part 路径（如 "1.2"）
     * @return 目标 Part
     */
    public Part findPart(Part root, String partId) throws Exception {
        if (partId == null || !partId.matches("[1-9]\\d*(\\.[1-9]\\d*)*")) {
            throw new com.aurora.starter.webmvc.exception.BizException("附件标识不正确");
        }
        Part current = root;
        for (String segment : partId.split("\\.")) {
            Object content = current.getContent();
            if (!(content instanceof Multipart multipart)) {
                throw new com.aurora.starter.webmvc.exception.BizException("附件不存在");
            }
            int index = Integer.parseInt(segment) - 1;
            if (index < 0 || index >= multipart.getCount()) {
                throw new com.aurora.starter.webmvc.exception.BizException("附件不存在");
            }
            current = multipart.getBodyPart(index);
        }
        if (current.isMimeType("multipart/*")) {
            throw new com.aurora.starter.webmvc.exception.BizException("附件不存在");
        }
        return current;
    }

    /**
     * 判断消息是否可能包含附件（基于 Content-Type 提示，不精确）。
     */
    public static boolean hasAttachmentHint(Message message) throws Exception {
        String contentType = message.getContentType();
        return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/mixed");
    }

    // ---- internal helpers (mirrored from ImapMailClient) ----

    static String firstHeader(Part part, String name) throws Exception {
        String[] values = part.getHeader(name);
        return values == null || values.length == 0 ? null : values[0];
    }

    static String decodedFilename(String filename) {
        if (filename == null) {
            return null;
        }
        try {
            return jakarta.mail.internet.MimeUtility.decodeText(filename);
        } catch (Exception exception) {
            return filename;
        }
    }

    static String baseContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        int separator = contentType.indexOf(';');
        return separator < 0 ? contentType : contentType.substring(0, separator).trim();
    }

    private static byte[] readLimited(java.io.InputStream input, int maxBytes) throws java.io.IOException {
        try (input) {
            byte[] value = input.readNBytes(maxBytes + 1);
            return value.length > maxBytes ? null : value;
        }
    }

    /**
     * 解析结果：包含 HTML、纯文本、附件列表和内联图片映射。
     */
    public static final class ParsedBody {
        String html;
        String text;
        final List<MailAttachmentVo> attachments = new ArrayList<>();
        final Map<String, String> inlineImages = new HashMap<>();
        int inlineImageBytes;
    }
}
