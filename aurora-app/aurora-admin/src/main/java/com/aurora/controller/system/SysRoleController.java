package com.aurora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.annotation.OperationLogger;
import com.aurora.biz.SysRoleBizService;
import com.aurora.domain.form.query.system.SysRoleQueryForm;
import com.aurora.domain.form.system.SysRoleForm;
import com.aurora.domain.vo.system.SysRoleVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
@Tag(name = "Role management")
public class SysRoleController {
    private final SysRoleBizService sysRoleBizService;

    @GetMapping("/")
    @Operation(summary = "获取角色列表")
    public Result<IPage<SysRoleVo>> listRoles(SysRoleQueryForm form, PageParam pageParam) {
        return Result.data(sysRoleBizService.list(form, pageParam));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    public Result<SysRoleVo> getRole(@PathVariable Integer id) { return Result.data(sysRoleBizService.get(id)); }

    @PostMapping("/")
    @Operation(summary = "新增角色")
    @OperationLogger("新增角色")
    @SaCheckPermission("sys:role:add")
    public Result<Void> addRole(@RequestBody SysRoleForm form) {
        sysRoleBizService.add(form);
        return Result.success();
    }

    @PutMapping("/")
    @Operation(summary = "修改角色")
    @OperationLogger("修改角色")
    @SaCheckPermission("sys:role:update")
    public Result<Void> updateRole(@RequestBody SysRoleForm form) {
        sysRoleBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "批量删除角色")
    @OperationLogger("批量删除角色")
    @SaCheckPermission("sys:role:delete")
    public Result<Void> delete(@PathVariable List<Integer> ids) {
        sysRoleBizService.delete(ids);
        return Result.success();
    }

    @GetMapping("/menus/{id}")
    @Operation(summary = "获取角色权限")
    public Result<List<Integer>> getRoleMenus(@PathVariable Integer id) { return Result.data(sysRoleBizService.getRoleMenus(id)); }

    @PutMapping("/menus/{id}")
    @Operation(summary = "修改角色权限")
    @OperationLogger("修改角色权限")
    @SaCheckPermission("sys:role:menus")
    public Result<Void> updateRoleMenus(@PathVariable Integer id, @RequestBody List<Integer> menuIds) {
        sysRoleBizService.updateRoleMenus(id, menuIds);
        return Result.success();
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有角色")
    public Result<List<SysRoleVo>> all() { return Result.data(sysRoleBizService.all()); }

    @GetMapping("/export")
    @Operation(summary = "导出角色")
    public void export(HttpServletResponse response) throws IOException { sysRoleBizService.export(response); }
}
