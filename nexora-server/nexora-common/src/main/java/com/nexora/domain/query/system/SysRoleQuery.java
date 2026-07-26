package com.nexora.domain.query.system;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysRoleQuery {
    @QueryField(operator = Operator.LIKE)
    private String name;
}
