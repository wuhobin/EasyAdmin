package com.nexora.mail.constants;

import lombok.Getter;

@Getter
public enum MailProviderEnum {
    QQ("QQ邮箱", "qq.com", "imap.qq.com", 993),
    NETEASE_163("163邮箱", "163.com", "imap.163.com", 993),
    NETEASE_126("126邮箱", "126.com", "imap.126.com", 993),
    YEAH("yeah邮箱", "yeah.net", "imap.yeah.net", 993);

    private final String description;
    private final String emailDomain;
    private final String host;
    private final int port;

    MailProviderEnum(String description, String emailDomain, String host, int port) {
        this.description = description;
        this.emailDomain = emailDomain;
        this.host = host;
        this.port = port;
    }

    public boolean matchesEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.trim().toLowerCase();
        return normalized.endsWith("@" + emailDomain) && normalized.length() > emailDomain.length() + 1;
    }
}
