package com.nexora.controller.mail;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.biz.MailAccountBizService;
import com.nexora.domain.form.mail.MailAccountForm;
import com.nexora.domain.vo.mail.MailAccountVo;
import com.nexora.domain.vo.mail.MailProviderVo;
import com.aurora.starter.webmvc.domain.response.Result;
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

import java.util.List;

@Tag(name = "邮箱账户")
@RestController
@RequestMapping("/mail/account")
@RequiredArgsConstructor
public class MailAccountController {
    private final MailAccountBizService mailAccountBizService;

    @GetMapping("/list")
    @SaCheckPermission("mail:account:list")
    public Result<List<MailAccountVo>> list() {
        return Result.data(mailAccountBizService.list());
    }

    @GetMapping("/providers")
    @Operation(summary = "读取邮箱类型配置")
    @SaCheckPermission("mail:account:list")
    public Result<List<MailProviderVo>> providers() {
        return Result.data(mailAccountBizService.listProviders());
    }

    @PostMapping
    @Operation(summary = "新增邮箱账户")
    @SaCheckPermission("mail:account:add")
    public Result<MailAccountVo> add(@Valid @RequestBody MailAccountForm form) {
        return Result.data(mailAccountBizService.add(form));
    }

    @PutMapping
    @Operation(summary = "修改邮箱账户")
    @SaCheckPermission("mail:account:update")
    public Result<Void> update(@Valid @RequestBody MailAccountForm form) {
        mailAccountBizService.update(form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除邮箱账户")
    @SaCheckPermission("mail:account:delete")
    public Result<Void> delete(@PathVariable Long id) {
        mailAccountBizService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试邮箱连接")
    @SaCheckPermission("mail:account:test")
    public Result<Void> test(@PathVariable Long id) {
        mailAccountBizService.test(id);
        return Result.success();
    }
}
