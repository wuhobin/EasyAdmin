package com.nexora.file.biz;

import com.nexora.constants.CommonConstants;
import com.nexora.file.domain.convert.OssFileConvert;
import com.nexora.file.domain.form.OssFileQueryForm;
import com.nexora.file.domain.query.OssFileQuery;
import com.nexora.file.domain.vo.SysOssFileVo;
import com.nexora.file.entity.SysOssFile;
import com.nexora.file.service.SysOssFileService;
import com.nexora.identity.service.SysUserService;
import com.aurora.starter.common.utils.DateUtils;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.file.task.OssFileRecordRetryTask;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileBizService {

    private static final Tika TIKA = new Tika();

    private final OssTemplate ossTemplate;
    private final SysOssFileService ossFileService;
    private final SysUserService sysUserService;
    private final OssFileRecordRetryTask retryTask;

    public String upload(MultipartFile file) {
        String detectedContentType = validateUpload(file);
        MultipartFile validatedFile = new ValidatedMultipartFile(file, detectedContentType);
        Long uploaderId = currentUploaderId();
        String datePath = DateUtils.parseDateToStr(DateUtils.YYYYMMDD, DateUtils.getNowDate());
        OssUploadResult result = ossTemplate.upload(validatedFile, datePath + "/");
        if (result == null || result.getUrl() == null) {
            throw new BizException(CommonConstants.FILE_UPLOAD_FAILED_MESSAGE);
        }
        if (result.getId() == null || result.getId().isBlank()) {
            result.setId(IdWorker.getIdStr());
        }
        recordUpload(validatedFile, result, uploaderId, detectedContentType);
        return result.getUrl();
    }

    private static String validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(CommonConstants.FILE_EMPTY_MESSAGE);
        }
        if (file.getSize() > CommonConstants.FILE_UPLOAD_MAX_SIZE) {
            throw new BizException(CommonConstants.FILE_TOO_LARGE_MESSAGE);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BizException(CommonConstants.FILE_NAME_REQUIRED_MESSAGE);
        }
        if (originalFilename.codePointCount(0, originalFilename.length())
                > CommonConstants.FILE_ORIGINAL_FILENAME_MAX_LENGTH) {
            throw new BizException(CommonConstants.FILE_NAME_TOO_LONG_MESSAGE);
        }
        String expectedContentType = expectedContentType(originalFilename);
        String detectedContentType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedContentType = TIKA.detect(inputStream);
        } catch (IOException exception) {
            throw new BizException(CommonConstants.FILE_CONTENT_DETECTION_FAILED_MESSAGE);
        }
        if (!expectedContentType.equals(detectedContentType)) {
            throw new BizException(CommonConstants.FILE_CONTENT_TYPE_MISMATCH_MESSAGE);
        }
        return detectedContentType;
    }

    private static String expectedContentType(String originalFilename) {
        int lastSeparator = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        int extensionSeparator = originalFilename.lastIndexOf('.');
        if (extensionSeparator <= lastSeparator || extensionSeparator == originalFilename.length() - 1) {
            throw new BizException(CommonConstants.FILE_EXTENSION_NOT_ALLOWED_MESSAGE);
        }
        String extension = originalFilename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        String contentType = CommonConstants.FILE_ALLOWED_CONTENT_TYPE_BY_EXTENSION.get(extension);
        if (contentType == null) {
            throw new BizException(CommonConstants.FILE_EXTENSION_NOT_ALLOWED_MESSAGE);
        }
        return contentType;
    }

    public IPage<SysOssFileVo> list(OssFileQueryForm form, PageParam pageParam) {
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy(CommonConstants.FILE_DEFAULT_ORDER);
        }
        OssFileQuery query = OssFileConvert.INSTANCE.toQuery(form);
        if (query == null) {
            query = new OssFileQuery();
        }
        if (!SecurityUtils.hasRole(CommonConstants.ADMIN)) {
            query.setUploaderId(currentUploaderId());
        }
        IPage<SysOssFile> page = ossFileService.listFiles(query, pageParam);
        return page.convert(OssFileConvert.INSTANCE::toVo);
    }

    public void download(Long id, HttpServletResponse response) throws IOException {
        SysOssFile file = getAccessibleFile(id);
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = file.getFileName();
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        response.setContentType(resolveMediaType(file.getContentType()).toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (file.getFileSize() != null && file.getFileSize() > 0) {
            response.setContentLengthLong(file.getFileSize());
        }
        ossTemplate.getFileStorageService()
                .download(toFileInfo(file))
                .outputStream(response.getOutputStream());
    }

    public void deleteById(Long id) {
        SysOssFile file = getAccessibleFile(id);
        if (sysUserService.existsByAvatar(file.getFileUrl())) {
            throw new BizException(CommonConstants.FILE_AVATAR_IN_USE_MESSAGE);
        }
        if (!deleteOssFile(file)) {
            throw new BizException(CommonConstants.FILE_OSS_DELETE_FAILED_MESSAGE);
        }
        if (!ossFileService.removeById(id)) {
            log.error("OSS file deleted but database record deletion failed, id={}, fileId={}, url={}",
                    file.getId(), file.getFileId(), file.getFileUrl());
            throw new BizException(CommonConstants.FILE_RECORD_DELETE_FAILED_MESSAGE.formatted(
                    file.getId(), file.getFileId()));
        }
    }

    private void recordUpload(MultipartFile file, OssUploadResult result, Long uploaderId,
                              String detectedContentType) {
        SysOssFile data = buildRecordData(file, result, uploaderId, detectedContentType);
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
            throw new BizException(CommonConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }
        SysOssFile file = ossFileService.getById(id);
        if (file == null || !canAccess(file)) {
            throw new BizException(CommonConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }
        return file;
    }

    private boolean deleteOssFile(SysOssFile file) {
        return ossTemplate.delete(toFileInfo(file));
    }

    private static boolean canAccess(SysOssFile file) {
        if (SecurityUtils.hasRole(CommonConstants.ADMIN)) {
            return true;
        }
        int currentUserId = SecurityUtils.getLoginIdAsInt();
        return file.getUploaderId() != null && currentUserId > 0
                && file.getUploaderId().longValue() == currentUserId;
    }

    private static Long currentUploaderId() {
        int currentUserId = SecurityUtils.getLoginIdAsInt();
        if (currentUserId <= 0) {
            throw new BizException(CommonConstants.FILE_CURRENT_USER_REQUIRED_MESSAGE);
        }
        return (long) currentUserId;
    }

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
            throw new BizException(CommonConstants.FILE_OBJECT_KEY_UNAVAILABLE_MESSAGE);
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
                                              Long uploaderId, String detectedContentType) {
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
                .build();
    }

    private static final class ValidatedMultipartFile implements MultipartFile {

        private final MultipartFile delegate;
        private final String contentType;

        private ValidatedMultipartFile(MultipartFile delegate, String contentType) {
            this.delegate = delegate;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getOriginalFilename() {
            return delegate.getOriginalFilename();
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return delegate.getBytes();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            delegate.transferTo(dest);
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            delegate.transferTo(dest);
        }
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
