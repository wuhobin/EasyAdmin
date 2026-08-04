package com.nexora.file.infrastructure;

import com.nexora.file.constants.FileConstants;
import com.aurora.starter.webmvc.exception.BizException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 文件上传校验器，包含扩展名白名单校验和 Tika 内容类型检测。
 */
@Component
public class FileUploadValidator {

    private static final Tika TIKA = new Tika();

    /**
     * 校验上传文件并返回检测到的 Content-Type。
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(FileConstants.FILE_EMPTY_MESSAGE);
        }
        if (file.getSize() > FileConstants.FILE_UPLOAD_MAX_SIZE) {
            throw new BizException(FileConstants.FILE_TOO_LARGE_MESSAGE);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BizException(FileConstants.FILE_NAME_REQUIRED_MESSAGE);
        }
        if (originalFilename.codePointCount(0, originalFilename.length())
                > FileConstants.FILE_ORIGINAL_FILENAME_MAX_LENGTH) {
            throw new BizException(FileConstants.FILE_NAME_TOO_LONG_MESSAGE);
        }
        String expectedContentType = expectedContentType(originalFilename);
        String detectedContentType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedContentType = TIKA.detect(inputStream);
        } catch (IOException exception) {
            throw new BizException(FileConstants.FILE_CONTENT_DETECTION_FAILED_MESSAGE);
        }
        if (!expectedContentType.equals(detectedContentType)) {
            throw new BizException(FileConstants.FILE_CONTENT_TYPE_MISMATCH_MESSAGE);
        }
        return detectedContentType;
    }

    static String expectedContentType(String originalFilename) {
        int lastSeparator = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        int extensionSeparator = originalFilename.lastIndexOf('.');
        if (extensionSeparator <= lastSeparator || extensionSeparator == originalFilename.length() - 1) {
            throw new BizException(FileConstants.FILE_EXTENSION_NOT_ALLOWED_MESSAGE);
        }
        String extension = originalFilename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        String contentType = FileConstants.FILE_ALLOWED_CONTENT_TYPE_BY_EXTENSION.get(extension);
        if (contentType == null) {
            throw new BizException(FileConstants.FILE_EXTENSION_NOT_ALLOWED_MESSAGE);
        }
        return contentType;
    }
}
