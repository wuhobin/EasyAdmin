package com.nexora.system.service.impl;

import com.nexora.system.domain.query.SysDictDataQuery;
import com.nexora.system.entity.SysDictData;
import com.nexora.system.mapper.SysDictDataMapper;
import com.nexora.system.service.SysDictDataService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData>
        implements SysDictDataService {

    @Override
    public IPage<SysDictData> listDictData(SysDictDataQuery query, PageParam pageParam) {
        return page(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public List<SysDictData> listOrdered(SysDictDataQuery query) {
        return baseMapper.selectOrdered(DynamicCondition.toWrapper(query));
    }
}
