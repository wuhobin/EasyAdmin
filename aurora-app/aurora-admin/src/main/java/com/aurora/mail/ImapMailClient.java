package com.aurora.mail;

import com.aurora.domain.vo.mail.MailAttachmentVo;
import com.aurora.domain.vo.mail.MailMessageDetailVo;
import com.aurora.domain.vo.mail.MailMessageSummaryVo;
import com.aurora.entity.MailAccount;
import com.aurora.enums.MailProviderEnum;
import com.aurora.starter.webmvc.exception.BizException;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Component
public class ImapMailClient {
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 15_000;
    private static final int MAX_INLINE_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Map<String, String> NETEASE_CLIENT_ID = Map.of(
            "name", "EasyAdmin",
            "version", "1.0",
            "vendor", "Aurora",
            "support-email", "support@easyadmin.local"
    );

    public void testConnection(MailAccount account, String authCode) {
        withInbox(account, authCode, folder -> null);
    }

    public MailCursor latestCursor(MailAccount account, String authCode) {
        return withInbox(account, authCode, folder -> {
            UIDFolder uidFolder = uidFolder(folder);
            int count = folder.getMessageCount();
            long latestUid = count == 0 ? 0 : uidFolder.getUID(folder.getMessage(count));
            return new MailCursor(uidFolder.getUIDValidity(), latestUid);
        });
    }

    public List<MailMessageSummaryVo> listLatest(MailAccount account, String authCode, int limit) {
        return withInbox(account, authCode, folder -> {
            int count = folder.getMessageCount();
            if (count == 0) {
                return List.of();
            }
            int start = Math.max(1, count - limit + 1);
            Message[] messages = folder.getMessages(start, count);
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            profile.add(FetchProfile.Item.CONTENT_INFO);
            profile.add(UIDFolder.FetchProfileItem.UID);
            folder.fetch(messages, profile);

            UIDFolder uidFolder = uidFolder(folder);
            long uidValidity = uidFolder.getUIDValidity();
            List<MailMessageSummaryVo> result = new ArrayList<>(messages.length);
            for (Message message : messages) {
                Sender sender = sender(message.getFrom());
                result.add(MailMessageSummaryVo.builder()
                        .accountId(account.getId())
                        .accountName(account.getAccountName())
                        .provider(account.getProvider())
                        .uid(uidFolder.getUID(message))
                        .uidValidity(uidValidity)
                        .fromName(sender.name())
                        .fromAddress(sender.address())
                        .subject(defaultSubject(message.getSubject()))
                        .receivedTime(toLocalDateTime(message.getReceivedDate()))
                        .read(message.isSet(Flags.Flag.SEEN))
                        .hasAttachment(hasAttachmentHint(message))
                        .build());
            }
            result.sort(Comparator.comparing(MailMessageSummaryVo::getReceivedTime,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return result;
        });
    }

    public MailMessageDetailVo getDetail(MailAccount account, String authCode, long uid, long uidValidity) {
        return withInbox(account, authCode, folder -> {
            UIDFolder uidFolder = uidFolder(folder);
            validateUidValidity(uidFolder, uidValidity);
            Message message = uidFolder.getMessageByUID(uid);
            if (message == null) {
                throw new BizException("邮件不存在或已被邮箱服务器删除");
            }
            ParsedBody parsed = new ParsedBody();
            parsePart(message, "", parsed);
            Sender sender = sender(message.getFrom());
            String html = parsed.html == null ? null : sanitizeHtml(parsed.html, parsed.inlineImages);
            return MailMessageDetailVo.builder()
                    .accountId(account.getId())
                    .uid(uid)
                    .uidValidity(uidValidity)
                    .fromName(sender.name())
                    .fromAddress(sender.address())
                    .recipients(addresses(message.getAllRecipients()))
                    .subject(defaultSubject(message.getSubject()))
                    .receivedTime(toLocalDateTime(message.getReceivedDate()))
                    .bodyHtml(html)
                    .bodyText(parsed.text == null ? "" : parsed.text.trim())
                    .attachments(parsed.attachments)
                    .build();
        });
    }

    public void downloadAttachment(MailAccount account, String authCode, long uid, long uidValidity,
                                   String partId, HttpServletResponse response) {
        withInbox(account, authCode, folder -> {
            UIDFolder uidFolder = uidFolder(folder);
            validateUidValidity(uidFolder, uidValidity);
            Message message = uidFolder.getMessageByUID(uid);
            if (message == null) {
                throw new BizException("邮件不存在或已被邮箱服务器删除");
            }
            Part part = findPart(message, partId);
            String filename = decodedFilename(part.getFileName());
            if (filename == null || filename.isBlank()) {
                filename = "attachment";
            }
            response.setContentType(baseContentType(part.getContentType()));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8).build().toString());
            if (part.getSize() > 0) {
                response.setContentLengthLong(part.getSize());
            }
            try (InputStream input = part.getInputStream()) {
                input.transferTo(response.getOutputStream());
            }
            return null;
        });
    }

