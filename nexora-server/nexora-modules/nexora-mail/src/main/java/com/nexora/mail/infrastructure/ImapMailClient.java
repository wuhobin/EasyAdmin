package com.nexora.mail.infrastructure;

import com.nexora.mail.domain.vo.MailMessageDetailVo;
import com.nexora.mail.domain.vo.MailMessageSummaryVo;
import com.nexora.mail.entity.MailAccount;
import com.nexora.mail.constants.MailProviderEnum;
import com.aurora.starter.webmvc.exception.BizException;
import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.eclipse.angus.mail.imap.IMAPStore;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class ImapMailClient {
    private final MailPartParser mailPartParser;
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 15_000;
    private static final Map<String, String> NETEASE_CLIENT_ID = Map.of(
            "name", "Nexora Admin",
            "version", "1.0",
            "vendor", "Nexora",
            "support-email", "support@nexora.local"
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
        return listPage(account, authCode, limit, null, 0).items();
    }

    public MailMessagePage listPage(MailAccount account, String authCode, int limit,
                                    Long anchorUid, int offset) {
        return withInbox(account, authCode, folder -> {
            int count = folder.getMessageCount();
            if (count == 0) {
                return new MailMessagePage(List.of(), 0, false);
            }

            UIDFolder uidFolder = uidFolder(folder);
            Message anchorMessage = anchorUid == null
                    ? folder.getMessage(count)
                    : uidFolder.getMessageByUID(anchorUid);
            if (anchorMessage == null) {
                throw new BizException("邮件列表已发生变化，请刷新后重试");
            }
            long resolvedAnchorUid = uidFolder.getUID(anchorMessage);
            int end = anchorMessage.getMessageNumber() - Math.max(offset, 0);
            if (end < 1) {
                return new MailMessagePage(List.of(), resolvedAnchorUid, false);
            }
            int start = Math.max(1, end - limit + 1);
            Message[] messages = folder.getMessages(start, end);
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            profile.add(UIDFolder.FetchProfileItem.UID);
            profile.add("Content-Type");
            folder.fetch(messages, profile);

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
                        .hasAttachment(MailPartParser.hasAttachmentHint(message))
                        .build());
            }
            result.sort(Comparator.comparing(MailMessageSummaryVo::getReceivedTime,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return new MailMessagePage(result, resolvedAnchorUid, start > 1);
        });
    }

    public MailMessageDetailVo getDetail(MailAccount account, String authCode, long uid, long uidValidity) {
        return withInbox(account, authCode, Folder.READ_ONLY,
                folder -> readDetail(account, folder, uid, uidValidity, false));
    }

    public MailMessageDetailVo openMessage(MailAccount account, String authCode, long uid, long uidValidity) {
        return withInbox(account, authCode, Folder.READ_WRITE,
                folder -> readDetail(account, folder, uid, uidValidity, true));
    }

    public void markRead(MailAccount account, String authCode, long uid, long uidValidity) {
        withInbox(account, authCode, Folder.READ_WRITE, folder -> {
            UIDFolder uidFolder = uidFolder(folder);
            validateUidValidity(uidFolder, uidValidity);
            Message message = uidFolder.getMessageByUID(uid);
            if (message == null) {
                throw new BizException("邮件不存在或已被邮箱服务器删除");
            }
            markAsRead(message);
            return null;
        });
    }

    private MailMessageDetailVo readDetail(MailAccount account, Folder folder, long uid,
                                           long uidValidity, boolean markRead) throws Exception {
        UIDFolder uidFolder = uidFolder(folder);
        validateUidValidity(uidFolder, uidValidity);
        Message message = uidFolder.getMessageByUID(uid);
        if (message == null) {
            throw new BizException("邮件不存在或已被邮箱服务器删除");
        }
        MailPartParser.ParsedBody parsed = mailPartParser.parse(message);
        Sender sender = sender(message.getFrom());
        String html = parsed.html == null ? null : HtmlSanitizer.sanitizeHtml(parsed.html, parsed.inlineImages);
        MailMessageDetailVo detail = MailMessageDetailVo.builder()
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
        if (markRead) {
            markAsRead(message);
        }
        return detail;
    }

    static void markAsRead(Message message) throws Exception {
        if (!message.isSet(Flags.Flag.SEEN)) {
            message.setFlag(Flags.Flag.SEEN, true);
        }
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
            Part part = mailPartParser.findPart(message, partId);
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
        return withInbox(account, authCode, Folder.READ_ONLY, callback);
    }

    private <T> T withInbox(MailAccount account, String authCode, int mode,
                            MailFolderCallback<T> callback) {
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
                inbox.open(mode);
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
        if (provider != MailProviderEnum.NETEASE_163
                && provider != MailProviderEnum.NETEASE_126
                && provider != MailProviderEnum.YEAH) {
            return;
        }
        if (!(store instanceof IMAPStore imapStore)) {
            throw new BizException("当前IMAP客户端不支持网易邮箱要求的ID命令");
        }
        imapStore.id(NETEASE_CLIENT_ID);
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
}
