package com.nexora.identity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.identity.entity.SysUserNotice;
import com.nexora.identity.mapper.SysUserNoticeMapper;
import com.nexora.identity.service.SysUserNoticeService;
import org.springframework.stereotype.Service;

@Service
public class SysUserNoticeServiceImpl extends ServiceImpl<SysUserNoticeMapper, SysUserNotice>
        implements SysUserNoticeService {
}
