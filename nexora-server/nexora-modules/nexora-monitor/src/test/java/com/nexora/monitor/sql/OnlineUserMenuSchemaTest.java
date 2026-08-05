package com.nexora.monitor.sql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineUserMenuSchemaTest {

    @Test
    void initializationSqlContainsOnlineUserMenuAndPermissions() throws Exception {
        String initializationSql = Files.readString(
                repositoryRoot().resolve("nexora-admin.sql"), StandardCharsets.UTF_8);

        assertThat(initializationSql).contains(
                "ENGINE = InnoDB AUTO_INCREMENT = 142 CHARACTER SET = utf8mb3",
                "INSERT INTO `sys_menu` VALUES (139, '4', 'online', "
                        + "'/monitor/online/index', '在线用户', 2, 'UserFilled', 'MENU'",
                "INSERT INTO `sys_menu` VALUES (140, '139', '', '', '列表', 1, '', "
                        + "'BUTTON'",
                "'sys:online'",
                "INSERT INTO `sys_menu` VALUES (141, '139', '', '', '强退', 2, '', "
                        + "'BUTTON'",
                "'sys:online:forceLogout'");
    }

    @Test
    void initializationAndMigrationSqlContainOnlineSessionCleanupJob() throws Exception {
        Path repositoryRoot = repositoryRoot();
        String initializationSql = Files.readString(
                repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);
        String migrationSql = Files.readString(repositoryRoot.resolve(
                "deploy/sql/20260805_online_session_cleanup_job.sql"), StandardCharsets.UTF_8);

        assertThat(initializationSql).contains(
                "'在线会话清理', 'SYSTEM', '0 0/10 * * * ?', "
                        + "'onlineSessionCleanupTask.cleanupInvalidSessions()', '1', '3', '0'");
        assertThat(migrationSql).contains(
                "'0 0/10 * * * ?'",
                "'onlineSessionCleanupTask.cleanupInvalidSessions()'",
                "WHERE NOT EXISTS");
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
