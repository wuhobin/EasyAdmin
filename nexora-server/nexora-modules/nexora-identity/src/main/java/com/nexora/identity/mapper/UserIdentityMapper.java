package com.nexora.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexora.identity.entity.UserIdentity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentity> {
}
