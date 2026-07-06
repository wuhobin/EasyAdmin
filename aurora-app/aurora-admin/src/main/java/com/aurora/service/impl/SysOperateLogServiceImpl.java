package com.aurora.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.aurora.mapper.SysOperateLogMapper;
import com.aurora.entity.SysOperateLog;
import com.aurora.service.SysOperateLogService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;

/**
 *  服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysOperateLogServiceImpl extends ServiceImpl<SysOperateLogMapper, SysOperateLog> implements SysOperateLogService {

    /**
     * 查询分页列表
     */
    @Override
    public IPage<SysOperateLog> listSysOperateLog(SysOperateLog sysOperateLog, PageParam pageParam) {
        LambdaQueryWrapper<SysOperateLog> wrapper = new LambdaQueryWrapper<SysOperateLog>()
                .like(StringUtils.isNotBlank(sysOperateLog.getUsername()), SysOperateLog::getUsername, sysOperateLog.getUsername());

        return page(PageUtils.buildPage(pageParam), wrapper);
    }
}
