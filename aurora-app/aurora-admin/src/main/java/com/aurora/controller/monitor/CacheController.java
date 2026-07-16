package com.aurora.controller.monitor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.service.CacheService;
import com.aurora.vo.cache.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "缓存监控", description = "Redis缓存监控相关接口")
@RestController
@RequestMapping("/monitor/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @Operation(summary = "获取缓存信息")
    @GetMapping("/info")
    public Result<CacheInfoVo> getCacheInfo() {
        return Result.data(cacheService.getCacheInfo());
    }

    @Operation(summary = "获取内存信息")
    @GetMapping("/memory")
    public Result<CacheMemoryVo> getMemoryInfo() {
        return Result.data(cacheService.getMemoryInfo());
    }

    @Operation(summary = "获取缓存键列表")
    @GetMapping("/keys")
    public Result<IPage<CacheKeyVo>> getKeyList(CacheKeyQuery query) {
        return Result.data(cacheService.getKeyList(query));
    }

    @Operation(summary = "清空缓存")
    @DeleteMapping
    public Result<Void> clearCache() {
        cacheService.clearCache();
        return Result.success();
    }
}
