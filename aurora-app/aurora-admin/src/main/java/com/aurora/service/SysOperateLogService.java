package com.aurora.service;

import com.aurora.entity.SysOperateLog;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 *  服务接口
 */
public interface SysOperateLogService extends IService<SysOperateLog> {
    /**
     * 查询分页列表
     */
    IPage<SysOperateLog> listSysOperateLog(SysOperateLog sysOperateLog, PageParam pageParam);
}
