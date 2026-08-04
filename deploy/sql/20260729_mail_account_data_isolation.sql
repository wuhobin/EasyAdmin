ALTER TABLE `mail_account`
    ADD COLUMN `owner_id` int NOT NULL COMMENT '所属用户ID' AFTER `id`,
    DROP INDEX `uk_mail_account_email`,
    ADD UNIQUE INDEX `uk_mail_account_owner_email` (`owner_id` ASC, `email` ASC) USING BTREE;
