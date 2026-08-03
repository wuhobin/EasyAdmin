package com.nexora.identity.sql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdentitySchemaTest {

    @Test
    void initializationSqlUsesEmailAsTheOnlyLoginIdentifier() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String sql = Files.readString(repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);
        String userTable = sql.substring(
                sql.indexOf("CREATE TABLE `sys_user`"),
                sql.indexOf("-- Records of sys_user"));

        assertThat(userTable).doesNotContain("`username`");
        assertThat(userTable).contains("`nickname` varchar(100)", "NOT NULL COMMENT '昵称'");
        assertThat(userTable).contains("`email` varchar(255)", "NOT NULL COMMENT '登录邮箱'");
        assertThat(userTable).contains("UNIQUE INDEX `uk_sys_user_email`");
        assertThat(sql).contains("`user_id` int NULL DEFAULT NULL COMMENT '操作用户ID'");
    }
}
