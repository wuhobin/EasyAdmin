package com.nexora.identity.domain.query;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityQueryTest {

    @Test
    void buildsRoleCodeAndExclusionConditions() {
        SysRoleQuery query = new SysRoleQuery();
        query.setCode("admin");
        query.setExcludeId(2);

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("code =")
                .contains("id <>");
    }

    @Test
    void buildsUserAvatarConditionWithoutPageAlias() {
        SysUserQuery query = new SysUserQuery();
        query.setAvatar("avatar.png");

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("avatar =")
                .doesNotContain("u.avatar");
    }

    @Test
    void buildsMenuParentCondition() {
        SysMenuQuery query = new SysMenuQuery();
        query.setParentId(0);

        assertThat(DynamicCondition.toWrapper(query).getSqlSegment())
                .contains("parent_id =");
    }
}
