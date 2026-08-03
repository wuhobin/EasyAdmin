package com.nexora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.monitor.annotation.OperationLogger;
import com.nexora.biz.system.SysDictBizService;
import com.nexora.domain.form.query.system.SysDictQueryForm;
import com.nexora.domain.form.system.SysDictForm;
import com.nexora.domain.vo.system.SysDictVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/sys/dict")
@RequiredArgsConstructor
@Tag(name = "Dictionary management")
public class SysDictController {
    private final SysDictBizService sysDictBizService;

    @GetMapping
    @Operation(summary = "List dictionaries")
    public Result<IPage<SysDictVo>> getDictList(SysDictQueryForm form, PageParam pageParam) {
        return Result.data(sysDictBizService.list(form, pageParam));
    }

    @PostMapping("/add")
    @Operation(summary = "Add dictionary")
    @OperationLogger("添加字典")
    @SaCheckPermission("sys:dict:add")
    public Result<Void> addDict(@RequestBody SysDictForm form) {
        sysDictBizService.add(form);
        return Result.success();
    }

    @PutMapping("/update")
    @Operation(summary = "Update dictionary")
    @OperationLogger("修改字典")
    @SaCheckPermission("sys:dict:update")
    public Result<Void> updateDict(@RequestBody SysDictForm form) {
        sysDictBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "Delete dictionaries")
    @OperationLogger("删除字典")
    @SaCheckPermission("sys:dict:delete")
    public Result<Void> delete(@PathVariable List<Long> ids) {
        sysDictBizService.delete(ids);
        return Result.success();
    }
}
