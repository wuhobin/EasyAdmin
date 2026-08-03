package com.nexora.file.service;

import com.nexora.file.domain.query.OssFileQuery;
import com.nexora.file.entity.SysOssFile;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysOssFileService extends IService<SysOssFile> {

    boolean saveIfAbsent(SysOssFile file);

    IPage<SysOssFile> listFiles(OssFileQuery query, PageParam pageParam);
}
