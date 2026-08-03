package com.nexora.system.api;

/**
 * 系统配置领域使用的邮件发送契约。
 */
public interface SystemMailSender {

    /**
     * 使用当前系统邮箱配置发送测试邮件。
     *
     * @param to 收件人邮箱
     */
    void sendTestMail(String to);
}
