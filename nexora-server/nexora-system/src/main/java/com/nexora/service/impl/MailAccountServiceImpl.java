package com.nexora.service.impl;

import com.nexora.entity.MailAccount;
import com.nexora.mapper.MailAccountMapper;
import com.nexora.service.MailAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class MailAccountServiceImpl extends ServiceImpl<MailAccountMapper, MailAccount>
        implements MailAccountService {

    @Override
    public List<MailAccount> listOrderedByOwnerId(Integer ownerId) {
        return list(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getOwnerId, ownerId)
                .orderByAsc(MailAccount::getSort)
                .orderByAsc(MailAccount::getId));
    }

    @Override
    public List<MailAccount> listEnabledByOwnerId(Integer ownerId) {
        return list(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getOwnerId, ownerId)
                .eq(MailAccount::getEnabled, 1)
                .orderByAsc(MailAccount::getSort)
                .orderByAsc(MailAccount::getId));
    }

    @Override
    public List<MailAccount> listEnabledForActiveOwners() {
        return baseMapper.selectEnabledForActiveOwners();
    }

    @Override
    public MailAccount getByIdAndOwnerId(Long id, Integer ownerId) {
        return getOne(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getId, id)
                .eq(MailAccount::getOwnerId, ownerId), false);
    }

    @Override
    public boolean existsByOwnerIdAndEmail(Integer ownerId, String email, Long excludedId) {
        LambdaQueryWrapper<MailAccount> wrapper = new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getOwnerId, ownerId)
                .eq(MailAccount::getEmail, email);
        if (excludedId != null) {
            wrapper.ne(MailAccount::getId, excludedId);
        }
        return count(wrapper) > 0;
    }

    @Override
    public boolean removeByIdAndOwnerId(Long id, Integer ownerId) {
        return remove(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getId, id)
                .eq(MailAccount::getOwnerId, ownerId));
    }

    @Override
    public boolean removeByOwnerIds(Collection<Integer> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return true;
        }
        return remove(new LambdaQueryWrapper<MailAccount>()
                .in(MailAccount::getOwnerId, ownerIds));
    }
}
