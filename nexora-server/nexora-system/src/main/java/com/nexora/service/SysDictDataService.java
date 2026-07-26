package com.nexora.service;

import com.nexora.entity.SysDictData;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.domain.query.system.SysDictDataQuery;

/**
 * 字典数据表 服务接口
 */
public interface SysDictDataService extends IService<SysDictData> {
    /**
     * 查询字典数据分页列表
     */
    IPage<SysDictData> listDictData(SysDictDataQuery query, PageParam pageParam);
    
    /**
     * 新增字典数据
     */
}
