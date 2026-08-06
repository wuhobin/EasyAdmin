package com.nexora.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.monitor.biz.ManagedServerBizService;
import com.nexora.monitor.domain.form.ManagedServerForm;
import com.nexora.monitor.domain.form.ManagedServerQueryForm;
import com.nexora.monitor.domain.form.ServerFingerprintForm;
import com.nexora.monitor.domain.form.ServerPasswordForm;
import com.nexora.monitor.domain.form.TerminalTicketForm;
import com.nexora.monitor.domain.vo.ManagedServerVo;
import com.nexora.monitor.domain.vo.ServerConnectionTestVo;
import com.nexora.monitor.domain.vo.TerminalTicketVo;
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

@Tag(name = "服务器管理")
@RestController
@RequestMapping("/monitor/server")
@RequiredArgsConstructor
public class ManagedServerController {

    private final ManagedServerBizService serverBizService;

    @GetMapping("/list")
    @SaCheckPermission("monitor:server:list")
    public Result<IPage<ManagedServerVo>> list(
            ManagedServerQueryForm form, PageParam pageParam) {
        return Result.data(serverBizService.list(form, pageParam));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("monitor:server:list")
    public Result<ManagedServerVo> get(@PathVariable Long id) {
        return Result.data(serverBizService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增服务器")
    @SaCheckPermission("monitor:server:add")
    public Result<ManagedServerVo> add(@Valid @RequestBody ManagedServerForm form) {
        return Result.data(serverBizService.add(form));
    }

    @PutMapping
    @Operation(summary = "修改服务器")
    @SaCheckPermission("monitor:server:update")
    public Result<Void> update(@Valid @RequestBody ManagedServerForm form) {
        serverBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除服务器")
    @SaCheckPermission("monitor:server:delete")
    public Result<Void> delete(@PathVariable Long id) {
        serverBizService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试 SSH 连接")
    @SaCheckPermission("monitor:server:test")
    public Result<ServerConnectionTestVo> test(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ServerPasswordForm form) {
        return Result.data(serverBizService.test(id, form));
    }

    @PostMapping("/{id}/fingerprint")
    @Operation(summary = "确认 SSH 主机指纹")
    @SaCheckPermission("monitor:server:fingerprint")
    public Result<Void> confirmFingerprint(
            @PathVariable Long id,
            @Valid @RequestBody ServerFingerprintForm form) {
        serverBizService.confirmFingerprint(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}/fingerprint")
    @Operation(summary = "重置 SSH 主机指纹")
    @SaCheckPermission("monitor:server:fingerprint")
    public Result<Void> resetFingerprint(@PathVariable Long id) {
        serverBizService.resetFingerprint(id);
        return Result.success();
    }

    @PostMapping("/{id}/terminal-ticket")
    @Operation(summary = "签发一次性 SSH 终端票据")
    @SaCheckPermission("monitor:server:terminal")
    public Result<TerminalTicketVo> terminalTicket(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) TerminalTicketForm form) {
        return Result.data(serverBizService.issueTerminalTicket(id, form));
    }
}
