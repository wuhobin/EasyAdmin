package com.nexora.service.impl;

import com.nexora.domain.query.monitor.QuartzJobQuery;
import com.nexora.service.JobService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.quartz.domain.QuartzJob;
import com.aurora.starter.quartz.service.IQuartzJobService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final IQuartzJobService quartzJobService;

    @Override
    public IPage<QuartzJob> list(QuartzJobQuery query, PageParam pageParam) {
        return quartzJobService.page(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public QuartzJob get(Long jobId) {
        return quartzJobService.getById(jobId);
    }
}
