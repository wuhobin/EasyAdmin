package com.nexora.monitor.domain.query;

import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysOperateLogQuery {
    @QueryField
    private Integer userId;
}
