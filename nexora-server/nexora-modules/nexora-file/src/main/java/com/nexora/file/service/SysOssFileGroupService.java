package com.nexora.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.file.entity.SysOssFileGroup;

import java.util.List;

public interface SysOssFileGroupService extends IService<SysOssFileGroup> {

    List<SysOssFileGroup> listByOwnerId(Long ownerId);

    Long countUngrouped(Long ownerId);
}
