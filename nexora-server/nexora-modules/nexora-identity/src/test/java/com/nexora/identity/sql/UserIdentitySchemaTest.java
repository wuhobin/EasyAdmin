package com.nexora.identity.sql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdentitySchemaTest {

    @Test
    void initializationSqlSupportsNullableEmailAndExternalIdentity() throws Exception {
        Path repositoryRoot = repositoryRoot();
        String sql = Files.readString(repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);
        String userTable = sql.substring(
                sql.indexOf("CREATE TABLE `sys_user`"),
                sql.indexOf("-- Records of sys_user"));

        assertThat(userTable).doesNotContain("`username`");
        assertThat(userTable).contains("`nickname` varchar(100)", "NOT NULL COMMENT '昵称'");
        assertThat(userTable).contains("`email` varchar(255)", "NULL DEFAULT NULL COMMENT '登录邮箱'");
        assertThat(userTable).contains("UNIQUE INDEX `uk_sys_user_email`");
        assertThat(sql).contains(
                "CREATE TABLE `user_identity`",
                "`provider_app_id` varchar(64)",
                "`provider_user_id` varchar(128)",
                "UNIQUE KEY `uk_user_identity_provider_user`");
        assertThat(sql).contains("`user_id` int NULL DEFAULT NULL COMMENT '操作用户ID'");
    }

    private static Path repositoryRoot() {
        Path currentPath = Path.of("").toAbsolutePath().normalize();
        while (currentPath != null) {
            if (Files.isRegularFile(currentPath.resolve("nexora-admin.sql"))
                    && Files.isDirectory(currentPath.resolve("deploy"))) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }
        throw new IllegalStateException("Unable to locate the repository root");
    }
}
