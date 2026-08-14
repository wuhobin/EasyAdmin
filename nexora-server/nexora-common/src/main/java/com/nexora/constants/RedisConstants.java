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

    /** 微信登录六码与事务映射 redis key。 */
    public static final String WECHAT_LOGIN_CODE_KEY = "nexora:wechat-login:code";

    /** 微信登录待处理事务 redis key。 */
    public static final String WECHAT_LOGIN_TRANSACTION_KEY = "nexora:wechat-login:transaction";

    /** 微信登录短期结果 redis key。 */
    public static final String WECHAT_LOGIN_RESULT_KEY = "nexora:wechat-login:result";

    /** 微信登录结果一次性消费 redis key。 */
    public static final String WECHAT_LOGIN_CONSUMED_KEY = "nexora:wechat-login:consumed";

    /** 微信登录六码有效期（秒）。 */
    public static final long WECHAT_LOGIN_CODE_TTL_SECONDS = 300L;

    /** 微信登录结果保留时间（秒）。 */
    public static final long WECHAT_LOGIN_RESULT_TTL_SECONDS = 60L;

    /**
     * 用户授权信息 redis key
     */
    public static final String SECURITY_PERMISSION_LIST_KEY = "nexora:user:permission-list";

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

}
