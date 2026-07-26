package com.nexora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.nexora.domain.query.system.SysDictQuery;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.nexora.entity.SysDict;
import com.nexora.mapper.SysDictMapper;
import com.nexora.service.SysDictService;
import org.springframework.stereotype.Service;

@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictService {

    @Override
    public IPage<SysDict> getDictPageList(SysDictQuery query, PageParam pageParam) {
        return baseMapper.selectPage(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean existsByType(String type, Long excludeId) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getType, type);
        if (excludeId != null) {
            wrapper.ne(SysDict::getId, excludeId);
        }
        return baseMapper.selectCount(wrapper) > 0;
    }
}
