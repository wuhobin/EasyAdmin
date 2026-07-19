package com.aurora.domain.query.system;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysDictQuery {
    @QueryField(operator = Operator.LIKE)
    private String name;
    @QueryField
    private Integer status;
}
