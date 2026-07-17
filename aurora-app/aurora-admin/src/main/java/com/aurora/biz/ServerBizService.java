package com.aurora.biz;

import com.aurora.domain.convert.ServerConvert;
import com.aurora.domain.vo.server.ServerInfoVo;
import com.aurora.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServerBizService {
    private final ServerService serverService;
    public ServerInfoVo getServerInfo() { return ServerConvert.INSTANCE.toVo(serverService.getServerInfo()); }
}
