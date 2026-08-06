package com.nexora.mail.constants;

public final class MailConstants {

    public static final String SYSTEM_MAIL_DISABLED_MESSAGE = "邮件服务未启用";
    public static final String TEST_EMAIL_SEND_FAILED_MESSAGE = "测试邮件发送失败：%s";
    public static final String MAIL_CREDENTIAL_PURPOSE = "mail.auth-code";
    public static final String MAIL_PROVIDER_DICT_TYPE = "mail_provider";
    public static final String MAIL_ACCOUNT_AUTH_CODE_REQUIRED_MESSAGE = "新增邮箱时授权码不能为空";
    public static final String MAIL_ACCOUNT_ID_REQUIRED_MESSAGE = "邮箱账户ID不能为空";
    public static final String MAIL_ACCOUNT_EXISTS_MESSAGE = "该邮箱已经添加";
    public static final String MAIL_ACCOUNT_UNAVAILABLE_MESSAGE = "邮箱账户不存在或不可用";
    public static final String MAIL_PROVIDER_NOT_CONFIGURED_MESSAGE = "未配置邮箱类型字典 mail_provider";
    public static final String MAIL_PROVIDER_EMPTY_MESSAGE = "邮箱类型字典没有可用数据";
    public static final String MAIL_ADDRESS_DOMAIN_REQUIRED_MESSAGE = "%s地址必须以 @%s 结尾";
    public static final String MAIL_CURSOR_INVALID_MESSAGE = "邮件分页游标无效，请刷新后重试";
    public static final String MAIL_CURSOR_CREATE_FAILED_MESSAGE = "邮件分页游标生成失败";

    private MailConstants() {
    }
}
