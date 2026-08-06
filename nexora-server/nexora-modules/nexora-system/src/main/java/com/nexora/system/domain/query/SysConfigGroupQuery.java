package com.nexora.system.domain.query;

import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysConfigGroupQuery {

    @QueryField(queryEmpty = true)
    private String groupCode;
}
