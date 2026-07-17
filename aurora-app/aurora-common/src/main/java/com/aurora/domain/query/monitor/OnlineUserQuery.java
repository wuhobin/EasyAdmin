package com.aurora.domain.query.monitor;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class OnlineUserQuery {
    @QueryField(operator = Operator.LIKE)
    private String username;
}
