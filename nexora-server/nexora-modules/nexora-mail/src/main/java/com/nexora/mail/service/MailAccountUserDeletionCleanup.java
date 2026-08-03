package com.nexora.mail.service;

import com.nexora.contract.UserDeletionCleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 删除用户时清理其邮箱账户。
 */
@Component
@RequiredArgsConstructor
public class MailAccountUserDeletionCleanup implements UserDeletionCleanup {

    private final MailAccountService mailAccountService;

    @Override
    public void cleanup(List<Integer> userIds) {
        mailAccountService.removeByOwnerIds(userIds);
    }
}
