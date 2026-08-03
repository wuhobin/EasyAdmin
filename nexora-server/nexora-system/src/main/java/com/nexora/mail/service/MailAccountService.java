package com.nexora.mail.service;

import com.nexora.mail.entity.MailAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

public interface MailAccountService extends IService<MailAccount> {
    List<MailAccount> listOrderedByOwnerId(Integer ownerId);

    List<MailAccount> listEnabledByOwnerId(Integer ownerId);

    List<MailAccount> listEnabledForActiveOwners();

    MailAccount getByIdAndOwnerId(Long id, Integer ownerId);

    boolean existsByOwnerIdAndEmail(Integer ownerId, String email, Long excludedId);

    boolean removeByIdAndOwnerId(Long id, Integer ownerId);

    boolean removeByOwnerIds(Collection<Integer> ownerIds);
}
