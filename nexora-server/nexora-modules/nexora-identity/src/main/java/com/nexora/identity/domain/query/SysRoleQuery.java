package com.nexora.identity.domain.query;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysRoleQuery {
    @QueryField(operator = Operator.LIKE)
    private String name;
    @QueryField(queryEmpty = true)
    private String code;
    @QueryField(field = "id", operator = Operator.NE)
    private Integer excludeId;
}
