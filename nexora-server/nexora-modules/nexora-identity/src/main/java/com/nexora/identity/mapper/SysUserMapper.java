package com.nexora.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.domain.vo.SysUserPageListVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    SysUser selectByEmail(@Param("email") String email);

    IPage<SysUserPageListVo> selectUserPage(@Param("page") Page<Object> page,
                                            @Param("ew") Wrapper<SysUser> wrapper);

    List<SysUserPageListVo> selectUserRoles(@Param("userIds") List<Integer> userIds);
}
