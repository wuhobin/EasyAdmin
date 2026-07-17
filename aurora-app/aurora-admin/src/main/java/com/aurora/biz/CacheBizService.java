package com.aurora.biz;

import com.aurora.domain.convert.CacheConvert;
import com.aurora.domain.form.query.monitor.CacheKeyQueryForm;
import com.aurora.domain.vo.cache.CacheInfoVo;
import com.aurora.domain.vo.cache.CacheKeyVo;
import com.aurora.domain.vo.cache.CacheMemoryVo;
import com.aurora.service.CacheService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheBizService {
    private final CacheService cacheService;
    public CacheInfoVo getCacheInfo() { return cacheService.getCacheInfo(); }
    public CacheMemoryVo getMemoryInfo() { return cacheService.getMemoryInfo(); }
    public IPage<CacheKeyVo> getKeyList(CacheKeyQueryForm form) {
        return cacheService.getKeyList(CacheConvert.INSTANCE.toQuery(form));
    }
    public void clearCache() { cacheService.clearCache(); }
}
