package com.nexora.biz.file;

import com.nexora.constants.CommonConstants;
import com.nexora.domain.convert.OssFileConvert;
import com.nexora.domain.form.query.file.OssFileQueryForm;
import com.nexora.domain.query.OssFileQuery;
import com.nexora.domain.vo.file.SysOssFileVo;
import com.nexora.entity.SysOssFile;
import com.nexora.service.SysOssFileService;
import com.aurora.starter.common.utils.DateUtils;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.task.OssFileRecordRetryTask;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileBizService {

    private final OssTemplate ossTemplate;
    private final SysOssFileService ossFileService;
    private final OssFileRecordRetryTask retryTask;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(CommonConstants.FILE_EMPTY_MESSAGE);
        }
        Long uploaderId = currentUploaderId();
        String datePath = DateUtils.parseDateToStr(DateUtils.YYYYMMDD, DateUtils.getNowDate());
        OssUploadResult result = ossTemplate.upload(file, datePath + "/");
        if (result == null || result.getUrl() == null) {
            throw new BizException(CommonConstants.FILE_UPLOAD_FAILED_MESSAGE);
        }
        if (result.getId() == null || result.getId().isBlank()) {
            result.setId(IdWorker.getIdStr());
        }
        recordUpload(file, result, uploaderId);
        return result.getUrl();
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

    private void recordUpload(MultipartFile file, OssUploadResult result, Long uploaderId) {
        SysOssFile data = buildRecordData(file, result, uploaderId);
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
                                               Long uploaderId) {
        String originalFilename = result.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = file.getOriginalFilename();
        }
        String contentType = result.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = file.getContentType();
        }
        return SysOssFile.builder()
                .fileId(result.getId())
                .fileUrl(result.getUrl())
                .fileName(result.getFilename())
                .originalFilename(originalFilename)
                .contentType(contentType)
                .fileSize(result.getSize())
                .platform(result.getPlatform())
                .thumbnailUrl(result.getThUrl())
                .uploaderId(uploaderId)
                .build();
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
