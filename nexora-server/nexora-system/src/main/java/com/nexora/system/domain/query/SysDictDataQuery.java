package com.nexora.system.domain.query;

import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysDictDataQuery {
    @QueryField
    private Long dictId;
}
