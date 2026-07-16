CREATE TABLE IF NOT EXISTS `sys_oss_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` varchar(64) NOT NULL COMMENT '文件唯一ID',
  `file_url` varchar(1000) NOT NULL COMMENT 'OSS访问地址',
  `file_name` varchar(255) NOT NULL COMMENT 'OSS保存文件名',
  `original_filename` varchar(255) NULL DEFAULT NULL COMMENT '原始文件名',
  `content_type` varchar(128) NULL DEFAULT NULL COMMENT 'MIME类型',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `platform` varchar(64) NULL DEFAULT NULL COMMENT '存储平台',
  `thumbnail_url` varchar(1000) NULL DEFAULT NULL COMMENT '缩略图地址',
  `uploader_id` bigint NULL DEFAULT NULL COMMENT '上传人ID',
  `uploader_name` varchar(100) NULL DEFAULT NULL COMMENT '上传人用户名',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_oss_file_file_id` (`file_id`) USING BTREE,
  INDEX `idx_sys_oss_file_url` (`file_url`(255)) USING BTREE,
  INDEX `idx_sys_oss_file_original_name` (`original_filename`) USING BTREE,
  INDEX `idx_sys_oss_file_content_type` (`content_type`) USING BTREE,
  INDEX `idx_sys_oss_file_uploader` (`uploader_id`, `uploader_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'OSS文件流水表' ROW_FORMAT = DYNAMIC;

INSERT INTO `sys_menu` (`parent_id`, `path`, `component`, `title`, `sort`, `icon`, `type`,
                        `create_time`, `update_time`, `redirect`, `name`, `hidden`, `perm`, `is_external`)
SELECT '0', '/file', 'Layout', '文件管理', 10, 'FolderOpened', 'CATALOG',
       NOW(), NULL, '/file/list', '', 0, '', 0
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `path` = '/file' AND `component` = 'Layout' AND `type` = 'CATALOG'
);

SET @file_catalog_id = (
  SELECT `id` FROM `sys_menu`
  WHERE `path` = '/file' AND `component` = 'Layout' AND `type` = 'CATALOG'
  ORDER BY `id` LIMIT 1
);

UPDATE `sys_menu`
SET `redirect` = '/file/list', `hidden` = 0, `icon` = 'FolderOpened'
WHERE `id` = @file_catalog_id;

INSERT INTO `sys_menu` (`parent_id`, `path`, `component`, `title`, `sort`, `icon`, `type`,
                        `create_time`, `update_time`, `redirect`, `name`, `hidden`, `perm`, `is_external`)
SELECT CAST(@file_catalog_id AS CHAR), 'list', '/file/index', '文件列表', 1, 'Files', 'MENU',
       NOW(), NULL, '', '', 0, '', 0
WHERE @file_catalog_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `component` = '/file/index' AND `type` = 'MENU'
  );

SET @file_list_id = (
  SELECT `id` FROM `sys_menu`
  WHERE `component` = '/file/index' AND `type` = 'MENU'
  ORDER BY `id` LIMIT 1
);

UPDATE `sys_menu`
SET `parent_id` = CAST(@file_catalog_id AS CHAR), `path` = 'list', `title` = '文件列表',
    `sort` = 1, `icon` = 'Files', `hidden` = 0
WHERE `id` = @file_list_id;

INSERT INTO `sys_menu` (`parent_id`, `path`, `component`, `title`, `sort`, `icon`, `type`,
                        `create_time`, `update_time`, `redirect`, `name`, `hidden`, `perm`, `is_external`)
SELECT CAST(@file_list_id AS CHAR), '', '', '文件列表', 1, '', 'BUTTON',
       NOW(), NULL, '', '', 1, 'sys:file:list', 0
WHERE @file_list_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perm` = 'sys:file:list');

UPDATE `sys_menu`
SET `parent_id` = CAST(@file_list_id AS CHAR), `sort` = 2
WHERE `perm` = 'sys:file:upload' AND @file_list_id IS NOT NULL;

UPDATE `sys_menu`
SET `parent_id` = CAST(@file_list_id AS CHAR), `sort` = 3
WHERE `perm` = 'sys:file:delete' AND @file_list_id IS NOT NULL;
