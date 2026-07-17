package com.aurora.service.impl;

import com.aurora.domain.query.OssFileQuery;
import com.aurora.domain.dto.file.OssFileRecordRetryData;
import com.aurora.entity.SysOssFile;
import com.aurora.mapper.SysOssFileMapper;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.webmvc.exception.BizException;
import com.aurora.domain.vo.file.SysOssFileVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.URI;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SysOssFileServiceImpl extends ServiceImpl<SysOssFileMapper, SysOssFile>
        implements SysOssFileService {

    private final OssTemplate ossTemplate;

    @Override
    public boolean saveIfAbsent(OssFileRecordRetryData data) {
        LambdaQueryWrapper<SysOssFile> fileIdQuery = new LambdaQueryWrapper<SysOssFile>()
                .eq(SysOssFile::getFileId, data.getFileId());
        if (baseMapper.exists(fileIdQuery)) {
            return true;
        }
        try {
            return save(toEntity(data));
        } catch (DuplicateKeyException exception) {
            return true;
        }
    }

    @Override
    public IPage<SysOssFileVo> listFiles(OssFileQuery query, PageParam pageParam) {
        OssFileQuery safeQuery = query == null ? new OssFileQuery() : query;
        LambdaQueryWrapper<SysOssFile> wrapper = new LambdaQueryWrapper<SysOssFile>()
                .like(StringUtils.isNotBlank(safeQuery.getFileName()),
                        SysOssFile::getFileName, safeQuery.getFileName())
                .eq(StringUtils.isNotBlank(safeQuery.getContentType()),
                        SysOssFile::getContentType, safeQuery.getContentType())
                .like(StringUtils.isNotBlank(safeQuery.getUploaderName()),
                        SysOssFile::getUploaderName, safeQuery.getUploaderName())
                .orderByDesc(SysOssFile::getCreateTime);
        IPage<SysOssFile> entityPage = page(PageUtils.buildPage(pageParam), wrapper);
        return entityPage.convert(SysOssFileServiceImpl::toVo);
    }

    @Override
    public SysOssFile getDownloadFile(Long id) {
        SysOssFile file = getById(id);
        if (file == null) {
            throw new BizException("文件记录不存在");
        }
        return file;
    }

    @Override
    public void download(SysOssFile file, OutputStream outputStream) {
        ossTemplate.getFileStorageService()
                .download(toFileInfo(file))
                .outputStream(outputStream);
    }

    @Override
    public void deleteById(Long id) {
        SysOssFile file = getById(id);
        if (file == null) {
            throw new BizException("文件记录不存在");
        }
        if (!deleteOssFile(file)) {
            throw new BizException("OSS 文件删除失败");
        }
        if (!removeById(id)) {
            log.error("OSS file deleted but database record deletion failed, id={}, fileId={}, url={}",
                    file.getId(), file.getFileId(), file.getFileUrl());
            throw new BizException("文件记录删除失败, id=" + file.getId()
                    + ", fileId=" + file.getFileId());
        }
    }

    @Override
    public boolean deleteByUrl(String url) {
        LambdaQueryWrapper<SysOssFile> wrapper = new LambdaQueryWrapper<SysOssFile>()
                .eq(SysOssFile::getFileUrl, url);
        List<SysOssFile> files = list(wrapper);
        if (files.isEmpty()) {
            return deleteOssFile(SysOssFile.builder().fileUrl(url).build());
        }
        if (!deleteOssFile(files.getFirst())) {
            return false;
        }
        if (!remove(wrapper)) {
            List<Long> recordIds = files.stream().map(SysOssFile::getId).toList();
            List<String> fileIds = files.stream().map(SysOssFile::getFileId).toList();
            log.error("OSS file deleted but database records deletion failed, ids={}, fileIds={}, url={}",
                    recordIds, fileIds, url);
            throw new BizException("文件记录删除失败, ids=" + recordIds + ", fileIds=" + fileIds);
        }
        return true;
    }

    private boolean deleteOssFile(SysOssFile file) {
        return ossTemplate.delete(toFileInfo(file));
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

    private static SysOssFile toEntity(OssFileRecordRetryData data) {
        return SysOssFile.builder()
                .fileId(data.getFileId())
                .fileUrl(data.getFileUrl())
                .fileName(data.getFileName())
                .originalFilename(data.getOriginalFilename())
                .contentType(data.getContentType())
                .fileSize(data.getFileSize())
                .platform(data.getPlatform())
                .thumbnailUrl(data.getThumbnailUrl())
                .uploaderId(data.getUploaderId())
                .uploaderName(data.getUploaderName())
                .build();
    }

    private static SysOssFileVo toVo(SysOssFile file) {
        return SysOssFileVo.builder()
                .id(file.getId())
                .fileId(file.getFileId())
                .fileUrl(file.getFileUrl())
                .fileName(file.getFileName())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .platform(file.getPlatform())
                .thumbnailUrl(file.getThumbnailUrl())
                .uploaderId(file.getUploaderId())
                .uploaderName(file.getUploaderName())
                .createTime(file.getCreateTime())
                .build();
    }
}
