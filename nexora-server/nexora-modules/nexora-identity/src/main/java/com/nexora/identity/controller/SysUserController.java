package com.nexora.identity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.annotation.OperationLogger;
import com.nexora.identity.biz.SysUserBizService;
import com.nexora.identity.domain.form.SysUserQueryForm;
import com.nexora.identity.domain.form.SysUserForm;
import com.nexora.identity.domain.vo.SysUserPageListVo;
import com.nexora.identity.domain.vo.SysUserProfileVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    public Result<Void> addUser(@Valid @RequestBody SysUserForm form) {
        sysUserBizService.add(form);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改用户")
    @OperationLogger("修改用户")
    @SaCheckPermission("sys:user:update")
    public Result<Void> update(@Valid @RequestBody SysUserForm form) {
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
    public Result<Void> updatePwd(@Valid @RequestBody SysUserForm form) {
        sysUserBizService.updatePassword(form);
        return Result.success();
    }

    @GetMapping("/profile")
    @Operation(summary = "获取个人信息")
    public Result<SysUserProfileVo> profile() { return Result.data(sysUserBizService.profile()); }

    @PutMapping("/updProfile")
    @Operation(summary = "修改个人信息")
    @OperationLogger("修改个人信息")
    public Result<Void> updateProfile(@Valid @RequestBody SysUserForm form) {
        sysUserBizService.updateProfile(form);
        return Result.success();
    }

    @PostMapping("/profile/email/sendCode")
    @Operation(summary = "发送换绑邮箱验证码")
    public Result<Void> sendEmailCode(@Valid @RequestBody SysUserForm form) {
        sysUserBizService.sendEmailCode(form);
        return Result.success();
    }

    @PutMapping("/profile/changeEmail")
    @Operation(summary = "修改当前用户邮箱")
    public Result<Void> changeEmail(@Valid @RequestBody SysUserForm form) {
        sysUserBizService.changeEmail(form);
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
    public Result<Boolean> resetPassword(@Valid @RequestBody SysUserForm form) {
        return Result.data(sysUserBizService.resetPassword(form));
    }

    @PutMapping("/audit/{id}")
    @Operation(summary = "审核通过待审核用户")
    @OperationLogger("审核通过用户")
    @SaCheckPermission("sys:user:update")
    public Result<Void> audit(@PathVariable Integer id) {
        sysUserBizService.audit(id);
        return Result.success();
    }
}
