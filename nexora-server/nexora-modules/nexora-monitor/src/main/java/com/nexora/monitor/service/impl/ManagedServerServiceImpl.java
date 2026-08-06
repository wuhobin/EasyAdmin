package com.nexora.monitor.service.impl;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.monitor.domain.query.ManagedServerQuery;
import com.nexora.monitor.entity.ManagedServer;
import com.nexora.monitor.mapper.ManagedServerMapper;
import com.nexora.monitor.service.ManagedServerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
public class ManagedServerServiceImpl extends ServiceImpl<ManagedServerMapper, ManagedServer>
        implements ManagedServerService {

    @Override
    public IPage<ManagedServer> listOwned(ManagedServerQuery query, PageParam pageParam) {
        LambdaQueryWrapper<ManagedServer> wrapper = new LambdaQueryWrapper<ManagedServer>()
                .eq(ManagedServer::getOwnerId, query.getOwnerId())
                .like(StringUtils.hasText(query.getName()), ManagedServer::getName, query.getName())
                .eq(query.getEnabled() != null, ManagedServer::getEnabled, query.getEnabled())
                .orderByAsc(ManagedServer::getSort)
                .orderByDesc(ManagedServer::getId);
        return page(PageUtils.buildPage(pageParam), wrapper);
    }

    @Override
    public ManagedServer getByIdAndOwnerId(Long id, Integer ownerId) {
        return getOne(new LambdaQueryWrapper<ManagedServer>()
                .eq(ManagedServer::getId, id)
                .eq(ManagedServer::getOwnerId, ownerId), false);
    }

    @Override
    public boolean updateByIdAndOwnerId(ManagedServer server, Integer ownerId) {
        return update(server, new LambdaUpdateWrapper<ManagedServer>()
                .eq(ManagedServer::getId, server.getId())
                .eq(ManagedServer::getOwnerId, ownerId));
    }

    @Override
    public boolean removeByIdAndOwnerId(Long id, Integer ownerId) {
        return remove(new LambdaQueryWrapper<ManagedServer>()
                .eq(ManagedServer::getId, id)
                .eq(ManagedServer::getOwnerId, ownerId));
    }

    @Override
    public boolean removeByOwnerIds(Collection<Integer> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return true;
        }
        return remove(new LambdaQueryWrapper<ManagedServer>()
                .in(ManagedServer::getOwnerId, ownerIds));
    }

    @Override
    public boolean updateTrustedFingerprint(Long id, Integer ownerId, String fingerprint,
                                            String algorithm, LocalDateTime verifiedTime) {
        return update(new LambdaUpdateWrapper<ManagedServer>()
                .eq(ManagedServer::getId, id)
                .eq(ManagedServer::getOwnerId, ownerId)
                .set(ManagedServer::getTrustedFingerprint, fingerprint)
                .set(ManagedServer::getFingerprintAlgorithm, algorithm)
                .set(ManagedServer::getFingerprintVerifiedTime, verifiedTime)
                .set(ManagedServer::getLastError, ""));
    }

    @Override
    public boolean clearTrustedFingerprint(Long id, Integer ownerId) {
        return update(new LambdaUpdateWrapper<ManagedServer>()
                .eq(ManagedServer::getId, id)
                .eq(ManagedServer::getOwnerId, ownerId)
                .set(ManagedServer::getTrustedFingerprint, null)
                .set(ManagedServer::getFingerprintAlgorithm, null)
                .set(ManagedServer::getFingerprintVerifiedTime, null)
                .set(ManagedServer::getLastConnectTime, null)
                .set(ManagedServer::getLastError, ""));
    }

    @Override
    public boolean clearSavedPassword(Long id, Integer ownerId) {
        return update(new LambdaUpdateWrapper<ManagedServer>()
                .eq(ManagedServer::getId, id)
                .eq(ManagedServer::getOwnerId, ownerId)
                .set(ManagedServer::getPasswordCiphertext, null));
    }

    @Override
    public void updateConnectionState(Long id, Integer ownerId, String error) {
        update(new LambdaUpdateWrapper<ManagedServer>()
                .eq(ManagedServer::getId, id)
                .eq(ManagedServer::getOwnerId, ownerId)
                .set(ManagedServer::getLastConnectTime, LocalDateTime.now())
                .set(ManagedServer::getLastError, error == null ? "" : error));
    }
}
