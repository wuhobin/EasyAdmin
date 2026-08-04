DELIMITER $$

CREATE PROCEDURE `validate_sys_oss_file_uploader`()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM `sys_oss_file`
        WHERE `uploader_id` IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'sys_oss_file contains rows without uploader_id';
    END IF;
END$$

CALL `validate_sys_oss_file_uploader`()$$
DROP PROCEDURE `validate_sys_oss_file_uploader`$$

DELIMITER ;

ALTER TABLE `sys_oss_file`
    MODIFY COLUMN `uploader_id` bigint NOT NULL COMMENT '上传人ID',
    DROP COLUMN `uploader_name`,
    DROP INDEX `idx_sys_oss_file_uploader`,
    ADD INDEX `idx_sys_oss_file_uploader` (`uploader_id` ASC) USING BTREE;
