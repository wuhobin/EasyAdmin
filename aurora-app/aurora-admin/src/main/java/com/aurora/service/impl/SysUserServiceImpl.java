package com.aurora.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.domain.query.system.SysUserQuery;
import com.aurora.domain.vo.user.SysUserPageListVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.entity.SysUser;
import com.aurora.mapper.SysUserMapper;
import com.aurora.service.SysUserService;
import org.springframework.stereotype.Service;


@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public IPage<SysUserPageListVo> listUsers(SysUserQuery query, PageParam pageParam) {
        return baseMapper.selectUserPage(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public SysUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }
}
