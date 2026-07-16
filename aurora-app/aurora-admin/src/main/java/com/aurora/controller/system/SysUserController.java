package com.aurora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.aurora.annotation.OperationLogger;
import com.aurora.dto.user.UserSaveOrUpdateDto;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.dto.user.UpdatePwdDTO;
import com.aurora.entity.SysUser;
import com.aurora.service.SysUserService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.vo.user.SysUserPageListVo;
import com.aurora.vo.user.SysUserProfileVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "系统用户相关接口")
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    @Operation(summary = "获取用户列表")
    public Result<IPage<SysUserPageListVo>> listUsers(@RequestParam(required = false) String nickname,
                                                      @RequestParam(required = false) Integer status,
                                                      PageParam pageParam) {
        return Result.data(sysUserService.listUsers(nickname, status, pageParam));
    }

    @PostMapping
    @OperationLogger("新增用户")
    @Operation(summary = "新增用户")
    @SaCheckPermission("sys:user:add")
    public Result<Void> addUser(@RequestBody UserSaveOrUpdateDto sysUserAddDto) {
        sysUserService.add(sysUserAddDto);
        return Result.success();
    }

    @PutMapping
    @OperationLogger("修改用户")
    @Operation(summary = "修改用户")
    @SaCheckPermission("sys:user:update")
    public Result<Void> update(@RequestBody UserSaveOrUpdateDto user) {
        sysUserService.update(user);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @OperationLogger("批量删除用户")
    @Operation(summary = "批量删除用户")
    @SaCheckPermission("sys:user:delete")
    public Result<Void> delete(@PathVariable List<Integer> ids) {
        sysUserService.delete(ids);
        return Result.success();
    }

    @PutMapping("/updatePwd")
    @OperationLogger("修改密码")
    @Operation(summary = "修改密码")
    @SaCheckPermission("sys:user:reset")
    public Result<Void> updatePwd(@RequestBody UpdatePwdDTO updatePwdDTO) {
        sysUserService.updatePwd(updatePwdDTO);
        return Result.success();
    }

    @GetMapping("/profile")
    @Operation(summary = "获取个人信息")
    public Result<SysUserProfileVo> profile() {
        return Result.data(sysUserService.profile());
    }

    @PutMapping("/updProfile")
    @OperationLogger("修改个人信息")
    @Operation(summary = "修改个人信息")
    @SaCheckPermission("sys:user:update")
    public Result<SysUserProfileVo> updateProfile(@RequestBody SysUser user) {
        sysUserService.updateProfile(user);
        return Result.success();
    }

    @GetMapping("/verifyPassword/{password}")
    @Operation(summary = "锁屏界面验证密码")
    public Result<Boolean> verifyPassword(@PathVariable String password) {
        return Result.data(sysUserService.verifyPassword(password));
    }

    @PutMapping("/reset")
    @OperationLogger("重置密码")
    @Operation(summary = "重置密码")
    @SaCheckPermission("sys:user:reset")
    public Result<Boolean> resetPassword(@RequestBody SysUser user) {
        return Result.data(sysUserService.resetPassword(user));
    }
}
