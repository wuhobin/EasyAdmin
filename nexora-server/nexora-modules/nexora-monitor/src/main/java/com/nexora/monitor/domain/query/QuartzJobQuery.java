package com.nexora.monitor.domain.query;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class QuartzJobQuery {
    @QueryField(operator = Operator.LIKE)
    private String jobName;
    @QueryField
    private String jobGroup;
    @QueryField
    private String status;
}
