package com.nexora.mail.service.impl;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.nexora.mail.domain.query.MailAccountQuery;
import com.nexora.mail.entity.MailAccount;
import com.nexora.mail.mapper.MailAccountMapper;
import com.nexora.mail.service.MailAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class MailAccountServiceImpl extends ServiceImpl<MailAccountMapper, MailAccount>
        implements MailAccountService {

    @Override
    public List<MailAccount> listOrderedByOwnerId(Integer ownerId) {
        MailAccountQuery query = new MailAccountQuery();
        query.setOwnerId(Objects.requireNonNull(ownerId, "ownerId"));
        return baseMapper.selectOrdered(DynamicCondition.toWrapper(query));
    }

    @Override
    public List<MailAccount> listEnabledByOwnerId(Integer ownerId) {
        MailAccountQuery query = new MailAccountQuery();
        query.setOwnerId(Objects.requireNonNull(ownerId, "ownerId"));
        query.setEnabled(1);
        return baseMapper.selectOrdered(DynamicCondition.toWrapper(query));
    }

    @Override
    public List<MailAccount> listEnabledForActiveOwners() {
        return baseMapper.selectEnabledForActiveOwners();
    }

    @Override
    public MailAccount getByIdAndOwnerId(Long id, Integer ownerId) {
        if (id == null || ownerId == null) {
            return null;
        }
        MailAccountQuery query = new MailAccountQuery();
        query.setId(id);
        query.setOwnerId(ownerId);
        return getOne(DynamicCondition.toWrapper(query), false);
    }

    @Override
    public boolean existsByOwnerIdAndEmail(Integer ownerId, String email, Long excludedId) {
        if (ownerId == null || email == null) {
            return false;
        }
        MailAccountQuery query = new MailAccountQuery();
        query.setOwnerId(ownerId);
        query.setEmail(email);
        query.setExcludeId(excludedId);
        return count(DynamicCondition.toWrapper(query)) > 0;
    }

    @Override
    public boolean removeByIdAndOwnerId(Long id, Integer ownerId) {
        if (id == null || ownerId == null) {
            return false;
        }
        MailAccountQuery query = new MailAccountQuery();
        query.setId(id);
        query.setOwnerId(ownerId);
        return remove(DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean removeByOwnerIds(Collection<Integer> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return true;
        }
        MailAccountQuery query = new MailAccountQuery();
        query.setOwnerIds(ownerIds);
        return remove(DynamicCondition.toWrapper(query));
    }
}
