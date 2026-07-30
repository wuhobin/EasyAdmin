package com.nexora.service;

import com.nexora.domain.query.OssFileQuery;
import com.nexora.entity.SysOssFile;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysOssFileService extends IService<SysOssFile> {

    boolean saveIfAbsent(SysOssFile file);

    IPage<SysOssFile> listFiles(OssFileQuery query, PageParam pageParam);
}
