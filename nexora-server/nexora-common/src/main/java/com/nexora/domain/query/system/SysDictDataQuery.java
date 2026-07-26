package com.nexora.domain.query.system;

import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;

@Data
public class SysDictDataQuery {
    @QueryField
    private Long dictId;
}
