package com.nexora.file.domain.query;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OssFileQueryTest {

    @Test
    void buildsFileIdConditionForIdempotentSave() {
        OssFileQuery query = new OssFileQuery();
        query.setFileId("file-123");

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("file_id =");
    }
}
