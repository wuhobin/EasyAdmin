package com.aurora.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aurora.dto.user.UserSaveOrUpdateDto;
import com.aurora.entity.SysUser;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.dto.user.UpdatePwdDTO;
import com.aurora.vo.user.OnlineUserVo;
import com.aurora.vo.user.SysUserPageListVo;
import com.aurora.vo.user.SysUserProfileVo;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    /**
     * 分页查询用户
     */
    IPage<SysUserPageListVo> listUsers(String nickname, Integer status, PageParam pageParam);

    /**
     * 新增用户
     */
    void add(UserSaveOrUpdateDto user);

    /**
     * 更新用户
     */
    void update(UserSaveOrUpdateDto user);

    /**
     * 删除用户
     */
    void delete(List<Integer> ids);


    /**
     * 修改密码
     *
     * @param updatePwdDTO 修改密码参数
     */
    void updatePwd(UpdatePwdDTO updatePwdDTO);

    /**
     * 获取个人信息
     * @return
     */
    SysUserProfileVo profile();

    /**
     * 修改个人信息
     * @param user
     */
    void updateProfile(SysUser user);

    /**
     * 锁屏界面验证密码
     * @param password
     * @return
     */
    Boolean verifyPassword(String password);

    /**
     * 重置密码
     * @param user
     * @return
     */
    Boolean resetPassword(SysUser user);

    /**
     * 获取在线用户列表
     * @return
     */
    IPage<OnlineUserVo> getOnlineUserList(String username, PageParam pageParam);


}
