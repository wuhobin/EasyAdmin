package com.nexora.service;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.domain.query.system.SysConfigQuery;
import com.nexora.entity.SysConfig;

public interface SysConfigService extends IService<SysConfig> {

    IPage<SysConfig> getConfigPageList(SysConfigQuery query, PageParam pageParam);

    boolean existsByConfigKey(String configKey);

    String getValueByConfigKey(String configKey);
}
