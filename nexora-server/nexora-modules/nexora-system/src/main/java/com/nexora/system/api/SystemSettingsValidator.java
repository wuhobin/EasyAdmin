package com.nexora.system.api;

/**
 * 配置分组的领域业务校验扩展点。
 */
public interface SystemSettingsValidator {

    /**
     * 当前校验器是否处理指定配置分组。
     *
     * @param groupCode 配置分组编码
     * @return 是否支持
     */
    boolean supports(String groupCode);

    /**
     * 校验已经通过结构校验的配置对象。
     *
     * @param config 配置对象
     */
    void validate(Object config);
}
