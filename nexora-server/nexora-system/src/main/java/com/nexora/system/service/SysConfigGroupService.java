package com.nexora.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.system.entity.SysConfigGroup;

import java.util.List;

public interface SysConfigGroupService extends IService<SysConfigGroup> {

    List<SysConfigGroup> listOrdered();

    SysConfigGroup getByGroupCode(String groupCode);

    String getValueByGroupCode(String groupCode);
}
