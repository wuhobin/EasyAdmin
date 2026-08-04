package com.nexora.system.constants;

public final class SystemConfigConstants {

    public static final String CONFIG_GROUP_UNSUPPORTED_MESSAGE = "不支持的配置分组";
    public static final String CONFIG_GROUP_VALUE_REQUIRED_MESSAGE = "配置值不能为空";
    public static final String CONFIG_GROUP_JSON_INVALID_MESSAGE = "配置 JSON 格式或字段不正确";
    public static final String CONFIG_GROUP_JSON_OBJECT_REQUIRED_MESSAGE = "配置值必须是 JSON 对象";
    public static final String CONFIG_GROUP_TYPE_MISMATCH_MESSAGE = "配置类型不匹配";
    public static final String CONFIG_GROUP_INVALID_MESSAGE = "配置分组 %s 无效：%s";
    public static final String CONFIG_GROUP_MISSING_MESSAGE = "缺少配置分组：%s";
    public static final String CONFIG_GROUP_STRUCTURE_INVALID_MESSAGE =
            "系统配置分组结构不完整，缺失=%s，不支持=%s";
    public static final String CONFIG_GROUP_NAME_MISMATCH_MESSAGE =
            "配置分组 %s 名称不一致，数据库=%s，预期=%s";
    public static final String CONFIG_GROUP_UPDATE_FAILED_MESSAGE = "修改配置分组失败";
    public static final String CONFIG_GROUP_CACHE_UNAVAILABLE_MESSAGE =
            "Redis 不可用，无法安全更新配置缓存";

    private SystemConfigConstants() {
    }
}
