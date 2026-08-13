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
                "ENGINE = InnoDB AUTO_INCREMENT = 150 CHARACTER SET = utf8mb3",
                "INSERT INTO `sys_menu` VALUES (139, '4', 'online', "
                        + "'/monitor/online/index', '在线用户', 2, 'antd:TeamOutlined', 'MENU'",
                "INSERT INTO `sys_menu` VALUES (140, '139', '', '', '列表', 1, '', "
                        + "'BUTTON'",
                "'sys:online'",
                "INSERT INTO `sys_menu` VALUES (141, '139', '', '', '强退', 2, '', "
                        + "'BUTTON'",
                "'sys:online:forceLogout'");
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
