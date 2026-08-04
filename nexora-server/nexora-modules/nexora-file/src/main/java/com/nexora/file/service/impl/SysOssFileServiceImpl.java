package com.nexora.file.service.impl;

import com.nexora.file.domain.query.OssFileQuery;
import com.nexora.file.entity.SysOssFile;
import com.nexora.file.mapper.SysOssFileMapper;
import com.nexora.file.service.SysOssFileService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class SysOssFileServiceImpl extends ServiceImpl<SysOssFileMapper, SysOssFile>
        implements SysOssFileService {

    @Override
    public boolean saveIfAbsent(SysOssFile file) {
        LambdaQueryWrapper<SysOssFile> fileIdQuery = new LambdaQueryWrapper<SysOssFile>()
                .eq(SysOssFile::getFileId, file.getFileId());
        if (baseMapper.exists(fileIdQuery)) {
            return true;
        }
        try {
            return save(file);
        } catch (DuplicateKeyException exception) {
            return true;
        }
    }

    @Override
    public IPage<SysOssFile> listFiles(OssFileQuery query, PageParam pageParam) {
        return page(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

}
