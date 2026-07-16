package com.aurora.service.impl;

import com.aurora.exception.BusinessException;
import com.aurora.service.FileService;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.common.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务实现
 *
 * <p>在 {@link OssTemplate} 之上提供管理员后台的文件操作能力。
 * 上传时会按当前日期（{@link DateUtils#YYYYMMDD}）生成形如 {@code 20260706/} 的存储子路径，
 * 便于在 OSS 上按时间归档。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final OssTemplate ossTemplate;

    @Override
    public OssUploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String datePath = DateUtils.parseDateToStr(DateUtils.YYYYMMDD, DateUtils.getNowDate());
        String path = datePath + "/";
        OssUploadResult result = ossTemplate.upload(file, path);
        if (result == null || result.getUrl() == null) {
            throw new BusinessException("上传文件失败");
        }
        return result;
    }

    @Override
    public OssUploadResult upload(MultipartFile file, String path) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        OssUploadResult result = ossTemplate.upload(file, path);
        if (result == null || result.getUrl() == null) {
            throw new BusinessException("上传文件失败");
        }
        return result;
    }

    @Override
    public boolean delete(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return ossTemplate.delete(url);
    }

    @Override
    public boolean exists(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return ossTemplate.exists(url);
    }

    @Override
    public FileInfo getFileInfo(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        return ossTemplate.getFileInfo(url);
    }
}
