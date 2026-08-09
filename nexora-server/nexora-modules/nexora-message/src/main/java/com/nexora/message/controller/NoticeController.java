package com.nexora.message.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.nexora.annotation.OperationLogger;
import com.nexora.message.biz.NoticeBizService;
import com.nexora.message.domain.form.NoticeForm;
import com.nexora.message.domain.form.NoticeQueryForm;
import com.nexora.message.domain.vo.NoticeAdminVo;
import com.nexora.message.domain.vo.NoticeUnreadVo;
import com.nexora.message.domain.vo.NoticeUserVo;
import com.nexora.message.domain.vo.NotificationTicketVo;
import com.nexora.message.websocket.NotificationTicketStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/notice")
@RequiredArgsConstructor
@Tag(name = "System notifications")
public class NoticeController {
    private final NoticeBizService noticeBizService;
    private final NotificationTicketStore ticketStore;

    @GetMapping
    @Operation(summary = "List notifications for administrators")
    @SaCheckPermission("sys:notice:list")
    public Result<?> adminPage(NoticeQueryForm form, PageParam pageParam) {
        return Result.data(noticeBizService.adminPage(form, pageParam));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification details")
    @SaCheckPermission("sys:notice:list")
    public Result<NoticeAdminVo> adminDetail(@PathVariable Long id) {
        return Result.data(noticeBizService.adminDetail(id));
    }

    @PostMapping("/add")
    @Operation(summary = "Save a notification draft")
    @OperationLogger("新增系统通知草稿")
    @SaCheckPermission("sys:notice:add")
    public Result<Long> add(@RequestBody NoticeForm form) {
        return Result.data(noticeBizService.addDraft(form));
    }

    @PutMapping("/update")
    @Operation(summary = "Update a notification draft")
    @OperationLogger("修改系统通知草稿")
    @SaCheckPermission("sys:notice:update")
    public Result<Void> update(@RequestBody NoticeForm form) {
        noticeBizService.updateDraft(form);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a notification draft")
    @OperationLogger("删除系统通知草稿")
    @SaCheckPermission("sys:notice:delete")
    public Result<Void> delete(@PathVariable Long id) {
        noticeBizService.deleteDraft(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a notification")
    @OperationLogger("发布系统通知")
    @SaCheckPermission("sys:notice:publish")
    public Result<Void> publish(@PathVariable Long id) {
        noticeBizService.publish(id);
        return Result.success();
    }

    @GetMapping("/my")
    @Operation(summary = "List notifications for current user")
    public Result<?> myPage(@RequestParam(defaultValue = "false") boolean unreadOnly,
                            PageParam pageParam) {
        return Result.data(noticeBizService.userPage(unreadOnly, pageParam));
    }

    @GetMapping("/my/unread-count")
    @Operation(summary = "Get unread notification count")
    public Result<NoticeUnreadVo> unreadCount() {
        return Result.data(new NoticeUnreadVo(noticeBizService.unreadCount()));
    }

    @GetMapping("/my/{id}")
    @Operation(summary = "Get a notification for current user")
    public Result<NoticeUserVo> myDetail(@PathVariable Long id) {
        return Result.data(noticeBizService.userDetail(id));
    }

    @PostMapping("/my/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public Result<Void> markRead(@PathVariable Long id) {
        noticeBizService.markRead(id);
        return Result.success();
    }

    @PostMapping("/my/read-all")
    @Operation(summary = "Mark all notifications as read")
    public Result<Void> markAllRead() {
        noticeBizService.markAllRead();
        return Result.success();
    }

    @GetMapping("/my/announcements/pending")
    @Operation(summary = "Get unread announcements for current login session")
    public Result<List<NoticeUserVo>> pendingAnnouncements() {
        return Result.data(noticeBizService.claimLoginAnnouncements());
    }

    @PostMapping("/my/announcements/read")
    @Operation(summary = "Acknowledge announcements")
    public Result<Void> acknowledgeAnnouncements(@RequestBody List<Long> noticeIds) {
        noticeBizService.acknowledgeAnnouncements(noticeIds);
        return Result.success();
    }

    @PostMapping("/ws-ticket")
    @Operation(summary = "Issue a short-lived notification WebSocket ticket")
    public Result<NotificationTicketVo> issueWebSocketTicket() {
        NotificationTicketStore.Ticket ticket = ticketStore.issue(
                com.aurora.starter.security.context.SecurityUtils.getLoginIdAsInt());
        return Result.data(new NotificationTicketVo(ticket.value(), ticket.expiresAt()));
    }
}
