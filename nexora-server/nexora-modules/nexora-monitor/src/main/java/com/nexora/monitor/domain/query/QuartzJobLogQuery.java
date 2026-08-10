package com.nexora.monitor.domain.query;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class QuartzJobLogQuery {
    @QueryField(operator = Operator.LIKE)
    private String jobName;
    private String jobGroup;
    private String status;
}
