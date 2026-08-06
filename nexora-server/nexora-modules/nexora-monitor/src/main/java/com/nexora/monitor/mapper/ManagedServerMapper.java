package com.nexora.monitor.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.nexora.monitor.entity.ManagedServer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ManagedServerMapper extends BaseMapper<ManagedServer> {

    int updateTrustedFingerprint(@Param("fingerprint") String fingerprint,
                                 @Param("algorithm") String algorithm,
                                 @Param("verifiedTime") LocalDateTime verifiedTime,
                                 @Param(Constants.WRAPPER) Wrapper<ManagedServer> wrapper);

    int clearTrustedFingerprint(@Param(Constants.WRAPPER) Wrapper<ManagedServer> wrapper);

    int clearSavedPassword(@Param(Constants.WRAPPER) Wrapper<ManagedServer> wrapper);

    int updateConnectionState(@Param("connectTime") LocalDateTime connectTime,
                              @Param("error") String error,
                              @Param(Constants.WRAPPER) Wrapper<ManagedServer> wrapper);
}
