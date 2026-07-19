package com.aurora.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MailSchemaTest {

    @Test
    void initializationSqlContainsMailAccountJobAndPermissionsWithoutMailHistoryTable() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String initializationSql = Files.readString(repositoryRoot.resolve("aurora-admin.sql"));

        assertThat(initializationSql).contains("CREATE TABLE `mail_account`");
        assertThat(initializationSql).contains("`auth_code_ciphertext` varchar(1000) NOT NULL");
        assertThat(initializationSql).contains("UNIQUE INDEX `uk_mail_account_email` (`email`)");
        assertThat(initializationSql).contains("mailFetchTask.checkNewMails");
        assertThat(initializationSql).contains("/mail/index");
        assertThat(initializationSql).contains("/mail/account/index");
        assertThat(initializationSql).contains("'邮箱列表'");
        assertThat(initializationSql).contains("mail:inbox:list");
        assertThat(initializationSql).contains("mail:inbox:view");
        assertThat(initializationSql).doesNotContain("CREATE TABLE `mail_message`");
    }
}
