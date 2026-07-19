package com.aurora.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aurora.entity.SysDict;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.domain.query.system.SysDictQuery;

public interface SysDictService extends IService<SysDict> {
    /**
     * 分页查询字典
     */
    IPage<SysDict> getDictPageList(SysDictQuery query, PageParam pageParam);

    /**
     * 新增字典
     */
    boolean existsByType(String type, Long excludeId);

    /**
     * 更新字典
     */
}
