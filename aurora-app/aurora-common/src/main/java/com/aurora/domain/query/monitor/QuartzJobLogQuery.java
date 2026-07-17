package com.aurora.domain.query.monitor;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class QuartzJobLogQuery {
    @QueryField(operator = Operator.LIKE)
    private String jobName;
    @QueryField
    private String jobGroup;
    @QueryField
    private String status;
}