    private <T> T withInbox(MailAccount account, String authCode, MailFolderCallback<T> callback) {
        MailProviderEnum provider = provider(account);
        Properties properties = new Properties();
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.connectiontimeout", String.valueOf(CONNECT_TIMEOUT));
        properties.put("mail.imaps.timeout", String.valueOf(READ_TIMEOUT));
        properties.put("mail.imaps.writetimeout", String.valueOf(READ_TIMEOUT));
        properties.put("mail.imaps.peek", "true");
        Session session = Session.getInstance(properties);
        try (Store store = session.getStore("imaps")) {
            store.connect(provider.getHost(), provider.getPort(), account.getEmail(), authCode);
            identifyClient(store, provider);
            Folder inbox = store.getFolder("INBOX");
            try {
                inbox.open(Folder.READ_ONLY);
                return callback.apply(inbox);
            } finally {
                if (inbox.isOpen()) {
                    inbox.close(false);
                }
            }
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException("邮箱连接或读取失败：" + safeMessage(exception));
        }
    }

    static void identifyClient(Store store, MailProviderEnum provider) throws Exception {
        if (provider == MailProviderEnum.QQ) {
            return;
        }
        if (!(store instanceof IMAPStore imapStore)) {
            throw new BizException("当前IMAP客户端不支持网易邮箱要求的ID命令");
        }
        imapStore.id(NETEASE_CLIENT_ID);
    }

