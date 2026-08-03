package com.nexora.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.monitor.annotation.OperationLogger;
import com.nexora.system.biz.SysDictDataBizService;
import com.nexora.system.domain.form.SysDictDataForm;
import com.nexora.system.domain.form.SysDictDataQueryForm;
import com.nexora.system.domain.vo.SysDictDataVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/sys/dictData")
@RequiredArgsConstructor
@Tag(name = "Dictionary data management")
public class SysDictDataController {
    private final SysDictDataBizService sysDictDataBizService;

    @GetMapping("list")
    @Operation(summary = "List dictionary data")
    public Result<IPage<SysDictDataVo>> listDictData(SysDictDataQueryForm form, PageParam pageParam) {
        return Result.data(sysDictDataBizService.list(form, pageParam));
    }

    @PostMapping("add")
    @Operation(summary = "Add dictionary data")
    @OperationLogger("新增字典数据")
    @SaCheckPermission("sys:dict:add")
    public Result<Void> addDictData(@RequestBody SysDictDataForm form) {
        sysDictDataBizService.add(form);
        return Result.success();
    }

    @PutMapping("update")
    @Operation(summary = "Update dictionary data")
    @OperationLogger("修改字典数据")
    @SaCheckPermission("sys:dict:update")
    public Result<Void> updateDictData(@RequestBody SysDictDataForm form) {
        sysDictDataBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "Delete dictionary data")
    @OperationLogger("删除字典数据")
    @SaCheckPermission("sys:dict:delete")
    public Result<Void> delete(@PathVariable List<Long> ids) {
        sysDictDataBizService.delete(ids);
        return Result.success();
    }
}
