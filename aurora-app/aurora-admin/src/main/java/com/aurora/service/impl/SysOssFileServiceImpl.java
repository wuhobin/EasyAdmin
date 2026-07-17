package com.aurora.service.impl;

import com.aurora.domain.query.OssFileQuery;
import com.aurora.domain.dto.file.OssFileRecordRetryData;
import com.aurora.entity.SysOssFile;
import com.aurora.mapper.SysOssFileMapper;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysOssFileServiceImpl extends ServiceImpl<SysOssFileMapper, SysOssFile>
        implements SysOssFileService {

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
    public IPage<SysOssFile> listFiles(OssFileQuery query, PageParam pageParam) {
        return page(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public List<SysOssFile> listByUrl(String url) {
        LambdaQueryWrapper<SysOssFile> wrapper = new LambdaQueryWrapper<SysOssFile>()
                .eq(SysOssFile::getFileUrl, url);
        return list(wrapper);
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

}
