package com.nexora.monitor.domain.query;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedServerQueryTest {

    @Test
    void buildsOwnerScopedServerConditions() {
        ManagedServerQuery query = new ManagedServerQuery();
        query.setId(11L);
        query.setOwnerId(7);
        query.setName("prod");
        query.setEnabled(1);

        String sql = DynamicCondition.toWrapper(query).getSqlSegment();

        assertThat(sql)
                .contains("id =")
                .contains("owner_id =")
                .contains("name LIKE")
                .contains("enabled =");
    }

    @Test
    void buildsOwnerIdInConditionForCleanup() {
        ManagedServerQuery query = new ManagedServerQuery();
        query.setOwnerIds(List.of(7, 8));

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("owner_id IN");
    }
}
