package com.nexora.service.impl;

import com.nexora.domain.query.system.SysOperateLogQuery;
import com.nexora.entity.SysOperateLog;
import com.nexora.mapper.SysOperateLogMapper;
import com.nexora.service.SysOperateLogService;
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
