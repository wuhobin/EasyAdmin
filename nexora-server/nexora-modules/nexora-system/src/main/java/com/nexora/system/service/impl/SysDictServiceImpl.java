package com.nexora.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.nexora.system.domain.query.SysDictQuery;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.nexora.system.entity.SysDict;
import com.nexora.system.mapper.SysDictMapper;
import com.nexora.system.service.SysDictService;
import org.springframework.stereotype.Service;

@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictService {

    @Override
    public IPage<SysDict> getDictPageList(SysDictQuery query, PageParam pageParam) {
        return baseMapper.selectPage(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean existsByType(String type, Long excludeId) {
        if (type == null) {
            return false;
        }
        SysDictQuery query = new SysDictQuery();
        query.setType(type);
        query.setExcludeId(excludeId);
        return baseMapper.selectCount(DynamicCondition.toWrapper(query)) > 0;
    }
}
