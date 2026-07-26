package com.nexora.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.domain.query.system.SysUserQuery;
import com.nexora.domain.vo.user.SysUserPageListVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.nexora.entity.SysUser;
import com.nexora.mapper.SysUserMapper;
import com.nexora.service.SysUserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public IPage<SysUserPageListVo> listUsers(SysUserQuery query, PageParam pageParam) {
        IPage<SysUserPageListVo> page = baseMapper.selectUserPage(
                PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
        List<Integer> userIds = page.getRecords().stream().map(SysUserPageListVo::getId).toList();
        if (userIds.isEmpty()) {
            return page;
        }
        Map<Integer, SysUserPageListVo> rolesByUserId = baseMapper.selectUserRoles(userIds).stream()
                .collect(Collectors.toMap(SysUserPageListVo::getId, Function.identity()));
        page.getRecords().forEach(user -> {
            SysUserPageListVo roles = rolesByUserId.get(user.getId());
            user.setRoleIds(roles == null ? List.of() : roles.getRoleIds());
        });
        return page;
    }

    @Override
    public SysUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }
}
