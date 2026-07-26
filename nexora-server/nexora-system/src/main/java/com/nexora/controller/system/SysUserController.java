package com.nexora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.annotation.OperationLogger;
import com.nexora.biz.SysUserBizService;
import com.nexora.domain.form.query.system.SysUserQueryForm;
import com.nexora.domain.form.system.ResetPasswordForm;
import com.nexora.domain.form.system.SysUserForm;
import com.nexora.domain.form.system.UpdatePasswordForm;
import com.nexora.domain.form.system.UserProfileForm;
import com.nexora.domain.vo.user.SysUserPageListVo;
import com.nexora.domain.vo.user.SysUserProfileVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
@Tag(name = "User management")
public class SysUserController {
    private final SysUserBizService sysUserBizService;

    @GetMapping
    @Operation(summary = "获取用户列表")
    public Result<IPage<SysUserPageListVo>> listUsers(SysUserQueryForm form, PageParam pageParam) {
        return Result.data(sysUserBizService.list(form, pageParam));
    }

    @PostMapping
    @Operation(summary = "新增用户")
    @OperationLogger("新增用户")
    @SaCheckPermission("sys:user:add")
    public Result<Void> addUser(@RequestBody SysUserForm form) {
        sysUserBizService.add(form);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改用户")
    @OperationLogger("修改用户")
    @SaCheckPermission("sys:user:update")
    public Result<Void> update(@RequestBody SysUserForm form) {
        sysUserBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @Operation(summary = "批量删除用户")
    @OperationLogger("批量删除用户")
    @SaCheckPermission("sys:user:delete")
    public Result<Void> delete(@PathVariable List<Integer> ids) {
        sysUserBizService.delete(ids);
        return Result.success();
    }

    @PutMapping("/updatePwd")
    @Operation(summary = "修改密码")
    @OperationLogger("修改密码")
    public Result<Void> updatePwd(@RequestBody UpdatePasswordForm form) {
        sysUserBizService.updatePassword(form);
        return Result.success();
    }

    @GetMapping("/profile")
    @Operation(summary = "获取个人信息")
    public Result<SysUserProfileVo> profile() { return Result.data(sysUserBizService.profile()); }

    @PutMapping("/updProfile")
    @Operation(summary = "修改个人信息")
    @OperationLogger("修改个人信息")
    public Result<Void> updateProfile(@RequestBody UserProfileForm form) {
        sysUserBizService.updateProfile(form);
        return Result.success();
    }

    @GetMapping("/verifyPassword/{password}")
    @Operation(summary = "验证密码")
    public Result<Boolean> verifyPassword(@PathVariable String password) {
        return Result.data(sysUserBizService.verifyPassword(password));
    }

    @PutMapping("/reset")
    @Operation(summary = "重置密码")
    @OperationLogger("重置密码")
    public Result<Boolean> resetPassword(@RequestBody ResetPasswordForm form) {
        return Result.data(sysUserBizService.resetPassword(form));
    }
}
