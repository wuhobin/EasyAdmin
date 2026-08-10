package com.nexora.monitor.service.impl;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexora.monitor.domain.query.ManagedServerQuery;
import com.nexora.monitor.entity.ManagedServer;
import com.nexora.monitor.mapper.ManagedServerMapper;
import com.nexora.monitor.service.ManagedServerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

@Service
public class ManagedServerServiceImpl extends ServiceImpl<ManagedServerMapper, ManagedServer>
        implements ManagedServerService {

    @Override
    public IPage<ManagedServer> listOwned(ManagedServerQuery query, PageParam pageParam) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(query.getOwnerId(), "ownerId");
        return page(PageUtils.buildPage(pageParam), DynamicCondition.toWrapper(query));
    }

    @Override
    public ManagedServer getByIdAndOwnerId(Long id, Integer ownerId) {
        if (id == null || ownerId == null) {
            return null;
        }
        ManagedServerQuery query = new ManagedServerQuery();
        query.setId(id);
        query.setOwnerId(ownerId);
        return getOne(DynamicCondition.toWrapper(query), false);
    }

    @Override
    public boolean updateByIdAndOwnerId(ManagedServer server, Integer ownerId) {
        if (server == null || server.getId() == null || ownerId == null) {
            return false;
        }
        ManagedServerQuery query = new ManagedServerQuery();
        query.setId(server.getId());
        query.setOwnerId(ownerId);
        return update(server, DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean removeByIdAndOwnerId(Long id, Integer ownerId) {
        if (id == null || ownerId == null) {
            return false;
        }
        ManagedServerQuery query = new ManagedServerQuery();
        query.setId(id);
        query.setOwnerId(ownerId);
        return remove(DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean removeByOwnerIds(Collection<Integer> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return true;
        }
        ManagedServerQuery query = new ManagedServerQuery();
        query.setOwnerIds(ownerIds);
        return remove(DynamicCondition.toWrapper(query));
    }

    @Override
    public boolean updateTrustedFingerprint(Long id, Integer ownerId, String fingerprint,
                                            String algorithm, LocalDateTime verifiedTime) {
        if (id == null || ownerId == null) {
            return false;
        }
        ManagedServerQuery query = byIdAndOwner(id, ownerId);
        return baseMapper.updateTrustedFingerprint(fingerprint, algorithm, verifiedTime,
                DynamicCondition.toWrapper(query)) > 0;
    }

    @Override
    public boolean clearTrustedFingerprint(Long id, Integer ownerId) {
        if (id == null || ownerId == null) {
            return false;
        }
        return baseMapper.clearTrustedFingerprint(
                DynamicCondition.toWrapper(byIdAndOwner(id, ownerId))) > 0;
    }

    @Override
    public boolean clearSavedPassword(Long id, Integer ownerId) {
        if (id == null || ownerId == null) {
            return false;
        }
        return baseMapper.clearSavedPassword(
                DynamicCondition.toWrapper(byIdAndOwner(id, ownerId))) > 0;
    }

    @Override
    public void updateConnectionState(Long id, Integer ownerId, String error) {
        if (id == null || ownerId == null) {
            return;
        }
        baseMapper.updateConnectionState(LocalDateTime.now(), error == null ? "" : error,
                DynamicCondition.toWrapper(byIdAndOwner(id, ownerId)));
    }

    private static ManagedServerQuery byIdAndOwner(Long id, Integer ownerId) {
        ManagedServerQuery query = new ManagedServerQuery();
        query.setId(id);
        query.setOwnerId(ownerId);
        return query;
    }
}
