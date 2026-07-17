package com.aurora.service.impl;

import com.aurora.common.Constants;
import com.aurora.dto.file.OssFileRecordRetryData;
import com.aurora.dto.user.LoginUserInfo;
import com.aurora.service.FileService;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.common.utils.DateUtils;
import com.aurora.starter.common.utils.JsonUtil;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.aurora.task.OssFileRecordRetryTask;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final OssTemplate ossTemplate;

    private final SysOssFileService ossFileService;

    private final OssFileRecordRetryTask retryTask;

    @Override
    public OssUploadResult upload(MultipartFile file) {
        String datePath = DateUtils.parseDateToStr(DateUtils.YYYYMMDD, DateUtils.getNowDate());
        return uploadAndRecord(file, datePath + "/");
    }

    @Override
    public OssUploadResult upload(MultipartFile file, String path) {
        return uploadAndRecord(file, path);
    }

    private OssUploadResult uploadAndRecord(MultipartFile file, String path) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        OssUploadResult result = ossTemplate.upload(file, path);
        if (result == null || result.getUrl() == null) {
            throw new BizException("上传文件失败");
        }
        if (result.getId() == null || result.getId().isBlank()) {
            result.setId(IdWorker.getIdStr());
        }

        OssFileRecordRetryData data = buildRecordData(file, result, currentUser());
        boolean saved;
        try {
            saved = ossFileService.saveIfAbsent(data);
        } catch (Exception exception) {
            log.warn("OSS upload succeeded but file record insert failed, fileId={}", result.getId(), exception);
            retryTask.submit(data);
            return result;
        }
        if (!saved) {
            retryTask.submit(data);
        }
        return result;
    }

    private static OssFileRecordRetryData buildRecordData(MultipartFile file, OssUploadResult result,
                                                            LoginUserInfo user) {
        String originalFilename = result.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = file.getOriginalFilename();
        }
        String contentType = result.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = file.getContentType();
        }
        return OssFileRecordRetryData.builder()
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

    private static LoginUserInfo currentUser() {
        try {
            Object sessionUser = SecurityUtils.getSessionAttribute(Constants.CURRENT_USER);
            return JsonUtil.parse(JsonUtil.toJson(sessionUser), LoginUserInfo.class);
        } catch (Exception exception) {
            log.debug("Current upload user is unavailable", exception);
            return null;
        }
    }

    @Override
    public boolean delete(String url) {
        return url != null && !url.isEmpty() && ossTemplate.delete(url);
    }

    @Override
    public boolean exists(String url) {
        return url != null && !url.isEmpty() && ossTemplate.exists(url);
    }

    @Override
    public FileInfo getFileInfo(String url) {
        return url == null || url.isEmpty() ? null : ossTemplate.getFileInfo(url);
    }
}
