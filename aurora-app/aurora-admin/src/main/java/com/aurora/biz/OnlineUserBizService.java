package com.aurora.biz;

import com.aurora.domain.convert.SysUserConvert;
import com.aurora.domain.form.query.monitor.OnlineUserQueryForm;
import com.aurora.domain.vo.user.OnlineUserVo;
import com.aurora.service.SysUserService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnlineUserBizService {
    private final SysUserService sysUserService;
    public IPage<OnlineUserVo> list(OnlineUserQueryForm form, PageParam pageParam) {
        return sysUserService.getOnlineUserList(SysUserConvert.INSTANCE.toQuery(form), pageParam)
                .convert(SysUserConvert.INSTANCE::toVo);
    }
}
