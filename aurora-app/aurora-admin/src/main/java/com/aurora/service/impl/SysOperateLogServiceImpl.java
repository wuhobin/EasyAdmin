package com.aurora.service.impl;

import com.aurora.domain.query.system.SysOperateLogQuery;
import com.aurora.entity.SysOperateLog;
import com.aurora.mapper.SysOperateLogMapper;
import com.aurora.service.SysOperateLogService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysOperateLogServiceImpl extends ServiceImpl<SysOperateLogMapper, SysOperateLog>
        implements SysOperateLogService {

    @Override
    public IPage<SysOperateLog> listSysOperateLog(SysOperateLogQuery query, PageParam pageParam) {
        return page(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }
}
