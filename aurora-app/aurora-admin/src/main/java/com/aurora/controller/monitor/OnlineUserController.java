package com.aurora.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.service.SysUserService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.vo.user.OnlineUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: quequnlong
 * @date: 2025/1/3
 * @description:
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/online")
@Tag(name = "在线用户")
public class OnlineUserController {

    private final SysUserService sysUserService;

    @GetMapping("/list")
    @Operation(summary = "获取在线用户列表")
    public Result<IPage<OnlineUserVo>> getOnlineUserList(String username, PageParam pageParam) {
        return Result.data(sysUserService.getOnlineUserList(username, pageParam));
    }

    @Operation(summary = "强制踢出")
    @GetMapping("/forceLogout/{token}")
    @SaCheckPermission("monitor:online:forceLogout")
    public Result<Void> forceLogout(@PathVariable String token) {
        SecurityUtils.logoutByTokenValue(token);
        return Result.success();
    }

}
