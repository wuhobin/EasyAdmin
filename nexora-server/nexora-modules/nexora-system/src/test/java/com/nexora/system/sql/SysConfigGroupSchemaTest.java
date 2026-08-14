package com.nexora.system.sql;

import com.nexora.system.entity.SysConfigGroup;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SysConfigGroupSchemaTest {

    @Test
    void entityContainsOnlyTheSupportedBusinessColumns() {
        Set<String> fields = Arrays.stream(SysConfigGroup.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "id", "groupCode", "groupName", "configValue", "sort");
    }

    @Test
    void initializationSqlContainsGroupedConfigurationAndOnlySupportedPermissions() throws Exception {
        Path repositoryRoot = repositoryRoot();
        String initializationSql = Files.readString(
                repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);

        assertThat(initializationSql).contains("CREATE TABLE `sys_config_group`");
        assertThat(initializationSql).contains(
                "'system', '系统配置'",
                "'register', '注册配置'",
                "'login', '登录配置'",
                "'password', '密码配置'",
                "'email', '邮箱配置'",
                "'wechat', '微信登录配置'");
        assertThat(initializationSql).contains("`group_code` varchar(50)");
        assertThat(initializationSql).contains("`config_value` json");
        assertThat(initializationSql).contains("UNIQUE INDEX `uk_sys_config_group_code`");
        assertThat(initializationSql).contains("/system/config/index");
        assertThat(initializationSql).contains("sys:config:list");
        assertThat(initializationSql).contains("sys:config:update");
        assertThat(initializationSql).doesNotContain("sys:config:add");
        assertThat(initializationSql).doesNotContain("sys:config:delete");
        assertThat(initializationSql).doesNotContain("CREATE TABLE `sys_config`");
    }

    @Test
    void captchaConfigurationBelongsToRegistration() throws Exception {
        Path repositoryRoot = repositoryRoot();
        String initializationSql = Files.readString(
                repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);
        String registerInsert = initializationSql.lines()
                .filter(line -> line.contains("'register', '注册配置'"))
                .findFirst()
                .orElseThrow();
        String loginInsert = initializationSql.lines()
                .filter(line -> line.contains("'login', '登录配置'"))
                .findFirst()
                .orElseThrow();

        assertThat(registerInsert).contains("\"captchaEnabled\":true");
        assertThat(registerInsert).doesNotContain("\"enabled\"");
        assertThat(loginInsert).doesNotContain("\"captchaEnabled\"");

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
