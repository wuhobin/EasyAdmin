package com.nexora.identity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.identity.entity.UserIdentity;

public interface UserIdentityService extends IService<UserIdentity> {

    UserIdentity getByProviderIdentity(String provider, String providerAppId, String providerUserId);
}
