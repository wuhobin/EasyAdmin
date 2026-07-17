package com.aurora.controller.monitor;

import com.aurora.biz.CacheBizService;
import com.aurora.domain.form.query.monitor.CacheKeyQueryForm;
import com.aurora.domain.vo.cache.CacheInfoVo;
import com.aurora.domain.vo.cache.CacheKeyVo;
import com.aurora.domain.vo.cache.CacheMemoryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.aurora.starter.webmvc.domain.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "缓存监控", description = "Redis缓存监控相关接口")
@RestController
@RequestMapping("/monitor/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheBizService cacheBizService;

    @Operation(summary = "获取缓存信息")
    @GetMapping("/info")
    public Result<CacheInfoVo> getCacheInfo() {
        return Result.data(cacheBizService.getCacheInfo());
    }

    @Operation(summary = "获取内存信息")
    @GetMapping("/memory")
    public Result<CacheMemoryVo> getMemoryInfo() {
        return Result.data(cacheBizService.getMemoryInfo());
    }

    @Operation(summary = "获取缓存键列表")
    @GetMapping("/keys")
    public Result<IPage<CacheKeyVo>> getKeyList(CacheKeyQueryForm form) {
        return Result.data(cacheBizService.getKeyList(form));
    }

    @Operation(summary = "清空缓存")
    @DeleteMapping
    public Result<Void> clearCache() {
        cacheBizService.clearCache();
        return Result.success();
    }
}
