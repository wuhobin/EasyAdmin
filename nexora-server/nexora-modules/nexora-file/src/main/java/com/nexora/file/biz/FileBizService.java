package com.nexora.file.biz;

import com.nexora.constants.SecurityConstants;
import com.nexora.file.constants.FileConstants;
import com.nexora.contract.StoredFileUsageChecker;
import com.nexora.file.domain.convert.OssFileConvert;
import com.nexora.file.domain.form.OssFileQueryForm;
import com.nexora.file.domain.form.FileBatchForm;
import com.nexora.file.domain.form.FileMoveForm;
import com.nexora.file.domain.form.FileRenameForm;
import com.nexora.file.domain.query.OssFileQuery;
import com.nexora.file.domain.vo.SysOssFileVo;
import com.nexora.file.entity.SysOssFile;
import com.nexora.file.service.SysOssFileService;
import com.aurora.starter.common.utils.DateUtils;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.oss.config.FileUploadValidationProperties;
import com.aurora.starter.oss.exception.FileValidationException;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.oss.validation.FileUploadValidator;
import com.aurora.starter.oss.validation.ValidatedMultipartFile;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.file.task.OssFileRecordRetryTask;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FileBizService {

    private final FileUploadValidator fileUploadValidator;
    private final OssTemplate ossTemplate;
    private final SysOssFileService ossFileService;
    private final StoredFileUsageChecker storedFileUsageChecker;
    private final OssFileRecordRetryTask retryTask;
    private final FileGroupBizService fileGroupBizService;
    private final FileUploadValidationProperties fileUploadValidationProperties;

    @Autowired
    public FileBizService(FileUploadValidator fileUploadValidator, OssTemplate ossTemplate,
                          SysOssFileService ossFileService, StoredFileUsageChecker storedFileUsageChecker,
                          OssFileRecordRetryTask retryTask, FileGroupBizService fileGroupBizService,
                          FileUploadValidationProperties fileUploadValidationProperties) {
        this.fileUploadValidator = fileUploadValidator;
        this.ossTemplate = ossTemplate;
        this.ossFileService = ossFileService;
        this.storedFileUsageChecker = storedFileUsageChecker;
        this.retryTask = retryTask;
        this.fileGroupBizService = fileGroupBizService;
        this.fileUploadValidationProperties = fileUploadValidationProperties;
    }

    public FileBizService(FileUploadValidator fileUploadValidator, OssTemplate ossTemplate,
                          SysOssFileService ossFileService, StoredFileUsageChecker storedFileUsageChecker,
                          OssFileRecordRetryTask retryTask, FileGroupBizService fileGroupBizService) {
        this(fileUploadValidator, ossTemplate, ossFileService, storedFileUsageChecker, retryTask,
                fileGroupBizService, new FileUploadValidationProperties());
    }

    public FileBizService(FileUploadValidator fileUploadValidator, OssTemplate ossTemplate,
                          SysOssFileService ossFileService, StoredFileUsageChecker storedFileUsageChecker,
                          OssFileRecordRetryTask retryTask) {
        this(fileUploadValidator, ossTemplate, ossFileService, storedFileUsageChecker, retryTask,
                null, new FileUploadValidationProperties());
    }

    public String upload(MultipartFile file) {
        return upload(file, null);
    }

    public String upload(MultipartFile file, Long groupId) {
        String detectedContentType = validateUpload(file);
        MultipartFile validatedFile = new ValidatedMultipartFile(file, detectedContentType);
        Long uploaderId = currentUploaderId();
        validateGroup(groupId, uploaderId);
        String datePath = DateUtils.parseDateToStr(DateUtils.YYYYMMDD, DateUtils.getNowDate());
        OssUploadResult result = ossTemplate.upload(validatedFile, datePath + "/");
        if (result == null || result.getUrl() == null) {
            throw new BizException(FileConstants.FILE_UPLOAD_FAILED_MESSAGE);
        }
        if (result.getId() == null || result.getId().isBlank()) {
            result.setId(IdWorker.getIdStr());
        }
        recordUpload(validatedFile, result, uploaderId, detectedContentType, groupId);
        return result.getUrl();
    }

    private String validateUpload(MultipartFile file) {
        try {
            return fileUploadValidator.validate(file);
        } catch (FileValidationException exception) {
            throw new BizException(validationMessage(exception), exception);
        }
    }

    private static String validationMessage(FileValidationException exception) {
        return switch (exception.getReason()) {
            case EMPTY -> FileConstants.FILE_EMPTY_MESSAGE;
            case TOO_LARGE -> FileConstants.FILE_TOO_LARGE_MESSAGE;
            case FILENAME_REQUIRED -> FileConstants.FILE_NAME_REQUIRED_MESSAGE;
            case FILENAME_TOO_LONG -> FileConstants.FILE_NAME_TOO_LONG_MESSAGE;
            case EXTENSION_NOT_ALLOWED -> FileConstants.FILE_EXTENSION_NOT_ALLOWED_MESSAGE;
            case CONTENT_DETECTION_FAILED -> FileConstants.FILE_CONTENT_DETECTION_FAILED_MESSAGE;
            case CONTENT_TYPE_MISMATCH -> FileConstants.FILE_CONTENT_TYPE_MISMATCH_MESSAGE;
        };
    }

    public IPage<SysOssFileVo> list(OssFileQueryForm form, PageParam pageParam) {
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy(FileConstants.FILE_DEFAULT_ORDER);
        }
        OssFileQuery query = OssFileConvert.INSTANCE.toQuery(form);
        if (query == null) {
            query = new OssFileQuery();
        }
        if (!SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)) {
            query.setUploaderId(currentUploaderId());
        } else if (query.getUploaderId() == null) {
            // 管理员首次进入文件页时默认查看自己的文件；传入明确的 uploaderId 时仍允许切换查看范围。
            query.setUploaderId(currentUploaderId());
        }
        validateListGroup(query);
        IPage<SysOssFile> page = ossFileService.listFiles(query, pageParam);
        Map<Long, String> groupNames = loadGroupNames(page.getRecords());
        return page.convert(file -> {
            if (file.getGroupId() != null) {
                file.setGroupName(groupNames.get(file.getGroupId()));
            }
            return OssFileConvert.INSTANCE.toVo(file);
        });
    }

    public void download(Long id, HttpServletResponse response) throws IOException {
        SysOssFile file = getAccessibleFile(id);
        writeFile(file, response, true);
    }

    public void deleteById(Long id) {
        SysOssFile file = getAccessibleFile(id);
        validateFileNotInUse(file);
        deleteStoredFile(file);
    }

    private void validateFileNotInUse(SysOssFile file) {
        if (storedFileUsageChecker.isInUse(file.getFileUrl())) {
            throw new BizException(FileConstants.FILE_AVATAR_IN_USE_MESSAGE);
        }
    }

    private void deleteStoredFile(SysOssFile file) {
        if (!deleteOssFile(file)) {
            throw new BizException(FileConstants.FILE_OSS_DELETE_FAILED_MESSAGE);
        }
        if (!ossFileService.removeById(file.getId())) {
            log.error("OSS file deleted but database record deletion failed, id={}, fileId={}, url={}",
                    file.getId(), file.getFileId(), file.getFileUrl());
            throw new BizException(FileConstants.FILE_RECORD_DELETE_FAILED_MESSAGE.formatted(
                    file.getId(), file.getFileId()));
        }
    }

    public void deleteByIds(FileBatchForm form) {
        List<Long> fileIds = requireFileIds(form == null ? null : form.getFileIds());
        Long requestedUploaderId = form.getUploaderId();
        List<SysOssFile> files = fileIds.stream()
                .map(fileId -> getAccessibleFileInScope(fileId, requestedUploaderId))
                .toList();
        files.forEach(this::validateFileNotInUse);
        List<Long> failedIds = new ArrayList<>();
        for (SysOssFile file : files) {
            try {
                deleteStoredFile(file);
            } catch (RuntimeException exception) {
                failedIds.add(file.getId());
                log.error("Batch OSS file deletion failed, id={}, fileId={}", file.getId(), file.getFileId(), exception);
            }
        }
        if (!failedIds.isEmpty()) {
            throw new BizException(FileConstants.FILE_BATCH_DELETE_FAILED_MESSAGE.formatted(failedIds));
        }
    }

    @Transactional
    public void move(FileMoveForm form) {
        List<Long> fileIds = requireFileIds(form == null ? null : form.getFileIds());
        Long requestedUploaderId = form.getUploaderId();
        Long ownerId = null;
        for (Long fileId : fileIds) {
            SysOssFile file = getAccessibleFile(fileId);
            if (requestedUploaderId != null && !requestedUploaderId.equals(file.getUploaderId())) {
                throw new BizException(FileConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
            }
            if (file.getUploaderId() == null || (ownerId != null && !ownerId.equals(file.getUploaderId()))) {
                throw new BizException(FileConstants.FILE_GROUP_NOT_FOUND_MESSAGE);
            }
            ownerId = file.getUploaderId();
        }
        Long targetGroupId = form.getGroupId();
        validateGroup(targetGroupId, ownerId);
        try {
            int affectedRows = ossFileService.updateGroup(fileIds, ownerId, targetGroupId);
            if (affectedRows != fileIds.size() && !allFilesInGroup(fileIds, ownerId, targetGroupId)) {
                throw new BizException(FileConstants.FILE_BATCH_MOVE_FAILED_MESSAGE);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BizException(FileConstants.FILE_GROUP_NOT_FOUND_MESSAGE, exception);
        }
    }

    public void rename(Long id, FileRenameForm form) {
        SysOssFile file = getAccessibleFile(id);
        String newName = form == null || form.getNewName() == null ? null : form.getNewName().trim();
        if (newName == null || newName.isBlank()) {
            throw new BizException(FileConstants.FILE_RENAME_REQUIRED_MESSAGE);
        }
        validateRenameName(newName);
        String oldName = file.getOriginalFilename();
        if (oldName == null || oldName.isBlank()) {
            oldName = file.getFileName();
        }
        if (!sameExtension(oldName, newName)) {
            throw new BizException(FileConstants.FILE_RENAME_EXTENSION_MESSAGE);
        }
        if (newName.equals(file.getOriginalFilename())) {
            return;
        }
        if (ossFileService.updateOriginalFilename(file.getId(), file.getUploaderId(), newName) != 1) {
            throw new BizException(FileConstants.FILE_RECORD_UPDATE_FAILED_MESSAGE.formatted(file.getId(), file.getFileId()));
        }
    }

    public void preview(Long id, HttpServletResponse response) throws IOException {
        SysOssFile file = getAccessibleFile(id);
        if (!isPreviewable(file.getContentType())) {
            throw new BizException(FileConstants.FILE_PREVIEW_UNSUPPORTED_MESSAGE);
        }
        writeFile(file, response, false);
    }

    public String textPreview(Long id) throws IOException {
        SysOssFile file = getAccessibleFile(id);
        if (!isTextPreviewable(file)) {
            throw new BizException(FileConstants.FILE_TEXT_PREVIEW_UNSUPPORTED_MESSAGE);
        }
        if (file.getFileSize() != null && file.getFileSize() > FileConstants.FILE_TEXT_PREVIEW_MAX_SIZE_BYTES) {
            throw new BizException(FileConstants.FILE_TEXT_PREVIEW_TOO_LARGE_MESSAGE);
        }
        byte[] content = readTextContent(file);
        if (content.length > FileConstants.FILE_TEXT_PREVIEW_MAX_SIZE_BYTES) {
            throw new BizException(FileConstants.FILE_TEXT_PREVIEW_TOO_LARGE_MESSAGE);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private void recordUpload(MultipartFile file, OssUploadResult result, Long uploaderId,
                              String detectedContentType, Long groupId) {
        SysOssFile data = buildRecordData(file, result, uploaderId, detectedContentType, groupId);
        boolean saved;
        try {
            saved = ossFileService.saveIfAbsent(data);
        } catch (Exception exception) {
            log.warn("OSS upload succeeded but file record insert failed, fileId={}", result.getId(), exception);
            retryTask.submit(data);
            return;
        }
        if (!saved) {
            retryTask.submit(data);
        }
    }

    private SysOssFile getAccessibleFile(Long id) {
        if (id == null) {
            throw new BizException(FileConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }
        SysOssFile file = ossFileService.getById(id);
        if (file == null || !canAccess(file)) {
            throw new BizException(FileConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }
        return file;
    }

    private boolean deleteOssFile(SysOssFile file) {
        return ossTemplate.delete(toFileInfo(file));
    }

    private static boolean canAccess(SysOssFile file) {
        if (SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)) {
            return true;
        }
        int currentUserId = SecurityUtils.getLoginIdAsInt();
        return file.getUploaderId() != null && currentUserId > 0
                && file.getUploaderId() == currentUserId;
    }

    private static Long currentUploaderId() {
        int currentUserId = SecurityUtils.getLoginIdAsInt();
        if (currentUserId <= 0) {
            throw new BizException(FileConstants.FILE_CURRENT_USER_REQUIRED_MESSAGE);
        }
        return (long) currentUserId;
    }

    @SuppressWarnings("deprecation")
    private FileInfo toFileInfo(SysOssFile file) {
        String platform = file.getPlatform();
        if (platform == null || platform.isBlank()) {
            platform = ossTemplate.getFileStorageService().getDefaultPlatform();
        }
        String objectKey = extractObjectKey(file.getFileUrl());
        if (objectKey == null || objectKey.isBlank()) {
            objectKey = file.getFileName();
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new BizException(FileConstants.FILE_OBJECT_KEY_UNAVAILABLE_MESSAGE);
        }
        return new FileInfo()
                .setUrl(file.getFileUrl())
                .setPlatform(platform)
                .setFilename(objectKey);
    }

    private static String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String path = URI.create(url).getRawPath();
            if (path == null) {
                return null;
            }
            int firstCharacter = 0;
            while (firstCharacter < path.length() && path.charAt(firstCharacter) == '/') {
                firstCharacter++;
            }
            return path.substring(firstCharacter);
        } catch (IllegalArgumentException exception) {
            log.warn("Invalid OSS file URL, url={}", url, exception);
            return null;
        }
    }

    private static SysOssFile buildRecordData(MultipartFile file, OssUploadResult result,
                                              Long uploaderId, String detectedContentType, Long groupId) {
        String originalFilename = result.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = file.getOriginalFilename();
        }
        return SysOssFile.builder()
                .fileId(result.getId())
                .fileUrl(result.getUrl())
                .fileName(result.getFilename())
                .originalFilename(originalFilename)
                .contentType(detectedContentType)
                .fileSize(result.getSize())
                .platform(result.getPlatform())
                .thumbnailUrl(result.getThUrl())
                .uploaderId(uploaderId)
                .groupId(groupId)
                .build();
    }

    private SysOssFile getAccessibleFileInScope(Long id, Long requestedUploaderId) {
        SysOssFile file = getAccessibleFile(id);
        if (requestedUploaderId != null && !requestedUploaderId.equals(file.getUploaderId())) {
            throw new BizException(FileConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }
        return file;
    }

    private boolean allFilesInGroup(List<Long> fileIds, Long uploaderId, Long groupId) {
        LambdaQueryWrapper<SysOssFile> query = new LambdaQueryWrapper<SysOssFile>()
                .in(SysOssFile::getId, fileIds)
                .eq(SysOssFile::getUploaderId, uploaderId);
        if (groupId == null) {
            query.isNull(SysOssFile::getGroupId);
        } else {
            query.eq(SysOssFile::getGroupId, groupId);
        }
        return ossFileService.count(query) == fileIds.size();
    }

    private void validateRenameName(String newName) {
        int configuredLimit = fileUploadValidationProperties.getMaxFilenameLength();
        int maxLength = Math.min(configuredLimit, FileConstants.FILE_NAME_DATABASE_MAX_LENGTH);
        if (newName.length() > maxLength) {
            throw new BizException(FileConstants.FILE_RENAME_TOO_LONG_MESSAGE);
        }
        if (newName.chars().anyMatch(character -> Character.isISOControl(character)
                || character == '/' || character == '\\')) {
            throw new BizException(FileConstants.FILE_RENAME_INVALID_MESSAGE);
        }
    }

    private byte[] readTextContent(SysOssFile file) throws IOException {
        byte[][] content = new byte[1][];
        try {
            ossTemplate.getFileStorageService().download(toFileInfo(file)).inputStream(input -> {
                try {
                    content[0] = input.readNBytes(FileConstants.FILE_TEXT_PREVIEW_MAX_SIZE_BYTES + 1);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
        return content[0] == null ? new byte[0] : content[0];
    }

    private void validateGroup(Long groupId, Long uploaderId) {
        if (groupId != null) {
            if (fileGroupBizService == null) {
                throw new BizException(FileConstants.FILE_GROUP_NOT_FOUND_MESSAGE);
            }
            fileGroupBizService.validateGroupForUploader(groupId, uploaderId);
        }
    }

    private void validateListGroup(OssFileQuery query) {
        if (query.getGroupId() == null) {
            return;
        }
        if (fileGroupBizService == null || query.getUploaderId() == null) {
            throw new BizException(FileConstants.FILE_GROUP_NOT_FOUND_MESSAGE);
        }
        fileGroupBizService.validateGroupForUploader(query.getGroupId(), query.getUploaderId());
    }

    private Map<Long, String> loadGroupNames(Collection<SysOssFile> files) {
        if (fileGroupBizService == null || files == null || files.isEmpty()) {
            return Map.of();
        }
        List<Long> groupIds = files.stream().map(SysOssFile::getGroupId).filter(java.util.Objects::nonNull).distinct().toList();
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        return fileGroupBizService.namesByIds(groupIds);
    }

    private static List<Long> requireFileIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BizException(FileConstants.FILE_FILE_IDS_REQUIRED_MESSAGE);
        }
        List<Long> normalized = fileIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (normalized.isEmpty()) {
            throw new BizException(FileConstants.FILE_FILE_IDS_REQUIRED_MESSAGE);
        }
        return normalized;
    }

    private static boolean sameExtension(String oldName, String newName) {
        return extension(oldName).equalsIgnoreCase(extension(newName));
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index);
    }

    private static boolean isPreviewable(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase();
        return normalized.startsWith("image/") || normalized.startsWith("video/")
                || normalized.startsWith("audio/") || "application/pdf".equals(normalized);
    }

    private static boolean isTextPreviewable(SysOssFile file) {
        String contentType = file.getContentType();
        if (contentType != null && (contentType.toLowerCase().startsWith("text/")
                || contentType.toLowerCase().contains("json")
                || contentType.toLowerCase().contains("javascript")
                || contentType.toLowerCase().contains("xml"))) {
            return true;
        }
        String name = file.getOriginalFilename() == null ? file.getFileName() : file.getOriginalFilename();
        return name != null && name.matches("(?i).*\\.(txt|md|csv|log|java|kt|js|ts|vue|html|css|json|xml|yaml|yml|sql)$");
    }

    private void writeFile(SysOssFile file, HttpServletResponse response, boolean attachment) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = file.getFileName();
        }
        ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(filename, StandardCharsets.UTF_8).build();
        response.setContentType(resolveMediaType(file.getContentType()).toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (file.getFileSize() != null && file.getFileSize() > 0) {
            response.setContentLengthLong(file.getFileSize());
        }
        ossTemplate.getFileStorageService().download(toFileInfo(file)).outputStream(response.getOutputStream());
    }

    private static MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
