package com.nexora.mail.mapper;

import com.nexora.mail.entity.MailAccount;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MailAccountMapper extends BaseMapper<MailAccount> {
    List<MailAccount> selectOrdered(@Param(Constants.WRAPPER) Wrapper<MailAccount> wrapper);

    List<MailAccount> selectEnabledForActiveOwners();
}
