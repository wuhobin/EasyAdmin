package com.nexora.file.service.impl;

import com.nexora.file.domain.query.OssFileQuery;
import com.nexora.file.entity.SysOssFile;
import com.nexora.file.mapper.SysOssFileMapper;
import com.nexora.file.service.SysOssFileService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysOssFileServiceImpl extends ServiceImpl<SysOssFileMapper, SysOssFile>
        implements SysOssFileService {

    @Override
    public boolean saveIfAbsent(SysOssFile file) {
        if (file.getFileId() == null) {
            return save(file);
        }
        OssFileQuery query = new OssFileQuery();
        query.setFileId(file.getFileId());
        if (baseMapper.exists(DynamicCondition.toWrapper(query))) {
            return true;
        }
        try {
            return save(file);
        } catch (DuplicateKeyException exception) {
            return true;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public IPage<SysOssFile> listFiles(OssFileQuery query, PageParam pageParam) {
        boolean ungrouped = Boolean.TRUE.equals(query.getUngrouped());
        query.setUngrouped(null);
        QueryWrapper<SysOssFile> wrapper = (QueryWrapper<SysOssFile>) (Wrapper<?>) DynamicCondition.toWrapper(query);
        if (ungrouped) {
            wrapper.isNull("group_id");
        }
        return page(PageUtils.buildPage(pageParam), wrapper);
    }

    @Override
    public int updateOriginalFilename(Long id, Long uploaderId, String originalFilename) {
        return baseMapper.updateOriginalFilename(id, uploaderId, originalFilename);
    }

    @Override
    public int updateGroup(List<Long> fileIds, Long uploaderId, Long groupId) {
        return baseMapper.updateGroup(fileIds, uploaderId, groupId);
    }

}
