package com.nexora.file.constants;

import java.util.Map;

public final class FileConstants {

    public static final String FILE_EMPTY_MESSAGE = "上传文件不能为空";
    public static final long FILE_UPLOAD_MAX_SIZE = 50L * 1024 * 1024;
    public static final int FILE_ORIGINAL_FILENAME_MAX_LENGTH = 255;
    public static final String FILE_TOO_LARGE_MESSAGE = "上传文件大小不能超过 50MB";
    public static final String FILE_NAME_REQUIRED_MESSAGE = "上传文件名不能为空";
    public static final String FILE_NAME_TOO_LONG_MESSAGE = "上传文件名长度不能超过 255 个字符";
    public static final String FILE_EXTENSION_NOT_ALLOWED_MESSAGE =
            "仅支持 JPG、JPEG、PNG、GIF、WEBP、MP4、PDF、ZIP、TXT 格式";
    public static final String FILE_CONTENT_TYPE_MISMATCH_MESSAGE = "文件扩展名与实际内容类型不匹配";
    public static final String FILE_CONTENT_DETECTION_FAILED_MESSAGE = "无法读取上传文件内容";
    public static final String FILE_MP4_CONTENT_TYPE = "video/mp4";
    public static final Map<String, String> FILE_ALLOWED_CONTENT_TYPE_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("mp4", FILE_MP4_CONTENT_TYPE),
            Map.entry("pdf", "application/pdf"),
            Map.entry("zip", "application/zip"),
            Map.entry("txt", "text/plain")
    );
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
