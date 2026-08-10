package com.nexora.monitor.constants;

public final class ServerConstants {

    public static final String SSH_PASSWORD_CREDENTIAL_PURPOSE = "monitor.ssh-password";
    public static final String SERVER_UNAVAILABLE_MESSAGE = "服务器不存在或不可用";
    public static final String SERVER_DISABLED_MESSAGE = "服务器已停用";
    public static final String SSH_PASSWORD_REQUIRED_MESSAGE = "请输入 SSH 密码";
    public static final String SSH_TARGET_FORBIDDEN_MESSAGE = "该服务器地址不允许连接";
    public static final String SSH_TARGET_RESOLVE_FAILED_MESSAGE = "无法解析服务器地址";
    public static final String SSH_CONNECTION_FAILED_MESSAGE = "SSH 连接失败，请检查地址、端口和密码";
    public static final String SSH_HOST_KEY_REQUIRED_MESSAGE = "请先测试并确认主机指纹";
    public static final String SSH_HOST_KEY_CHANGED_MESSAGE = "主机指纹已变化，连接已阻止";
    public static final String SSH_HOST_KEY_CONFIRMATION_FAILED_MESSAGE =
            "主机指纹与当前服务器不一致，请重新测试";
    public static final String SSH_TERMINAL_LIMIT_MESSAGE = "最多同时打开 3 个 SSH 终端";
    public static final String SSH_TICKET_INVALID_MESSAGE = "终端票据无效或已过期";
    public static final int DEFAULT_TERMINAL_COLUMNS = 80;
    public static final int DEFAULT_TERMINAL_ROWS = 24;

    public static final String TEST_STATUS_SUCCESS = "SUCCESS";
    public static final String TEST_STATUS_CONFIRM_REQUIRED = "CONFIRM_REQUIRED";
    public static final String TEST_STATUS_FINGERPRINT_MISMATCH = "FINGERPRINT_MISMATCH";

    private ServerConstants() {
    }
}
