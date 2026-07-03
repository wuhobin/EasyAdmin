package com.aurora.service.impl;

import com.aurora.service.IJobLogService;
import com.aurora.starter.quartz.domain.QuartzJobLog;
import com.aurora.starter.quartz.mapper.QuartzJobLogMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobLogServiceImpl extends ServiceImpl<QuartzJobLogMapper, QuartzJobLog> implements IJobLogService {

    @Override
    public void cleanJobLog() {
        baseMapper.delete(null);
    }
}
