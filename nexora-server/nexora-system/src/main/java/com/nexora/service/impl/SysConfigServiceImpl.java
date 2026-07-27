package com.nexora.service.impl;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.domain.query.system.SysConfigQuery;
import com.nexora.entity.SysConfig;
import com.nexora.mapper.SysConfigMapper;
import com.nexora.service.SysConfigService;
import org.springframework.stereotype.Service;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig>
        implements SysConfigService {

    @Override
    public IPage<SysConfig> getConfigPageList(SysConfigQuery query, PageParam pageParam) {
        return baseMapper.selectPage(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean existsByConfigKey(String configKey) {
        return baseMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)) > 0;
    }

    @Override
    public String getValueByConfigKey(String configKey) {
        SysConfig config = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .select(SysConfig::getConfigValue)
                .eq(SysConfig::getConfigKey, configKey));
        return config == null ? null : config.getConfigValue();
    }
}
