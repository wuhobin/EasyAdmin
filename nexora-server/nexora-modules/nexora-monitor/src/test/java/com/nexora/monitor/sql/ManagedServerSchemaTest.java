package com.nexora.monitor.sql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedServerSchemaTest {

    @Test
    void initializationSqlContainsOwnedServerTableAndGlobalPermissions() throws Exception {
        String sql = Files.readString(
                repositoryRoot().resolve("nexora-admin.sql"), StandardCharsets.UTF_8);

        assertThat(sql).contains(
                "CREATE TABLE `monitor_server`",
                "`owner_id` int NOT NULL COMMENT '所属用户ID'",
                "`password_ciphertext` varchar(4096)",
                "INDEX `idx_monitor_server_owner_sort`(`owner_id` ASC",
                "'/monitor/server/index'",
                "'monitor:server:list'",
                "'monitor:server:terminal'",
                "INSERT INTO `sys_role_menu` VALUES (549, 1, 142)",
                "INSERT INTO `sys_role_menu` VALUES (557, 20, 142)");
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