    private static void parsePart(Part part, String path, ParsedBody parsed) throws Exception {
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
            byte[] bytes = readLimited(part.getInputStream(), MAX_INLINE_IMAGE_BYTES);
            if (bytes != null) {
                String normalizedCid = contentId.replace("<", "").replace(">", "").trim();
                parsed.inlineImages.put(normalizedCid, "data:" + baseContentType(part.getContentType())
                        + ";base64," + Base64.getEncoder().encodeToString(bytes));
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

    private static Part findPart(Part root, String partId) throws Exception {
        if (partId == null || !partId.matches("[1-9]\\d*(\\.[1-9]\\d*)*")) {
            throw new BizException("附件标识不正确");
        }
        Part current = root;
        for (String segment : partId.split("\\.")) {
            Object content = current.getContent();
            if (!(content instanceof Multipart multipart)) {
                throw new BizException("附件不存在");
            }
            int index = Integer.parseInt(segment) - 1;
            if (index < 0 || index >= multipart.getCount()) {
                throw new BizException("附件不存在");
            }
            current = multipart.getBodyPart(index);
        }
        if (current.isMimeType("multipart/*")) {
            throw new BizException("附件不存在");
        }
        return current;
    }

    static String sanitizeHtml(String html, Map<String, String> inlineImages) {
        Document source = Jsoup.parse(html);
        source.select("script,iframe,object,embed,form,input,button,meta,base,link").remove();
        for (Element element : source.getAllElements()) {
            List<Attribute> attributes = new ArrayList<>(element.attributes().asList());
            for (Attribute attribute : attributes) {
                String key = attribute.getKey().toLowerCase(Locale.ROOT);
                String value = attribute.getValue().trim();
                if (key.startsWith("on") || ("style".equals(key)
                        && (value.toLowerCase(Locale.ROOT).contains("url(")
                        || value.toLowerCase(Locale.ROOT).contains("expression(")))) {
                    element.removeAttr(attribute.getKey());
                }
            }
        }
        for (Element image : source.select("img[src]")) {
            String src = image.attr("src").trim();
            if (src.toLowerCase(Locale.ROOT).startsWith("cid:")) {
                String data = inlineImages.get(src.substring(4));
                if (data == null) {
                    image.removeAttr("src");
                } else {
                    image.attr("src", data);
                }
            } else if (src.startsWith("http://") || src.startsWith("https://")) {
                image.removeAttr("src");
                image.attr("title", "外部图片已阻止加载");
            }
        }
        Safelist safelist = Safelist.relaxed()
                .addAttributes(":all", "style", "class", "align")
                .addAttributes("img", "width", "height", "alt", "title")
                .addProtocols("img", "src", "data")
                .addProtocols("a", "href", "http", "https", "mailto");
        Document cleaned = new Cleaner(safelist).clean(source);
        cleaned.outputSettings().prettyPrint(false);
        return cleaned.body().html();
    }

    private static byte[] readLimited(InputStream input, int maxBytes) throws IOException {
        try (input) {
            byte[] value = input.readNBytes(maxBytes + 1);
            return value.length > maxBytes ? null : value;
        }
    }

    private static boolean hasAttachmentHint(Message message) throws Exception {
        String contentType = message.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/mixed");
    }

    private static Sender sender(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return new Sender("未知发件人", "");
        }
        Address first = addresses[0];
        if (first instanceof InternetAddress internetAddress) {
            String address = internetAddress.getAddress() == null ? "" : internetAddress.getAddress();
            String name = internetAddress.getPersonal();
            return new Sender(name == null || name.isBlank() ? address : name, address);
        }
        return new Sender(first.toString(), first.toString());
    }

    private static List<String> addresses(Address[] addresses) {
        if (addresses == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>(addresses.length);
        for (Address address : addresses) {
            result.add(address.toString());
        }
        return result;
    }

    private static String firstHeader(Part part, String name) throws Exception {
        String[] values = part.getHeader(name);
        return values == null || values.length == 0 ? null : values[0];
    }

    private static String decodedFilename(String filename) {
        if (filename == null) {
            return null;
        }
        try {
            return MimeUtility.decodeText(filename);
        } catch (Exception exception) {
            return filename;
        }
    }

    private static String baseContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        int separator = contentType.indexOf(';');
        return separator < 0 ? contentType : contentType.substring(0, separator).trim();
    }

    private static String defaultSubject(String subject) {
        return subject == null || subject.isBlank() ? "（无主题）" : subject;
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static UIDFolder uidFolder(Folder folder) {
        if (!(folder instanceof UIDFolder uidFolder)) {
            throw new BizException("当前邮箱服务器不支持 UID 增量读取");
        }
        return uidFolder;
    }

    private static void validateUidValidity(UIDFolder folder, long expected) throws Exception {
        if (expected > 0 && folder.getUIDValidity() != expected) {
            throw new BizException("邮箱目录已更新，请刷新邮件列表后重试");
        }
    }

    private static MailProviderEnum provider(MailAccount account) {
        try {
            return MailProviderEnum.valueOf(account.getProvider());
        } catch (Exception exception) {
            throw new BizException("不支持的邮箱类型");
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    @FunctionalInterface
    private interface MailFolderCallback<T> {
        T apply(Folder folder) throws Exception;
    }

    private record Sender(String name, String address) {
    }

    private static final class ParsedBody {
        private String html;
        private String text;
        private final List<MailAttachmentVo> attachments = new ArrayList<>();
        private final Map<String, String> inlineImages = new HashMap<>();
    }
}
