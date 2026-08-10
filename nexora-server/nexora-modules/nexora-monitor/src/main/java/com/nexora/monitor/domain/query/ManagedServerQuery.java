package com.nexora.monitor.domain.query;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

import java.util.Collection;

@Data
public class ManagedServerQuery {

    private Long id;

    private Integer ownerId;

    @QueryField(operator = Operator.LIKE)
    private String name;

    private Integer enabled;

    @QueryField(field = "ownerId", operator = Operator.IN)
    private Collection<Integer> ownerIds;
}
