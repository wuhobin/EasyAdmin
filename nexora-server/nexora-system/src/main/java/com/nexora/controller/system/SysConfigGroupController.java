package com.nexora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.webmvc.domain.response.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexora.biz.system.SysConfigGroupBizService;
import com.nexora.domain.vo.system.SysConfigGroupDetailVo;
import com.nexora.domain.vo.system.SysConfigGroupSummaryVo;
import com.nexora.domain.vo.system.SysConfigPublicVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sys/config-group")
@RequiredArgsConstructor
@Tag(name = "配置管理")
public class SysConfigGroupController {

    private final SysConfigGroupBizService configGroupBizService;

    @GetMapping("/list")
    @SaCheckPermission("sys:config:list")
    @Operation(summary = "查询配置分组")
    public Result<List<SysConfigGroupSummaryVo>> list() {
        return Result.data(configGroupBizService.list());
    }

    @GetMapping("/public")
    @Operation(summary = "查询匿名公开配置")
    public Result<SysConfigPublicVo> publicConfig() {
        return Result.data(configGroupBizService.getPublicConfig());
    }

    @GetMapping("/{groupCode}")
    @SaCheckPermission("sys:config:list")
    @Operation(summary = "查询指定配置分组")
    public Result<SysConfigGroupDetailVo> get(@PathVariable String groupCode) {
        return Result.data(configGroupBizService.get(groupCode));
    }

    @PutMapping("/{groupCode}")
    @SaCheckPermission("sys:config:update")
    @Operation(summary = "更新指定配置分组")
    public Result<Void> update(@PathVariable String groupCode, @RequestBody JsonNode configValue) {
        configGroupBizService.update(groupCode, configValue);
        return Result.success();
    }

    @PostMapping("/refresh")
    @SaCheckPermission("sys:config:update")
    @Operation(summary = "刷新全部配置缓存")
    public Result<Void> refresh() {
        configGroupBizService.refreshCache();
        return Result.success();
    }
}
