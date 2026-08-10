package com.nexora.system.mapper;

import com.nexora.system.entity.SysDictData;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据表 Mapper接口
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {
    List<SysDictData> selectOrdered(@Param(Constants.WRAPPER) Wrapper<SysDictData> wrapper);
}
