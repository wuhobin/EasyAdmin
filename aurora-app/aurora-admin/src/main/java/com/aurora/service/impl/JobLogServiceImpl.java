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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobLogServiceImpl extends ServiceImpl<QuartzJobLogMapper, QuartzJobLog> implements IJobLogService {

    @Override
    public IPage<QuartzJobLog> list(QuartzJobLogQuery query, PageParam pageParam) {
        PageParam result = pageParam == null ? new PageParam() : pageParam;
        if (result.getPageNum() == null) result.setPageNum(PageParam.DEFAULT_PAGE);
        if (result.getPageSize() == null) result.setPageSize(PageParam.DEFAULT_SIZE);
        if (result.getOrderBy() == null || result.getOrderBy().isBlank()) result.setOrderBy("start_time desc");
        return page(PageUtils.buildPage(result), DynamicCondition.toWrapper(query));
    }

    @Override
    public void cleanJobLog() {
        baseMapper.delete(null);
    }
}
