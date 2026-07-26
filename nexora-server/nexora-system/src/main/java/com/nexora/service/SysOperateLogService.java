package com.nexora.service;

import com.nexora.entity.SysOperateLog;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.domain.query.system.SysOperateLogQuery;

/**
 *  服务接口
 */
public interface SysOperateLogService extends IService<SysOperateLog> {
    /**
     * 查询分页列表
     */
    IPage<SysOperateLog> listSysOperateLog(SysOperateLogQuery query, PageParam pageParam);
}
