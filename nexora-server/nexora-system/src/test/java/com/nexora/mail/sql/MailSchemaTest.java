package com.nexora.mail.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MailSchemaTest {

    @Test
    void initializationSqlContainsMailAccountDictionaryAndPermissionsWithoutMailHistoryTable() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String initializationSql = Files.readString(repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);

        assertThat(initializationSql).contains("CREATE TABLE `mail_account`");
        assertThat(initializationSql).contains("`owner_id` int NOT NULL");
        assertThat(initializationSql).contains("`auth_code_ciphertext` varchar(1000)");
        assertThat(initializationSql).contains("UNIQUE INDEX `uk_mail_account_owner_email`");
        assertThat(initializationSql).contains(
                "`uk_mail_account_owner_email`(`owner_id` ASC, `email` ASC)");
        assertThat(initializationSql).contains("/mail/index");
        assertThat(initializationSql).contains("/mail/account/index");
        assertThat(initializationSql).contains("mail:inbox:list");
        assertThat(initializationSql).contains("mail:inbox:view");
        assertThat(initializationSql).contains("'mail_provider'");
        assertThat(initializationSql).contains("'QQ'");
        assertThat(initializationSql).contains("'NETEASE_163'");
        assertThat(initializationSql).doesNotContain("CREATE TABLE `mail_message`");

        String migrationSql = Files.readString(repositoryRoot.resolve(
                "deploy/sql/20260729_mail_account_data_isolation.sql"), StandardCharsets.UTF_8);
        assertThat(migrationSql).contains("ADD COLUMN `owner_id` int NOT NULL");
        assertThat(migrationSql).contains("DROP INDEX `uk_mail_account_email`");
        assertThat(migrationSql).contains("ADD UNIQUE INDEX `uk_mail_account_owner_email`");

        String mapperXml = Files.readString(repositoryRoot.resolve(
                "nexora-server/nexora-boot/src/main/resources/mapper/MailAccountMapper.xml"),
                StandardCharsets.UTF_8);
        assertThat(mapperXml).contains("INNER JOIN sys_user su ON su.id = ma.owner_id");
        assertThat(mapperXml).contains("AND su.status = 1");
    }
}
