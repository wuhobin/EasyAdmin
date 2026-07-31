package com.nexora.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexora.entity.SysConfigGroup;

import java.util.List;

public interface SysConfigGroupService extends IService<SysConfigGroup> {

    List<SysConfigGroup> listOrdered();

    SysConfigGroup getByGroupCode(String groupCode);

    String getValueByGroupCode(String groupCode);
}
