package com.nexora.file.constants;

public final class FileConstants {

    public static final int FILE_NAME_DATABASE_MAX_LENGTH = 255;
    public static final int FILE_TEXT_PREVIEW_MAX_SIZE_BYTES = 5 * 1024 * 1024;

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
    public static final String FILE_RECORD_UPDATE_FAILED_MESSAGE = "文件记录更新失败, id=%s, fileId=%s";
    public static final String FILE_OBJECT_KEY_UNAVAILABLE_MESSAGE = "无法解析 OSS 文件对象名称";
    public static final String FILE_DEFAULT_ORDER = "create_time desc";
    public static final String FILE_GROUP_REQUIRED_MESSAGE = "请选择文件分组范围";
    public static final String FILE_GROUP_NOT_FOUND_MESSAGE = "文件分组不存在或无权访问";
    public static final String FILE_GROUP_NAME_REQUIRED_MESSAGE = "分组名称不能为空";
    public static final String FILE_GROUP_NAME_TOO_LONG_MESSAGE = "分组名称不能超过50个字符";
    public static final String FILE_GROUP_NAME_EXISTS_MESSAGE = "分组名称已存在";
    public static final String FILE_GROUP_CREATE_FAILED_MESSAGE = "文件分组创建失败";
    public static final String FILE_GROUP_UPDATE_FAILED_MESSAGE = "文件分组更新失败";
    public static final String FILE_FILE_IDS_REQUIRED_MESSAGE = "请选择至少一个文件";
    public static final String FILE_BATCH_DELETE_FAILED_MESSAGE = "部分文件删除失败，失败文件ID：%s";
    public static final String FILE_BATCH_MOVE_FAILED_MESSAGE = "部分文件移动失败，请刷新后重试";
    public static final String FILE_RENAME_REQUIRED_MESSAGE = "文件名不能为空";
    public static final String FILE_RENAME_TOO_LONG_MESSAGE = "文件名长度超过限制";
    public static final String FILE_RENAME_INVALID_MESSAGE = "文件名包含非法字符";
    public static final String FILE_RENAME_EXTENSION_MESSAGE = "不能修改文件扩展名";
    public static final String FILE_TEXT_PREVIEW_UNSUPPORTED_MESSAGE = "仅支持文本或代码文件预览";
    public static final String FILE_TEXT_PREVIEW_TOO_LARGE_MESSAGE = "文本预览文件不能超过5MB";
    public static final String FILE_PREVIEW_UNSUPPORTED_MESSAGE = "该文件类型不支持在线预览";

    private FileConstants() {
    }
}
