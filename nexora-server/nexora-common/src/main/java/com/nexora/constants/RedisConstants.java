package com.nexora.constants;

/**
 * Redis常量
 */
public class RedisConstants {

    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN = "login:token";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_code";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit";

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
     * 验证码有效期（分钟）
     */
    public static final Integer CAPTCHA_EXPIRATION = 1;

}
