package com.nexora.constants;

/**
 * Redis常量
 */
public class RedisConstants {
    /**
     * 系统配置 redis key
     */
    public static final String SYS_CONFIG_GROUP_KEY = "nexora:sys-config-group";

    /**
     * 登录失败重试 redis key
     */
    public static final String LOGIN_RETRY_KEY = "nexora:login-retry";

    /**
     * 用户授权信息 redis key
     */
    public static final String SECURITY_AUTHORIZATION_KEY = "nexora:security:authorization";

    /**
     * 在线会话数据 redis key
     */
    public static final String ONLINE_SESSION_DATA_KEY = "nexora:online-session:data";

    /**
     * 在线会话最后访问时间 redis key
     */
    public static final String ONLINE_SESSION_LAST_ACCESS_KEY = "nexora:online-session:last-access";

    /**
     * 在线会话访问时间写入节流 redis key
     */
    public static final String ONLINE_SESSION_TOUCH_KEY = "nexora:online-session:touch";

    /**
     * 在线会话全局索引 redis key
     */
    public static final String ONLINE_SESSION_INDEX_KEY = "nexora:online-session:index";

    /**
     * 用户在线会话索引 redis key
     */
    public static final String ONLINE_SESSION_USER_KEY = "nexora:online-session:user";

    /**
     * 在线会话访问时间写入间隔（秒）
     */
    public static final long ONLINE_SESSION_TOUCH_INTERVAL_SECONDS = 60L;

    /**
     * 验证码有效期（分钟）
     */
    public static final Integer CAPTCHA_EXPIRATION = 1;

}
