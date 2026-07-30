package com.nexora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.biz.system.SysConfigBizService;
import com.nexora.domain.form.query.system.SysConfigQueryForm;
import com.nexora.domain.form.system.SysConfigForm;
import com.nexora.domain.vo.system.SysConfigVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sys/config")
@RequiredArgsConstructor
@Tag(name = "配置管理")
public class SysConfigController {

    private final SysConfigBizService sysConfigBizService;

    @GetMapping("/value/{configKey}")
    @Operation(summary = "按配置键获取配置值")
    public Result<String> getValue(@PathVariable String configKey) {
        return Result.data(sysConfigBizService.getValue(configKey));
    }

    @GetMapping
    @SaCheckPermission("sys:config:list")
    @Operation(summary = "分页查询配置")
    public Result<IPage<SysConfigVo>> list(SysConfigQueryForm form, PageParam pageParam) {
        return Result.data(sysConfigBizService.list(form, pageParam));
    }

    @PostMapping("/add")
    @SaCheckPermission("sys:config:add")
    @Operation(summary = "新增配置")
    public Result<Void> add(@Valid @RequestBody SysConfigForm form) {
        sysConfigBizService.add(form);
        return Result.success();
    }

    @PutMapping("/update")
    @SaCheckPermission("sys:config:update")
    @Operation(summary = "修改配置")
    public Result<Void> update(@Valid @RequestBody SysConfigForm form) {
        sysConfigBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("sys:config:delete")
    @Operation(summary = "删除配置")
    public Result<Void> delete(@PathVariable Long id) {
        sysConfigBizService.delete(id);
        return Result.success();
    }
}
