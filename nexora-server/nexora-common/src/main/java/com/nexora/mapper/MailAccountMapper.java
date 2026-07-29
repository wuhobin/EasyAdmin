package com.nexora.mapper;

import com.nexora.entity.MailAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MailAccountMapper extends BaseMapper<MailAccount> {
    List<MailAccount> selectEnabledForActiveOwners();
}
