package com.aurora.biz;

import com.aurora.common.Constants;
import com.aurora.domain.convert.OssFileConvert;
import com.aurora.domain.form.query.file.OssFileQueryForm;
import com.aurora.domain.vo.auth.LoginUserInfoVo;
import com.aurora.domain.vo.file.SysOssFileVo;
import com.aurora.entity.SysOssFile;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.common.utils.DateUtils;
import com.aurora.starter.common.utils.JsonUtil;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.aurora.task.OssFileRecordRetryTask;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileBizService {

    private final OssTemplate ossTemplate;
    private final SysOssFileService ossFileService;
    private final OssFileRecordRetryTask retryTask;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        String datePath = DateUtils.parseDateToStr(DateUtils.YYYYMMDD, DateUtils.getNowDate());
        OssUploadResult result = ossTemplate.upload(file, datePath + "/");
        if (result == null || result.getUrl() == null) {
            throw new BizException("上传文件失败");
        }
        if (result.getId() == null || result.getId().isBlank()) {
            result.setId(IdWorker.getIdStr());
        }
        recordUpload(file, result);
        return result.getUrl();
    }

    public IPage<SysOssFileVo> list(OssFileQueryForm form, PageParam pageParam) {
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy("create_time desc");
        }
        IPage<SysOssFile> page = ossFileService.listFiles(OssFileConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(OssFileConvert.INSTANCE::toVo);
    }

    public void download(Long id, HttpServletResponse response) throws IOException {
        SysOssFile file = getFile(id);
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
        SysOssFile file = getFile(id);
        checkDeletePermission(file);
        if (!deleteOssFile(file)) {
            throw new BizException("OSS 文件删除失败");
        }
        if (!ossFileService.removeById(id)) {
            log.error("OSS file deleted but database record deletion failed, id={}, fileId={}, url={}",
                    file.getId(), file.getFileId(), file.getFileUrl());
            throw new BizException("文件记录删除失败, id=" + file.getId() + ", fileId=" + file.getFileId());
        }
    }

    public boolean deleteByUrl(String url) {
        List<SysOssFile> files = ossFileService.listByUrl(url);
        if (files.isEmpty()) {
            checkAdminDeletePermission();
            return deleteOssFile(SysOssFile.builder().fileUrl(url).build());
        }
        files.forEach(this::checkDeletePermission);
        if (!deleteOssFile(files.getFirst())) {
            return false;
        }
        List<Long> recordIds = files.stream().map(SysOssFile::getId).toList();
        if (!ossFileService.removeBatchByIds(recordIds)) {
            List<String> fileIds = files.stream().map(SysOssFile::getFileId).toList();
            log.error("OSS file deleted but database records deletion failed, ids={}, fileIds={}, url={}",
                    recordIds, fileIds, url);
            throw new BizException("文件记录删除失败, ids=" + recordIds + ", fileIds=" + fileIds);
        }
        return true;
    }

    private void recordUpload(MultipartFile file, OssUploadResult result) {
        SysOssFile data = buildRecordData(file, result, currentUser());
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

    private SysOssFile getFile(Long id) {
        SysOssFile file = ossFileService.getById(id);
        if (file == null) {
            throw new BizException("文件记录不存在");
        }
        return file;
    }

    private boolean deleteOssFile(SysOssFile file) {
        return ossTemplate.delete(toFileInfo(file));
    }

    private void checkDeletePermission(SysOssFile file) {
        if (SecurityUtils.hasRole(Constants.ADMIN)) {
            return;
        }
        Integer currentUserId = SecurityUtils.getLoginIdAsInt();
        if (file.getUploaderId() == null || currentUserId == null
                || file.getUploaderId().longValue() != currentUserId.longValue()) {
            throw new BizException("只能删除自己上传的文件");
        }
    }

    private static void checkAdminDeletePermission() {
        if (!SecurityUtils.hasRole(Constants.ADMIN)) {
            throw new BizException("只能删除自己上传的文件");
        }
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
            throw new BizException("无法解析 OSS 文件对象名称");
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
                                               LoginUserInfoVo user) {
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
                .uploaderId(user == null || user.getId() == null ? null : user.getId().longValue())
                .uploaderName(user == null ? null : user.getNickname())
                .build();
    }

    private static LoginUserInfoVo currentUser() {
        try {
            Object sessionUser = SecurityUtils.getSessionAttribute(Constants.CURRENT_USER);
            return JsonUtil.parse(JsonUtil.toJson(sessionUser), LoginUserInfoVo.class);
        } catch (Exception exception) {
            log.debug("Current upload user is unavailable", exception);
            return null;
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
