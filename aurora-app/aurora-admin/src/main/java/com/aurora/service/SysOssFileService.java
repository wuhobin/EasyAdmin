package com.aurora.service;

import com.aurora.domain.query.OssFileQuery;
import com.aurora.domain.dto.file.OssFileRecordRetryData;
import com.aurora.entity.SysOssFile;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.domain.vo.file.SysOssFileVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.OutputStream;

public interface SysOssFileService extends IService<SysOssFile> {

    boolean saveIfAbsent(OssFileRecordRetryData data);

    IPage<SysOssFileVo> listFiles(OssFileQuery query, PageParam pageParam);

    SysOssFile getDownloadFile(Long id);

    void download(SysOssFile file, OutputStream outputStream);

    void deleteById(Long id);

    boolean deleteByUrl(String url);
}
