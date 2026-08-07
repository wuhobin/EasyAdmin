package com.nexora.file.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OssFileSchemaTest {

    @Test
    void initializationSqlContainsTheFileTableAndMenuPermissions() throws Exception {
        Path repositoryRoot = repositoryRoot();
        String initializationSql = Files.readString(repositoryRoot.resolve("nexora-admin.sql"));
        String migrationSql = Files.readString(repositoryRoot.resolve("deploy/migrations/20260807_file_groups.sql"));

        assertThat(initializationSql).contains("CREATE TABLE `sys_oss_file`");
        assertThat(initializationSql).contains("`id` bigint NOT NULL AUTO_INCREMENT");
        assertThat(initializationSql).contains("`file_id` varchar(64)");
        assertThat(initializationSql)
                .contains("UNIQUE INDEX `uk_sys_oss_file_file_id`(`file_id` ASC) USING BTREE");
        assertThat(initializationSql).contains("`uploader_id` bigint NOT NULL COMMENT '上传人ID'");
        assertThat(initializationSql)
                .contains("INDEX `idx_sys_oss_file_uploader`(`uploader_id` ASC) USING BTREE");
        assertThat(initializationSql).doesNotContain("`uploader_name`");
        assertThat(initializationSql).contains("`group_id` bigint NULL DEFAULT NULL");
        assertThat(initializationSql)
                .contains("INDEX `idx_sys_oss_file_group`(`group_id` ASC) USING BTREE");
        assertThat(initializationSql).contains("CREATE TABLE `sys_oss_file_group`");
        assertThat(initializationSql)
                .contains("UNIQUE INDEX `uk_sys_oss_file_group_owner_name`(`owner_id` ASC, `name` ASC) USING BTREE");
        assertThat(initializationSql).contains("CONSTRAINT `fk_sys_oss_file_group`");
        assertThat(initializationSql).contains("ON DELETE SET NULL");
        assertThat(initializationSql).contains("/file/index");
        assertThat(initializationSql).contains("sys:file:list");
        assertThat(initializationSql).contains("sys:file:upload");
        assertThat(initializationSql).contains("sys:file:download");
        assertThat(initializationSql).contains("sys:file:delete");
        assertThat(migrationSql).contains("ADD COLUMN group_id BIGINT");
        assertThat(migrationSql).contains("CREATE TABLE sys_oss_file_group");
        assertThat(migrationSql).contains("uk_sys_oss_file_group_owner_name");
        assertThat(migrationSql).contains("ADD CONSTRAINT fk_sys_oss_file_group");
        assertThat(migrationSql).contains("ON DELETE SET NULL");
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
