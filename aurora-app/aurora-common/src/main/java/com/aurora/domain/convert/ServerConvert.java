package com.aurora.domain.convert;

import com.aurora.domain.vo.server.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper
public interface ServerConvert {
    ServerConvert INSTANCE = Mappers.getMapper(ServerConvert.class);
    ServerInfoVo toVo(ServerInfo source);
    CpuInfoVo toVo(CpuInfo source);
    MemInfoVo toVo(MemInfo source);
    SysInfoVo toVo(SysInfo source);
    JvmInfoVo toVo(JvmInfo source);
    SysFileVo toVo(SysFile source);
    List<SysFileVo> toFileVoList(List<SysFile> source);
}
