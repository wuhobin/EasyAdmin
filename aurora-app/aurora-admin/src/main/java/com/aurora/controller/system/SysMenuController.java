package com.aurora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.annotation.OperationLogger;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.entity.SysMenu;
import com.aurora.service.SysMenuService;
import com.aurora.domain.vo.menu.RouterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
@Tag(name = "菜单管理", description = "系统菜单相关接口")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/tree")
    @Operation(summary = "获取菜单树列表")
    public Result<List<SysMenu>> getMenuTree() {
        return Result.data(sysMenuService.getMenuTree());
    }

    @PostMapping
    @Operation(summary = "添加菜单")
    @OperationLogger(value = "添加菜单")
    @SaCheckPermission("sys:menu:add")
    public Result<Void> addMenu(@RequestBody SysMenu menu) {
        sysMenuService.addMenu(menu);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改菜单")
    @OperationLogger(value = "修改菜单")
    @SaCheckPermission("sys:menu:update")
    public Result<Void> updateMenu(@RequestBody SysMenu menu) {
        sysMenuService.updateMenu(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    @OperationLogger(value = "删除菜单")
    @SaCheckPermission("sys:menu:delete")
    public Result<Void> deleteMenu(@PathVariable Integer id) {
        sysMenuService.deleteMenu(id);
        return Result.success();
    }

    @GetMapping(value = "/routers")
    @Operation(summary = "获取用户菜单")
    public Result<List<RouterVO>> getCurrentUserMenu() {
        return Result.data(sysMenuService.getCurrentUserMenu());
    }
}
