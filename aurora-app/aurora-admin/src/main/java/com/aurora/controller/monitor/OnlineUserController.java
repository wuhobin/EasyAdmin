package com.aurora.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.biz.OnlineUserBizService;
import com.aurora.domain.form.query.monitor.OnlineUserQueryForm;
import com.aurora.domain.vo.user.OnlineUserVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/online")
@Tag(name = "Online users")
public class OnlineUserController {
    private final OnlineUserBizService onlineUserBizService;

    @GetMapping("/list")
    @Operation(summary = "获取在线用户列表")
    public Result<IPage<OnlineUserVo>> getOnlineUserList(OnlineUserQueryForm form, PageParam pageParam) {
        return Result.data(onlineUserBizService.list(form, pageParam));
    }

    @GetMapping("/forceLogout/{token}")
    @Operation(summary = "强制退出")
    @SaCheckPermission("monitor:online:forceLogout")
    public Result<Void> forceLogout(@PathVariable String token) {
        SecurityUtils.logoutByTokenValue(token);
        return Result.success();
    }
}
