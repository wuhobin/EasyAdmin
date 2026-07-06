package com.aurora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.aurora.annotation.OperationLogger;
import com.aurora.common.Result;
import com.aurora.entity.SysDictData;
import com.aurora.service.SysDictDataService;
import com.aurora.starter.mybatisplus.model.PageParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "字典数据管理", description = "字典数据相关接口")
@RestController
@RequestMapping("/sys/dictData")
@RequiredArgsConstructor
public class SysDictDataController {

    private final SysDictDataService sysDictDataService;

    @GetMapping("list")
    @Operation(summary = "获取字典数据列表")
    public Result<IPage<SysDictData>> listDictData(Long dictId, PageParam pageParam) {
        return Result.success(sysDictDataService.listDictData(dictId, pageParam));
    }

    @PostMapping("add")
    @Operation(summary = "新增字典数据")
    @OperationLogger(value = "新增字典数据")
    @SaCheckPermission("sys:dict:add")
    public Result<Void> addDictData(@RequestBody SysDictData dictData) {
        sysDictDataService.addDictData(dictData);
        return Result.success();
    }

    @PutMapping("update")
    @Operation(summary = "修改字典数据")
    @OperationLogger(value = "修改字典数据")
    @SaCheckPermission("sys:dict:update")
    public Result<Void> updateDictData(@RequestBody SysDictData dictData) {
        sysDictDataService.updateDictData(dictData);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "删除字典数据")
    @OperationLogger(value = "删除字典数据")
    @SaCheckPermission("sys:dict:delete")
    public Result<Void> delete(@PathVariable List<Long> ids) {
        sysDictDataService.removeBatchByIds(ids);
        return Result.success();
    }
} 