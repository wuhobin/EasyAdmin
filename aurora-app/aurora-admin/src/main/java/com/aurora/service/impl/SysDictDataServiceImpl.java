package com.aurora.service.impl;

import com.aurora.service.SysDictDataService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.entity.SysDictData;
import com.aurora.mapper.SysDictDataMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * 字典数据表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    @Override
    public IPage<SysDictData> listDictData(Long dictId, PageParam pageParam) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        // 构建查询条件
        wrapper.eq(SysDictData::getDictId,dictId)
                .orderByAsc(SysDictData::getSort);
        return page(PageUtils.buildPage(pageParam), wrapper);
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