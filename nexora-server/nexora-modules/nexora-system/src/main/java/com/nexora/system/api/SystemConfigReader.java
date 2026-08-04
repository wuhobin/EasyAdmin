package com.nexora.system.api;

/**
 * 对外提供类型安全的系统配置读取能力。
 */
public interface SystemConfigReader {

    SystemSettings system();

    RegistrationSettings register();

    LoginSettings login();

    PasswordSettings password();

    EmailSettings email();
}
