package com.nexora.monitor.service;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.monitor.domain.query.ManagedServerQuery;
import com.nexora.monitor.entity.ManagedServer;

import java.time.LocalDateTime;
import java.util.Collection;

public interface ManagedServerService extends IService<ManagedServer> {

    IPage<ManagedServer> listOwned(ManagedServerQuery query, PageParam pageParam);

    ManagedServer getByIdAndOwnerId(Long id, Integer ownerId);

    boolean updateByIdAndOwnerId(ManagedServer server, Integer ownerId);

    boolean removeByIdAndOwnerId(Long id, Integer ownerId);

    boolean removeByOwnerIds(Collection<Integer> ownerIds);

    boolean updateTrustedFingerprint(Long id, Integer ownerId, String fingerprint,
                                     String algorithm, LocalDateTime verifiedTime);

    boolean clearTrustedFingerprint(Long id, Integer ownerId);

    boolean clearSavedPassword(Long id, Integer ownerId);

    void updateConnectionState(Long id, Integer ownerId, String error);
}
