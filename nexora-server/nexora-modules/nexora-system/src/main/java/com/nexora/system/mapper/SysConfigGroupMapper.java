package com.nexora.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.nexora.system.entity.SysConfigGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysConfigGroupMapper extends BaseMapper<SysConfigGroup> {
    List<SysConfigGroup> selectOrdered(@Param(Constants.WRAPPER) Wrapper<SysConfigGroup> wrapper);
}
