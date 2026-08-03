package com.nexora.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.webmvc.domain.response.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexora.system.biz.SysConfigGroupBizService;
import com.nexora.system.domain.vo.SysConfigGroupDetailVo;
import com.nexora.system.domain.vo.SysConfigGroupSummaryVo;
import com.nexora.system.domain.vo.SysConfigPublicVo;
import com.nexora.system.service.SystemMailSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sys/config-group")
@RequiredArgsConstructor
@Tag(name = "配置管理")
public class SysConfigGroupController {

    private final SysConfigGroupBizService configGroupBizService;
    private final SystemMailSender systemMailSender;

    @GetMapping("/list")
    @SaCheckPermission("sys:config:list")
    @Operation(summary = "查询配置分组")
    public Result<List<SysConfigGroupSummaryVo>> list() {
        return Result.data(configGroupBizService.list());
    }

    @GetMapping("/public")
    @Operation(summary = "查询匿名公开配置")
    public Result<SysConfigPublicVo> publicConfig() {
        return Result.data(configGroupBizService.getPublicConfig());
    }

    @GetMapping("/{groupCode}")
    @SaCheckPermission("sys:config:list")
    @Operation(summary = "查询指定配置分组")
    public Result<SysConfigGroupDetailVo> get(@PathVariable String groupCode) {
        return Result.data(configGroupBizService.get(groupCode));
    }

    @PutMapping("/{groupCode}")
    @SaCheckPermission("sys:config:update")
    @Operation(summary = "更新指定配置分组")
    public Result<Void> update(@PathVariable String groupCode, @RequestBody JsonNode configValue) {
        configGroupBizService.update(groupCode, configValue);
        return Result.success();
    }

    @PostMapping("/refresh")
    @SaCheckPermission("sys:config:update")
    @Operation(summary = "刷新全部配置缓存")
    public Result<Void> refresh() {
        configGroupBizService.refreshCache();
        return Result.success();
    }

    @PostMapping("/test-email")
    @SaCheckPermission("sys:config:update")
    @Operation(summary = "发送测试邮件")
    public Result<Void> testEmail(@RequestParam @NotBlank(message = "测试收件人邮箱不能为空") @Email(message = "测试收件人邮箱格式不正确") @Size(max = 254, message = "测试收件人邮箱不能超过254个字符") String to) {
        systemMailSender.sendTestMail(to);
        return Result.success();
    }
}
