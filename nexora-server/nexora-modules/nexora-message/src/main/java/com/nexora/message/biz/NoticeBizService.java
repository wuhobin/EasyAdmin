package com.nexora.message.biz;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.service.SysUserService;
import com.nexora.message.domain.form.NoticeForm;
import com.nexora.message.domain.form.NoticeQueryForm;
import com.nexora.message.domain.vo.NoticeAdminVo;
import com.nexora.message.domain.vo.NoticeUserVo;
import com.nexora.message.entity.SysNotice;
import com.nexora.message.entity.SysUserNotice;
import com.nexora.message.mapper.SysUserNoticeMapper;
import com.nexora.message.service.AnnouncementSessionClaimStore;
import com.nexora.message.service.NotificationPushService;
import com.nexora.message.service.SysNoticeService;
import com.nexora.message.service.SysUserNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class NoticeBizService {
    private static final int NOTICE = 1;
    private static final int ANNOUNCEMENT = 2;
    private static final int TARGET_USERS = 1;
    private static final int TARGET_ALL = 3;
    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;
    private static final Pattern DANGEROUS_PROTOCOL = Pattern.compile(
            "(?i)(javascript|vbscript|data)\\s*:");

    private final SysNoticeService noticeService;
    private final SysUserNoticeService userNoticeService;
    private final SysUserNoticeMapper userNoticeMapper;
    private final SysUserService userService;
    private final NotificationPushService pushService;
    private final AnnouncementSessionClaimStore claimStore;
    private final ObjectMapper objectMapper;

    public NoticeBizService(SysNoticeService noticeService,
                            SysUserNoticeService userNoticeService,
                            SysUserNoticeMapper userNoticeMapper,
                            SysUserService userService,
                            NotificationPushService pushService,
                            AnnouncementSessionClaimStore claimStore,
                            ObjectMapper objectMapper) {
        this.noticeService = noticeService;
        this.userNoticeService = userNoticeService;
        this.userNoticeMapper = userNoticeMapper;
        this.userService = userService;
        this.pushService = pushService;
        this.claimStore = claimStore;
        this.objectMapper = objectMapper;
    }

    public IPage<NoticeAdminVo> adminPage(NoticeQueryForm form, PageParam pageParam) {
        IPage<SysNotice> page = noticeService.page(form, pageParam);
        return page.convert(this::toAdminVo);
    }

    public NoticeAdminVo adminDetail(Long id) {
        return toAdminVo(requiredNotice(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addDraft(NoticeForm form) {
        ValidatedForm validated = validate(form);
        int creatorId = SecurityUtils.getLoginIdAsInt();
        SysUser creator = userService.getById(creatorId);
        LocalDateTime now = LocalDateTime.now();
        SysNotice notice = new SysNotice();
        apply(notice, validated);
        notice.setStatus(DRAFT);
        notice.setCreateBy(creatorId);
        notice.setCreateName(creator == null ? "" : creator.getNickname());
        notice.setCreateTime(now);
        notice.setUpdateTime(now);
        noticeService.save(notice);
        return notice.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(NoticeForm form) {
        if (form == null || form.getId() == null) {
            throw new BizException("通知不存在");
        }
        SysNotice notice = requiredDraft(form.getId());
        apply(notice, validate(form));
        notice.setUpdateTime(LocalDateTime.now());
        noticeService.updateById(notice);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long id) {
        requiredDraft(id);
        noticeService.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        SysNotice notice = requiredDraft(id);
        List<Integer> recipients = resolveRecipients(notice);
        if (recipients.isEmpty()) {
            throw new BizException("没有可接收该消息的正常用户");
        }
        LocalDateTime now = LocalDateTime.now();
        List<SysUserNotice> snapshots = recipients.stream().map(userId -> {
            SysUserNotice item = new SysUserNotice();
            item.setNoticeId(notice.getId());
            item.setUserId(userId);
            item.setIsRead(0);
            return item;
        }).toList();
        userNoticeService.saveBatch(snapshots);
        notice.setStatus(PUBLISHED);
        notice.setPublishTime(now);
        notice.setUpdateTime(now);
        noticeService.updateById(notice);
        Runnable push = () -> pushService.pushPublished(notice, recipients);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
    }

    public IPage<NoticeUserVo> userPage(boolean unreadOnly, PageParam pageParam) {
        return userNoticeMapper.selectUserPage(
                PageUtils.buildPage(pageParam), currentUserId(), unreadOnly);
    }

    public NoticeUserVo userDetail(Long noticeId) {
        NoticeUserVo notice = userNoticeMapper.selectUserNotice(noticeId, currentUserId());
        if (notice == null) {
            throw new BizException("通知不存在");
        }
        return notice;
    }

    public long unreadCount() {
        return userNoticeMapper.selectCount(Wrappers.<SysUserNotice>lambdaQuery()
                .eq(SysUserNotice::getUserId, currentUserId())
                .eq(SysUserNotice::getIsRead, 0));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long noticeId) {
        Integer userId = currentUserId();
        SysUserNotice item = userNoticeMapper.selectOne(Wrappers.<SysUserNotice>lambdaQuery()
                .eq(SysUserNotice::getNoticeId, noticeId)
                .eq(SysUserNotice::getUserId, userId));
        if (item == null) {
            throw new BizException("通知不存在");
        }
        markRead(List.of(item));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        List<SysUserNotice> items = userNoticeMapper.selectList(Wrappers.<SysUserNotice>lambdaQuery()
                .eq(SysUserNotice::getUserId, currentUserId())
                .eq(SysUserNotice::getIsRead, 0));
        markRead(items);
    }

    public List<NoticeUserVo> claimLoginAnnouncements() {
        List<NoticeUserVo> notices = userNoticeMapper.selectUnreadAnnouncements(currentUserId());
        if (notices.isEmpty() || !claimStore.claimCurrentSession()) {
            return List.of();
        }
        return notices;
    }

    @Transactional(rollbackFor = Exception.class)
    public void acknowledgeAnnouncements(List<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return;
        }
        List<SysUserNotice> items = userNoticeMapper.selectList(Wrappers.<SysUserNotice>lambdaQuery()
                .eq(SysUserNotice::getUserId, currentUserId())
                .in(SysUserNotice::getNoticeId, new LinkedHashSet<>(noticeIds)));
        Set<Long> announcementIds = userNoticeMapper.selectUnreadAnnouncements(currentUserId()).stream()
                .map(NoticeUserVo::getNoticeId).collect(java.util.stream.Collectors.toSet());
        markRead(items.stream().filter(item -> announcementIds.contains(item.getNoticeId())).toList());
    }

    private void markRead(List<SysUserNotice> items) {
        LocalDateTime now = LocalDateTime.now();
        List<SysUserNotice> unread = items.stream().filter(item -> !Integer.valueOf(1).equals(item.getIsRead()))
                .peek(item -> {
                    item.setIsRead(1);
                    item.setReadTime(now);
                }).toList();
        if (!unread.isEmpty()) {
            userNoticeService.updateBatchById(unread);
        }
    }

    private ValidatedForm validate(NoticeForm form) {
        if (form == null || !StringUtils.hasText(form.getTitle())) {
            throw new BizException("请输入标题");
        }
        String title = form.getTitle().trim();
        if (title.length() > 62) {
            throw new BizException("标题不能超过62个字符");
        }
        if (form.getNoticeType() == null || (form.getNoticeType() != NOTICE
                && form.getNoticeType() != ANNOUNCEMENT)) {
            throw new BizException("消息类型不正确");
        }
        if (!"text".equals(form.getContentFormat()) && !"html".equals(form.getContentFormat())) {
            throw new BizException("内容格式不正确");
        }
        String content = form.getContent() == null ? "" : form.getContent();
        if (!StringUtils.hasText(content)) {
            throw new BizException("请输入正文");
        }
        if ("text".equals(form.getContentFormat()) && content.length() > 20_000) {
            throw new BizException("普通文案不能超过20000个字符");
        }
        if ("html".equals(form.getContentFormat())
                && content.getBytes(StandardCharsets.UTF_8).length > 256 * 1024) {
            throw new BizException("HTML内容不能超过256KB");
        }
        if ("html".equals(form.getContentFormat()) && DANGEROUS_PROTOCOL.matcher(content).find()) {
            throw new BizException("HTML中包含不安全的链接协议");
        }
        int targetType = form.getNoticeType() == ANNOUNCEMENT ? TARGET_ALL
                : form.getTargetType() == null ? 0 : form.getTargetType();
        if (targetType != TARGET_USERS && targetType != TARGET_ALL) {
            throw new BizException("请选择接收对象");
        }
        List<Integer> targetIds = form.getTargetUserIds() == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(form.getTargetUserIds()));
        if (targetType == TARGET_USERS) {
            if (targetIds.isEmpty()) {
                throw new BizException("请选择接收用户");
            }
            long count = userService.count(Wrappers.<SysUser>lambdaQuery()
                    .in(SysUser::getId, targetIds).eq(SysUser::getStatus, 1));
            if (count != targetIds.size()) {
                throw new BizException("接收用户中包含不存在或已停用的账号");
            }
        } else {
            targetIds = List.of();
        }
        return new ValidatedForm(title, content, form.getContentFormat(), form.getNoticeType(),
                targetType, targetIds);
    }

    private void apply(SysNotice notice, ValidatedForm form) {
        notice.setTitle(form.title());
        notice.setContent(form.content());
        notice.setContentFormat(form.contentFormat());
        notice.setNoticeType(form.noticeType());
        notice.setTargetType(form.targetType());
        try {
            notice.setTargetIds(objectMapper.writeValueAsString(form.targetUserIds()));
        } catch (Exception exception) {
            throw new BizException("接收用户格式不正确");
        }
    }

    private List<Integer> resolveRecipients(SysNotice notice) {
        if (Integer.valueOf(TARGET_ALL).equals(notice.getTargetType())) {
            return userService.list(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getStatus, 1))
                    .stream().map(SysUser::getId).toList();
        }
        List<Integer> targetIds = readTargetIds(notice.getTargetIds());
        List<Integer> normalIds = userService.list(Wrappers.<SysUser>lambdaQuery()
                        .in(SysUser::getId, targetIds).eq(SysUser::getStatus, 1))
                .stream().map(SysUser::getId).toList();
        if (normalIds.size() != targetIds.size()) {
            throw new BizException("接收用户中包含不存在或已停用的账号");
        }
        return normalIds;
    }

    private NoticeAdminVo toAdminVo(SysNotice notice) {
        NoticeAdminVo vo = new NoticeAdminVo();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setContentFormat(notice.getContentFormat());
        vo.setNoticeType(notice.getNoticeType());
        vo.setTargetType(notice.getTargetType());
        vo.setTargetUserIds(readTargetIds(notice.getTargetIds()));
        vo.setStatus(notice.getStatus());
        vo.setCreateBy(notice.getCreateBy());
        vo.setCreateName(notice.getCreateName());
        vo.setCreateTime(notice.getCreateTime());
        vo.setPublishTime(notice.getPublishTime());
        vo.setUpdateTime(notice.getUpdateTime());
        long recipients = userNoticeMapper.selectCount(Wrappers.<SysUserNotice>lambdaQuery()
                .eq(SysUserNotice::getNoticeId, notice.getId()));
        long read = userNoticeMapper.selectCount(Wrappers.<SysUserNotice>lambdaQuery()
                .eq(SysUserNotice::getNoticeId, notice.getId())
                .eq(SysUserNotice::getIsRead, 1));
        vo.setRecipientCount(recipients);
        vo.setReadCount(read);
        vo.setUnreadCount(recipients - read);
        return vo;
    }

    private List<Integer> readTargetIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new BizException("接收用户数据损坏");
        }
    }

    private SysNotice requiredNotice(Long id) {
        SysNotice notice = noticeService.getById(id);
        if (notice == null) {
            throw new BizException("通知不存在");
        }
        return notice;
    }

    private SysNotice requiredDraft(Long id) {
        SysNotice notice = requiredNotice(id);
        if (!Integer.valueOf(DRAFT).equals(notice.getStatus())) {
            throw new BizException("已发布通知不可修改");
        }
        return notice;
    }

    private static int currentUserId() {
        return SecurityUtils.getLoginIdAsInt();
    }

    private record ValidatedForm(String title, String content, String contentFormat,
                                 Integer noticeType, Integer targetType,
                                 List<Integer> targetUserIds) {
    }
}
