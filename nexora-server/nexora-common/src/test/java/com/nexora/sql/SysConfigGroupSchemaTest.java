package com.nexora.sql;

import com.nexora.entity.SysConfigGroup;
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
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String initializationSql = Files.readString(
                repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);

        assertThat(initializationSql).contains("CREATE TABLE `sys_config_group`");
        assertThat(initializationSql).contains(
                "'system', '系统配置'",
                "'register', '注册配置'",
                "'login', '登录配置'",
                "'password', '密码配置'",
                "'email', '邮箱配置'");
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
    void incrementalSqlMigratesTheTwoLegacyValuesAndDropsLegacyObjects() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String migrationSql = Files.readString(repositoryRoot.resolve(
                Path.of("deploy", "sql", "20260731_replace_sys_config.sql")), StandardCharsets.UTF_8);

        assertThat(migrationSql).contains(
                "CREATE TABLE `sys_config_group`",
                "('system', '系统配置'",
                "('register', '注册配置'",
                "('login', '登录配置'",
                "('password', '密码配置'",
                "WHERE `config_key` = 'register.enabled'",
                "WHERE `config_key` = 'register.role-code'",
                "DELETE FROM `sys_role_menu` WHERE `menu_id` IN (131, 133)",
                "DELETE FROM `sys_menu` WHERE `id` IN (131, 133)",
                "DROP TABLE `sys_config`");
        assertThat(migrationSql).doesNotContain(
                "`group_icon`", "`status`", "`remark`", "`version`");
    }
}
