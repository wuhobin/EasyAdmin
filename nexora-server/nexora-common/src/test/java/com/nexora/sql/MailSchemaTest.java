package com.nexora.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MailSchemaTest {

    @Test
    void initializationSqlContainsMailAccountDictionaryAndPermissionsWithoutMailHistoryTable() throws Exception {
        Path repositoryRoot = Path.of("..", "..").toAbsolutePath().normalize();
        String initializationSql = Files.readString(repositoryRoot.resolve("aurora-admin.sql"), StandardCharsets.UTF_8);

        assertThat(initializationSql).contains("CREATE TABLE `mail_account`");
        assertThat(initializationSql).contains("`auth_code_ciphertext` varchar(1000)");
        assertThat(initializationSql).contains("UNIQUE INDEX `uk_mail_account_email`");
        assertThat(initializationSql).contains("/mail/index");
        assertThat(initializationSql).contains("/mail/account/index");
        assertThat(initializationSql).contains("mail:inbox:list");
        assertThat(initializationSql).contains("mail:inbox:view");
        assertThat(initializationSql).contains("'mail_provider'");
        assertThat(initializationSql).contains("'QQ'");
        assertThat(initializationSql).contains("'NETEASE_163'");
        assertThat(initializationSql).doesNotContain("CREATE TABLE `mail_message`");
    }
}
