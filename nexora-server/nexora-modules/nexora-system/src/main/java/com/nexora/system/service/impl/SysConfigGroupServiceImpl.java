package com.nexora.system.service.impl;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.system.domain.query.SysConfigGroupQuery;
import com.nexora.system.entity.SysConfigGroup;
import com.nexora.system.mapper.SysConfigGroupMapper;
import com.nexora.system.service.SysConfigGroupService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysConfigGroupServiceImpl extends ServiceImpl<SysConfigGroupMapper, SysConfigGroup>
        implements SysConfigGroupService {

    @Override
    public List<SysConfigGroup> listOrdered() {
        return baseMapper.selectOrdered(DynamicCondition.toWrapper(new SysConfigGroupQuery()));
    }

    @Override
    public SysConfigGroup getByGroupCode(String groupCode) {
        if (groupCode == null) {
            return null;
        }
        SysConfigGroupQuery query = new SysConfigGroupQuery();
        query.setGroupCode(groupCode);
        return baseMapper.selectOne(DynamicCondition.toWrapper(query));
    }

    @Override
    public String getValueByGroupCode(String groupCode) {
        SysConfigGroup group = getByGroupCode(groupCode);
        return group == null ? null : group.getConfigValue();
    }
}
