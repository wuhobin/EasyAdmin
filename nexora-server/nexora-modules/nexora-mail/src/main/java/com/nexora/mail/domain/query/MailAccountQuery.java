package com.nexora.mail.domain.query;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

import java.util.Collection;

@Data
public class MailAccountQuery {

    private Long id;

    private Integer ownerId;

    @QueryField(queryEmpty = true)
    private String email;

    private Integer enabled;

    @QueryField(field = "id", operator = Operator.NE)
    private Long excludeId;

    @QueryField(field = "ownerId", operator = Operator.IN)
    private Collection<Integer> ownerIds;
}
