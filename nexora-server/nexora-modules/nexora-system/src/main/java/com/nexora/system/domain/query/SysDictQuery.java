package com.nexora.system.domain.query;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysDictQuery {
    @QueryField(operator = Operator.LIKE)
    private String name;
    private Integer status;
    @QueryField(queryEmpty = true)
    private String type;
    @QueryField(field = "id", operator = Operator.NE)
    private Long excludeId;
}
