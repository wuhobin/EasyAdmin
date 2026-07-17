package com.aurora.service;

import com.aurora.domain.query.OssFileQuery;
import com.aurora.domain.dto.file.OssFileRecordRetryData;
import com.aurora.entity.SysOssFile;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysOssFileService extends IService<SysOssFile> {

    boolean saveIfAbsent(OssFileRecordRetryData data);

    IPage<SysOssFile> listFiles(OssFileQuery query, PageParam pageParam);

    List<SysOssFile> listByUrl(String url);
}
