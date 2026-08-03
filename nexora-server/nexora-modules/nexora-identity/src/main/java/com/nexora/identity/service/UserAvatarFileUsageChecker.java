package com.nexora.identity.service;

import com.nexora.contract.StoredFileUsageChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAvatarFileUsageChecker implements StoredFileUsageChecker {

    private final SysUserService sysUserService;

    @Override
    public boolean isInUse(String fileUrl) {
        return sysUserService.existsByAvatar(fileUrl);
    }
}
