package com.nexora.sql;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.nexora.entity.SysConfig;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SysConfigSchemaTest {

    @Test
    void remarkCanBeClearedDuringUpdate() throws Exception {
        TableField tableField = SysConfig.class.getDeclaredField("remark").getAnnotation(TableField.class);

        assertThat(tableField).isNotNull();
        assertThat(tableField.updateStrategy()).isEqualTo(FieldStrategy.ALWAYS);
    }

    @Test
    void initializationSqlContainsConfigTableMenuAndPermissions() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String initializationSql = Files.readString(
                repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);

        assertThat(initializationSql).contains("CREATE TABLE `sys_config`");
        assertThat(initializationSql).contains("`config_key` varchar(128)");
        assertThat(initializationSql).contains("`config_value` varchar(512)");
        assertThat(initializationSql).contains("UNIQUE INDEX `uk_sys_config_key`");
        assertThat(initializationSql).contains("/system/config/index");
        assertThat(initializationSql).contains("sys:config:list");
        assertThat(initializationSql).contains("sys:config:add");
        assertThat(initializationSql).contains("sys:config:update");
        assertThat(initializationSql).contains("sys:config:delete");
    }
}
