package com.nexora.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.file.entity.SysOssFileGroup;
import com.nexora.file.mapper.SysOssFileGroupMapper;
import com.nexora.file.service.SysOssFileGroupService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysOssFileGroupServiceImpl extends ServiceImpl<SysOssFileGroupMapper, SysOssFileGroup>
        implements SysOssFileGroupService {

    @Override
    public List<SysOssFileGroup> listByOwnerId(Long ownerId) {
        return baseMapper.selectByOwnerId(ownerId);
    }

    @Override
    public Long countUngrouped(Long ownerId) {
        return baseMapper.countUngrouped(ownerId);
    }
}
