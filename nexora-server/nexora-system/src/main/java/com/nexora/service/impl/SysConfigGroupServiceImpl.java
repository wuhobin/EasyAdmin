package com.nexora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.entity.SysConfigGroup;
import com.nexora.mapper.SysConfigGroupMapper;
import com.nexora.service.SysConfigGroupService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysConfigGroupServiceImpl extends ServiceImpl<SysConfigGroupMapper, SysConfigGroup>
        implements SysConfigGroupService {

    @Override
    public List<SysConfigGroup> listOrdered() {
        return baseMapper.selectList(new LambdaQueryWrapper<SysConfigGroup>()
                .orderByAsc(SysConfigGroup::getSort)
                .orderByAsc(SysConfigGroup::getId));
    }

    @Override
    public SysConfigGroup getByGroupCode(String groupCode) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SysConfigGroup>()
                .eq(SysConfigGroup::getGroupCode, groupCode));
    }

    @Override
    public String getValueByGroupCode(String groupCode) {
        SysConfigGroup group = baseMapper.selectOne(new LambdaQueryWrapper<SysConfigGroup>()
                .select(SysConfigGroup::getConfigValue)
                .eq(SysConfigGroup::getGroupCode, groupCode));
        return group == null ? null : group.getConfigValue();
    }
}
