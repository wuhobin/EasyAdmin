package com.nexora.service;

import com.nexora.entity.MailAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MailAccountService extends IService<MailAccount> {
    List<MailAccount> listOrdered();

    List<MailAccount> listEnabled();

    boolean existsByEmail(String email, Long excludedId);
}
