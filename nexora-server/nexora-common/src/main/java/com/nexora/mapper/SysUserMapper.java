package com.nexora.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexora.entity.SysUser;
import com.nexora.domain.vo.user.SysUserPageListVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser selectByUsername(@Param("username") String username);

    IPage<SysUserPageListVo> selectUserPage(@Param("page") Page<Object> page,
                                            @Param("ew") Wrapper<SysUser> wrapper);

    List<SysUserPageListVo> selectUserRoles(@Param("userIds") List<Integer> userIds);
}
