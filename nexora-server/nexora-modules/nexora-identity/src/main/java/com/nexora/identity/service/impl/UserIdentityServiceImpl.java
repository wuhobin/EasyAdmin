package com.nexora.identity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.identity.entity.UserIdentity;
import com.nexora.identity.mapper.UserIdentityMapper;
import com.nexora.identity.service.UserIdentityService;
import org.springframework.stereotype.Service;

@Service
public class UserIdentityServiceImpl extends ServiceImpl<UserIdentityMapper, UserIdentity>
        implements UserIdentityService {

    @Override
    public UserIdentity getByProviderIdentity(String provider, String providerAppId, String providerUserId) {
        return lambdaQuery()
                .eq(UserIdentity::getProvider, provider)
                .eq(UserIdentity::getProviderAppId, providerAppId)
                .eq(UserIdentity::getProviderUserId, providerUserId)
                .one();
    }
}
