package com.aurora.service;

import com.aurora.domain.vo.cache.CacheInfoVo;
import com.aurora.domain.query.monitor.CacheKeyQuery;
import com.aurora.domain.vo.cache.CacheKeyVo;
import com.aurora.domain.vo.cache.CacheMemoryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface CacheService {
    
    /**
     * 获取缓存基本信息
     */
    CacheInfoVo getCacheInfo();
    
    /**
     * 获取内存信息
     */
    CacheMemoryVo getMemoryInfo();
    
    /**
     * 获取缓存键列表
     */
    IPage<CacheKeyVo> getKeyList(CacheKeyQuery query);
    
    /**
     * 清空缓存
     */
    void clearCache();
}
