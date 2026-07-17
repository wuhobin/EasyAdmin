package com.aurora.service.impl;

import com.aurora.domain.query.system.SysDictDataQuery;
import com.aurora.entity.SysDictData;
import com.aurora.mapper.SysDictDataMapper;
import com.aurora.service.SysDictDataService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData>
        implements SysDictDataService {

    @Override
    public IPage<SysDictData> listDictData(SysDictDataQuery query, PageParam pageParam) {
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy("sort asc");
        }
        return page(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean addDictData(SysDictData sysDictData) {
        return save(sysDictData);
    }

    @Override
    public boolean updateDictData(SysDictData sysDictData) {
        return updateById(sysDictData);
    }
}
