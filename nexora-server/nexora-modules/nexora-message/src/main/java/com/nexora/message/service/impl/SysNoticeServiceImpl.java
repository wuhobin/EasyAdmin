package com.nexora.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.nexora.message.domain.form.NoticeQueryForm;
import com.nexora.message.entity.SysNotice;
import com.nexora.message.mapper.SysNoticeMapper;
import com.nexora.message.service.SysNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice>
        implements SysNoticeService {
    @Override
    public IPage<SysNotice> page(NoticeQueryForm form, PageParam pageParam) {
        QueryWrapper<SysNotice> wrapper = new QueryWrapper<>();
        if (form != null && StringUtils.hasText(form.getTitle())) {
            wrapper.like("title", form.getTitle().trim());
        }
        if (form != null && form.getNoticeType() != null) {
            wrapper.eq("notice_type", form.getNoticeType());
        }
        if (form != null && form.getStatus() != null) {
            wrapper.eq("status", form.getStatus());
        }
        wrapper.orderByDesc("publish_time").orderByDesc("create_time").orderByDesc("id");
        return baseMapper.selectPage(PageUtils.buildPage(pageParam), wrapper);
    }
}
