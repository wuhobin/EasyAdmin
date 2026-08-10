package com.nexora.identity.domain.query;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysUserQuery {
    @QueryField(operator = Operator.LIKE)
    private String nickname;
    @QueryField(operator = Operator.LIKE)
    private String email;
    private Integer status;
    private String avatar;
}
