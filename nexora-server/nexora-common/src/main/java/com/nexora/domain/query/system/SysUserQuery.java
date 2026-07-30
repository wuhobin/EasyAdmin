package com.nexora.domain.query.system;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysUserQuery {
    @QueryField(field = "u.nickname", operator = Operator.LIKE)
    private String nickname;
    @QueryField(field = "u.email", operator = Operator.LIKE)
    private String email;
    @QueryField(field = "u.status")
    private Integer status;
}
