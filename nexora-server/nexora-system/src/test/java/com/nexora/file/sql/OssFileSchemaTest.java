package com.nexora.file.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OssFileSchemaTest {

    @Test
    void initializationSqlContainsTheFileTableAndMenuPermissions() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String initializationSql = Files.readString(repositoryRoot.resolve("nexora-admin.sql"));

        assertThat(initializationSql).contains("CREATE TABLE `sys_oss_file`");
        assertThat(initializationSql).contains("`id` bigint NOT NULL AUTO_INCREMENT");
        assertThat(initializationSql).contains("`file_id` varchar(64)");
        assertThat(initializationSql)
                .contains("UNIQUE INDEX `uk_sys_oss_file_file_id`(`file_id` ASC) USING BTREE");
        assertThat(initializationSql).contains("`uploader_id` bigint NOT NULL COMMENT '上传人ID'");
        assertThat(initializationSql)
                .contains("INDEX `idx_sys_oss_file_uploader`(`uploader_id` ASC) USING BTREE");
        assertThat(initializationSql).doesNotContain("`uploader_name`");
        assertThat(initializationSql).contains("/file/index");
        assertThat(initializationSql).contains("sys:file:list");
        assertThat(initializationSql).contains("sys:file:upload");
        assertThat(initializationSql).contains("sys:file:download");
        assertThat(initializationSql).contains("sys:file:delete");
    }

    @Test
    void incrementalSqlRejectsNullUploadersWithoutMutatingBusinessData() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String migrationSql = Files.readString(repositoryRoot.resolve(
                "deploy/sql/20260730_sys_oss_file_uploader.sql"));

        assertThat(migrationSql).contains("WHERE `uploader_id` IS NULL");
        assertThat(migrationSql)
                .contains("MODIFY COLUMN `uploader_id` bigint NOT NULL COMMENT '上传人ID'");
        assertThat(migrationSql).contains("DROP COLUMN `uploader_name`");
        assertThat(migrationSql).contains("DROP INDEX `idx_sys_oss_file_uploader`");
        assertThat(migrationSql)
                .contains("ADD INDEX `idx_sys_oss_file_uploader` (`uploader_id` ASC) USING BTREE");
        assertThat(migrationSql)
                .doesNotContain("UPDATE `sys_oss_file`")
                .doesNotContain("INSERT INTO `sys_oss_file`")
                .doesNotContain("DELETE FROM `sys_oss_file`")
                .doesNotContain("TRUNCATE TABLE `sys_oss_file`");
    }
}
