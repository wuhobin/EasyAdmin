package com.nexora.file.constants;

public final class FileConstants {

    public static final String FILE_EMPTY_MESSAGE = "上传文件不能为空";
    public static final String FILE_TOO_LARGE_MESSAGE = "上传文件大小超过限制";
    public static final String FILE_NAME_REQUIRED_MESSAGE = "上传文件名不能为空";
    public static final String FILE_NAME_TOO_LONG_MESSAGE = "上传文件名长度超过限制";
    public static final String FILE_EXTENSION_NOT_ALLOWED_MESSAGE = "不支持该文件类型";
    public static final String FILE_CONTENT_TYPE_MISMATCH_MESSAGE = "文件扩展名与实际内容类型不匹配";
    public static final String FILE_CONTENT_DETECTION_FAILED_MESSAGE = "无法读取上传文件内容";
    public static final String FILE_UPLOAD_FAILED_MESSAGE = "上传文件失败";
    public static final String FILE_CURRENT_USER_REQUIRED_MESSAGE = "无法获取当前登录用户";
    public static final String FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE = "文件不存在或无权访问";
    public static final String FILE_OSS_DELETE_FAILED_MESSAGE = "OSS 文件删除失败";
    public static final String FILE_AVATAR_IN_USE_MESSAGE = "该文件正在作为用户头像使用，请先更换头像后再删除";
    public static final String FILE_RECORD_DELETE_FAILED_MESSAGE = "文件记录删除失败, id=%s, fileId=%s";
    public static final String FILE_OBJECT_KEY_UNAVAILABLE_MESSAGE = "无法解析 OSS 文件对象名称";
    public static final String FILE_DEFAULT_ORDER = "create_time desc";

    private FileConstants() {
    }
}
