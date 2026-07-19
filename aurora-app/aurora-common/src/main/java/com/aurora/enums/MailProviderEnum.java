package com.aurora.enums;

import lombok.Getter;

@Getter
public enum MailProviderEnum {
    QQ("QQ邮箱", "imap.qq.com", 993),
    NETEASE_163("163邮箱", "imap.163.com", 993),
    NETEASE_126("126邮箱", "imap.126.com", 993),
    YEAH("yeah邮箱", "imap.yeah.net", 993);

    private final String description;
    private final String host;
    private final int port;

    MailProviderEnum(String description, String host, int port) {
        this.description = description;
        this.host = host;
        this.port = port;
    }
}
