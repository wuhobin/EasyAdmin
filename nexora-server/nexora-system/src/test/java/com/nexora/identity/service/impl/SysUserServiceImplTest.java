package com.nexora.identity.service.impl;

import com.nexora.identity.domain.query.SysUserQuery;
import com.nexora.identity.domain.vo.user.SysUserPageListVo;
import com.nexora.identity.mapper.SysUserMapper;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper mapper;

    private SysUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysUserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void fillsRolesWithOneBatchQueryAfterPagingUsers() {
        SysUserPageListVo first = user(1);
        SysUserPageListVo second = user(2);
        Page<SysUserPageListVo> page = new Page<>(1, 10);
        page.setRecords(List.of(first, second));
        page.setTotal(2);
        when(mapper.selectUserPage(any(), any())).thenReturn(page);
        SysUserPageListVo firstRoles = user(1);
        firstRoles.setRoleIds(List.of(10, 11));
        when(mapper.selectUserRoles(List.of(1, 2))).thenReturn(List.of(firstRoles));

        service.listUsers(new SysUserQuery(), new PageParam(1, 10));

        assertThat(first.getRoleIds()).containsExactly(10, 11);
        assertThat(second.getRoleIds()).isEmpty();
        verify(mapper).selectUserRoles(List.of(1, 2));
    }

    @Test
    void skipsTheRoleQueryForAnEmptyPage() {
        Page<SysUserPageListVo> page = new Page<>(1, 10);
        when(mapper.selectUserPage(any(), any())).thenReturn(page);

        service.listUsers(new SysUserQuery(), new PageParam(1, 10));

        verify(mapper, never()).selectUserRoles(any());
    }

    @Test
    void checksWhetherAnAvatarUrlIsInUse() {
        when(mapper.exists(any())).thenReturn(true, false);

        assertThat(service.existsByAvatar("https://oss.example.com/avatar.png")).isTrue();
        assertThat(service.existsByAvatar("https://oss.example.com/unused.png")).isFalse();
        assertThat(service.existsByAvatar(" ")).isFalse();

        verify(mapper, times(2)).exists(any());
    }

    private static SysUserPageListVo user(int id) {
        SysUserPageListVo user = new SysUserPageListVo();
        user.setId(id);
        return user;
    }
}
