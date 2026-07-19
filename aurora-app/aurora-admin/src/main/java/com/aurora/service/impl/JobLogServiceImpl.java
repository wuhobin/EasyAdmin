package com.aurora.service.impl;

import com.aurora.service.IJobLogService;
import com.aurora.starter.quartz.domain.QuartzJobLog;
import com.aurora.starter.quartz.mapper.QuartzJobLogMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.domain.query.monitor.QuartzJobLogQuery;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;

@Service
public class JobLogServiceImpl extends ServiceImpl<QuartzJobLogMapper, QuartzJobLog> implements IJobLogService {

    @Override
    public IPage<QuartzJobLog> list(QuartzJobLogQuery query, PageParam pageParam) {
        return page(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public void cleanJobLog() {
        baseMapper.delete(null);
    }
}
