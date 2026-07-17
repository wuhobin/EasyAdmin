package com.aurora.domain.query.monitor;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class CacheKeyQuery {
    @QueryField(operator = Operator.LIKE)
    private String key;
    @QueryField(ignore = true)
    private Integer pageNum = 1;
    @QueryField(ignore = true)
    private Integer pageSize = 10;
}
