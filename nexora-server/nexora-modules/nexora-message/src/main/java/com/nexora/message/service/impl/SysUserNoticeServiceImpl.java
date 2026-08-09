package com.nexora.message.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.message.entity.SysUserNotice;
import com.nexora.message.mapper.SysUserNoticeMapper;
import com.nexora.message.service.SysUserNoticeService;
import org.springframework.stereotype.Service;

@Service
public class SysUserNoticeServiceImpl extends ServiceImpl<SysUserNoticeMapper, SysUserNotice>
        implements SysUserNoticeService {
}
