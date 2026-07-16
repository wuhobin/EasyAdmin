package com.aurora.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OssFileSchemaTest {

    @Test
    void initializationAndUpgradeSqlContainTheFileTableAndMenuPermission() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String initializationSql = Files.readString(repositoryRoot.resolve("aurora-admin.sql"));
        String upgradeSql = Files.readString(
                repositoryRoot.resolve("docs/sql/2026-07-16-oss-file-management.sql"));
        String gitignore = Files.readString(repositoryRoot.resolve(".gitignore"));

        for (String sql : new String[]{initializationSql, upgradeSql}) {
            assertThat(sql).contains("sys_oss_file");
            assertThat(sql).contains("`id` bigint NOT NULL AUTO_INCREMENT");
            assertThat(sql).contains("`file_id` varchar(64) NOT NULL");
            assertThat(sql).contains("UNIQUE INDEX `uk_sys_oss_file_file_id` (`file_id`)");
            assertThat(sql).contains("`uploader_name` varchar(100)");
            assertThat(sql).contains("/file/index");
            assertThat(sql).contains("sys:file:list");
        }
        assertThat(upgradeSql).contains("CREATE TABLE IF NOT EXISTS");
        assertThat(upgradeSql).doesNotContain("SELECT 109", "SELECT 110");
        assertThat(gitignore).contains("!docs/sql/", "!docs/sql/2026-07-16-oss-file-management.sql");
    }
}
