package com.nexora.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.nexora.message.domain.form.NoticeQueryForm;
import com.nexora.message.entity.SysNotice;

public interface SysNoticeService extends IService<SysNotice> {
    IPage<SysNotice> page(NoticeQueryForm form, PageParam pageParam);
}
