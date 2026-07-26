package com.nexora.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OssFileSchemaTest {

    @Test
    void initializationSqlContainsTheFileTableAndMenuPermission() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String initializationSql = Files.readString(repositoryRoot.resolve("aurora-admin.sql"));

        assertThat(initializationSql).contains("sys_oss_file");
        assertThat(initializationSql).contains("`id` bigint NOT NULL AUTO_INCREMENT");
        assertThat(initializationSql).contains("`file_id` varchar(64) NOT NULL");
        assertThat(initializationSql).contains("UNIQUE INDEX `uk_sys_oss_file_file_id` (`file_id`)");
        assertThat(initializationSql).contains("`uploader_name` varchar(100)");
        assertThat(initializationSql).contains("/file/index");
        assertThat(initializationSql).contains("sys:file:list");
        assertThat(initializationSql).contains("sys:file:download");
    }
}
