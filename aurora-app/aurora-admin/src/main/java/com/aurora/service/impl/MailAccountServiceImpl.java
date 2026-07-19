package com.aurora.service.impl;

import com.aurora.entity.MailAccount;
import com.aurora.mapper.MailAccountMapper;
import com.aurora.service.MailAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailAccountServiceImpl extends ServiceImpl<MailAccountMapper, MailAccount>
        implements MailAccountService {

    @Override
    public List<MailAccount> listOrdered() {
        return list(new LambdaQueryWrapper<MailAccount>()
                .orderByAsc(MailAccount::getSort)
                .orderByAsc(MailAccount::getId));
    }

    @Override
    public List<MailAccount> listEnabled() {
        return list(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getEnabled, 1)
                .orderByAsc(MailAccount::getSort)
                .orderByAsc(MailAccount::getId));
    }

    @Override
    public boolean existsByEmail(String email, Long excludedId) {
        LambdaQueryWrapper<MailAccount> wrapper = new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getEmail, email);
        if (excludedId != null) {
            wrapper.ne(MailAccount::getId, excludedId);
        }
        return count(wrapper) > 0;
    }
}
