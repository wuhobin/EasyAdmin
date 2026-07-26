package com.nexora.service.impl;

import com.nexora.domain.query.OssFileQuery;
import com.nexora.entity.SysOssFile;
import com.nexora.mapper.SysOssFileMapper;
import com.nexora.service.SysOssFileService;
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

    @Override
    public List<SysOssFile> listByUrl(String url) {
        LambdaQueryWrapper<SysOssFile> wrapper = new LambdaQueryWrapper<SysOssFile>()
                .eq(SysOssFile::getFileUrl, url);
        return list(wrapper);
    }

}
