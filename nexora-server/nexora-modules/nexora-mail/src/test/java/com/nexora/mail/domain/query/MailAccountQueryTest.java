package com.nexora.mail.domain.query;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MailAccountQueryTest {

    @Test
    void buildsOwnerAndExclusionConditions() {
        MailAccountQuery query = new MailAccountQuery();
        query.setOwnerId(7);
        query.setEmail("mail@example.com");
        query.setExcludeId(12L);

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("owner_id =")
                .contains("email =")
                .contains("id <>");
    }

    @Test
    void buildsOwnerIdsInCondition() {
        MailAccountQuery query = new MailAccountQuery();
        query.setOwnerIds(List.of(7, 8));

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("owner_id IN");
    }
}
