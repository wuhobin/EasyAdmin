package com.nexora.identity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.identity.domain.query.SysUserQuery;
import com.nexora.identity.domain.vo.SysUserPageListVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.mapper.SysUserMapper;
import com.nexora.identity.service.SysUserService;
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
    public SysUser getByEmail(String email) {
        return baseMapper.selectByEmail(email);
    }

    @Override
    public boolean existsByAvatar(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return false;
        }
        return baseMapper.exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAvatar, avatar));
    }
}
