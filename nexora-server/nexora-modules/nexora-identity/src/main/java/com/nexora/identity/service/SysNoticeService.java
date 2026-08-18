package com.nexora.identity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.nexora.identity.domain.form.NoticeQueryForm;
import com.nexora.identity.entity.SysNotice;

public interface SysNoticeService extends IService<SysNotice> {
    IPage<SysNotice> page(NoticeQueryForm form, PageParam pageParam);
}
