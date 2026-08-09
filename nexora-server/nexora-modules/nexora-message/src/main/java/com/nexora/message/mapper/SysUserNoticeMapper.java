package com.nexora.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexora.message.domain.vo.NoticeUserVo;
import com.nexora.message.domain.vo.NoticeUserUnreadVo;
import com.nexora.message.entity.SysUserNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserNoticeMapper extends BaseMapper<SysUserNotice> {
    IPage<NoticeUserVo> selectUserPage(@Param("page") Page<Object> page,
                                       @Param("userId") Integer userId,
                                       @Param("unreadOnly") boolean unreadOnly);

    NoticeUserVo selectUserNotice(@Param("noticeId") Long noticeId,
                                  @Param("userId") Integer userId);

    List<NoticeUserVo> selectUnreadAnnouncements(@Param("userId") Integer userId);

    @MapKey("userId")
    Map<Integer, NoticeUserUnreadVo> selectUnreadCounts(@Param("userIds") List<Integer> userIds);
}
