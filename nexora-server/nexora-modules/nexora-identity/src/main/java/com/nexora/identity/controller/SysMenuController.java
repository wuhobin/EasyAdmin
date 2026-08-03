package com.nexora.identity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.annotation.OperationLogger;
import com.nexora.identity.biz.SysMenuBizService;
import com.nexora.identity.domain.form.SysMenuForm;
import com.nexora.identity.domain.vo.SysRouterVo;
import com.nexora.identity.domain.vo.SysMenuVo;
import com.aurora.starter.webmvc.domain.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
@Tag(name = "Menu management")
public class SysMenuController {
    private final SysMenuBizService sysMenuBizService;

    @GetMapping("/tree")
    @Operation(summary = "获取菜单树列表")
    public Result<List<SysMenuVo>> getMenuTree() { return Result.data(sysMenuBizService.getMenuTree()); }

    @PostMapping
    @Operation(summary = "添加菜单")
    @OperationLogger("添加菜单")
    @SaCheckPermission("sys:menu:add")
    public Result<Void> addMenu(@RequestBody SysMenuForm form) {
        sysMenuBizService.add(form);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改菜单")
    @OperationLogger("修改菜单")
    @SaCheckPermission("sys:menu:update")
    public Result<Void> updateMenu(@RequestBody SysMenuForm form) {
        sysMenuBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    @OperationLogger("删除菜单")
    @SaCheckPermission("sys:menu:delete")
    public Result<Void> deleteMenu(@PathVariable Integer id) {
        sysMenuBizService.delete(id);
        return Result.success();
    }

    @GetMapping("/routers")
    @Operation(summary = "获取用户菜单")
    public Result<List<SysRouterVo>> getCurrentUserMenu() { return Result.data(sysMenuBizService.getCurrentUserMenu()); }
}
