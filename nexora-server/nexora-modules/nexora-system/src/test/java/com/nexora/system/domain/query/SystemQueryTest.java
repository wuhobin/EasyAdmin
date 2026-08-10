package com.nexora.system.domain.query;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemQueryTest {

    @Test
    void buildsDictionaryTypeAndExclusionConditions() {
        SysDictQuery query = new SysDictQuery();
        query.setType("mail_provider");
        query.setExcludeId(4L);

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("type =")
                .contains("id <>");
    }

    @Test
    void buildsDictionaryDataStatusCondition() {
        SysDictDataQuery query = new SysDictDataQuery();
        query.setDictId(10L);
        query.setStatus(1);

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("dict_id =")
                .contains("status =");
    }

    @Test
    void buildsConfigGroupCodeCondition() {
        SysConfigGroupQuery query = new SysConfigGroupQuery();
        query.setGroupCode("mail");

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("group_code =");
    }
}
