package com.aurora.controller.mail;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.biz.MailInboxBizService;
import com.aurora.domain.vo.mail.MailMessageDetailVo;
import com.aurora.domain.vo.mail.MailMessageSummaryVo;
import com.aurora.starter.webmvc.domain.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "聚合收件箱")
@RestController
@RequestMapping("/mail/inbox")
@RequiredArgsConstructor
public class MailInboxController {
    private final MailInboxBizService mailInboxBizService;

    @GetMapping("/list")
    @Operation(summary = "读取最新邮件")
    @SaCheckPermission("mail:inbox:list")
    public Result<List<MailMessageSummaryVo>> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Integer limit) {
        return Result.data(mailInboxBizService.list(accountId, limit));
    }

    @GetMapping("/detail")
    @Operation(summary = "读取邮件详情")
    @SaCheckPermission("mail:inbox:view")
    public Result<MailMessageDetailVo> detail(@RequestParam Long accountId,
                                               @RequestParam long uid,
                                               @RequestParam long uidValidity) {
        return Result.data(mailInboxBizService.getDetail(accountId, uid, uidValidity));
    }

    @GetMapping("/attachment")
    @Operation(summary = "下载邮件附件")
    @SaCheckPermission("mail:inbox:download")
    public void attachment(@RequestParam Long accountId,
                           @RequestParam long uid,
                           @RequestParam long uidValidity,
                           @RequestParam String partId,
                           HttpServletResponse response) {
        mailInboxBizService.downloadAttachment(accountId, uid, uidValidity, partId, response);
    }
}
