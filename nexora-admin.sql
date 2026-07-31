/*
 Navicat Premium Data Transfer

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80046
 Source Host           : 127.0.0.1:3306
 Source Schema         : nexora-admin

 Target Server Type    : MySQL
 Target Server Version : 80046
 File Encoding         : 65001

 Date: 31/07/2026 12:06:49
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for mail_account
-- ----------------------------
DROP TABLE IF EXISTS `mail_account`;
CREATE TABLE `mail_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_id` int NOT NULL COMMENT '所属用户ID',
  `account_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账户名称',
  `provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱类型',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱地址',
  `auth_code_ciphertext` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '加密后的邮箱授权码',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `last_uid` bigint NULL DEFAULT NULL COMMENT '后台检查到的最新邮件UID',
  `uid_validity` bigint NULL DEFAULT NULL COMMENT 'IMAP UID有效性标识',
  `last_connect_time` datetime NULL DEFAULT NULL COMMENT '最后连接时间',
  `last_error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后连接错误',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mail_account_owner_email`(`owner_id` ASC, `email` ASC) USING BTREE,
  INDEX `idx_mail_account_enabled_sort`(`enabled` ASC, `sort` ASC) USING BTREE,
  INDEX `idx_mail_account_owner_sort`(`owner_id` ASC, `sort` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '聚合邮箱账户' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of mail_account
-- ----------------------------
INSERT INTO `mail_account` VALUES (1, 1, 'QQ邮箱', 'QQ', '1289066006@qq.com', 'v1:2jdaO+DSabB/rHuv:YDskUtAw10RUebiHvDOO8SEOIeoYacs47qcdPRFayfI=', 1, 0, 3066, 1763133047, '2026-07-29 09:51:57', '', '2026-07-19 21:23:12', '2026-07-29 09:51:57');
INSERT INTO `mail_account` VALUES (2, 1, '网易邮箱', 'NETEASE_163', 'wuhongbinyos@163.com', 'v1:PraA3Styz5bgaZlp:ep5JyA4rcv3kDWYmOpVbrVwfjkRx9F68dB70h5B0NpY=', 1, 1, 1672974450, 1, '2026-07-29 09:51:56', '', '2026-07-19 21:54:58', '2026-07-29 09:51:56');
INSERT INTO `mail_account` VALUES (3, 1, '126邮箱', 'NETEASE_126', 'wuhobin@126.com', 'v1:mvC+jFbn0KjEsPmq:vEjeTZKk1bCenSFkmcjODllLW/21+s4sdUoLwoGAM08=', 1, 3, 1784626138, 1, '2026-07-29 09:51:56', '', '2026-07-21 17:29:44', '2026-07-29 09:51:56');

-- ----------------------------
-- Table structure for quartz_job
-- ----------------------------
DROP TABLE IF EXISTS `quartz_job`;
CREATE TABLE `quartz_job`  (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Cron表达式',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '是否并发',
  `misfire_policy` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT 'misfire策略',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态',
  PRIMARY KEY (`job_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quartz_job
-- ----------------------------
INSERT INTO `quartz_job` VALUES (3, '邮箱新邮件检查', 'SYSTEM', '0 0 0 * * ?', 'mailFetchTask.checkNewMails()', '1', '3', '0');

-- ----------------------------
-- Table structure for quartz_job_log
-- ----------------------------
DROP TABLE IF EXISTS `quartz_job_log`;
CREATE TABLE `quartz_job_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `job_id` bigint NULL DEFAULT NULL COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务分组',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调用目标字符串',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `stop_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `cost_millis` bigint NULL DEFAULT NULL COMMENT '耗时(毫秒)',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务消息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '执行状态',
  `exception_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '异常信息',
  PRIMARY KEY (`log_id`) USING BTREE,
  INDEX `idx_job_id`(`job_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 59 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务执行日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quartz_job_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置值',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统全局配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, 'register.enabled', 'true', '是否开始注册，开启后前端展示注册按钮', '2026-07-27 13:53:56', '2026-07-28 16:30:56');
INSERT INTO `sys_config` VALUES (2, 'register.role-code', 'user', '用户注册之后默认用户角色', '2026-07-28 17:17:27', '2026-07-28 17:17:27');

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典名称',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典类型',
  `status` int NOT NULL DEFAULT 1 COMMENT '是否发布(1:是，0:否)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (1, '文件类型', 'file_content_type', 1, '文件MIME类型分类', '2026-07-17 11:50:39', '2026-07-17 11:50:39', 1);
INSERT INTO `sys_dict` VALUES (34, '邮箱类型', 'mail_provider', 1, '聚合邮箱支持的邮箱服务商', '2026-07-20 00:00:00', '2026-07-20 00:00:00', 2);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_id` bigint NOT NULL COMMENT '字典类型id',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典标签',
  `value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典键值',
  `style` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '回显样式',
  `is_default` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否默认（1是 0否）',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `status` int NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (38, 1, 'image/jpeg', 'image/jpeg', 'success', NULL, 1, 'image/jpeg', 1);
INSERT INTO `sys_dict_data` VALUES (39, 1, 'image/gif', 'image/gif', 'danger', NULL, 2, 'image/gif', 1);
INSERT INTO `sys_dict_data` VALUES (40, 1, 'image/webp', 'image/webp', 'warning', NULL, 3, 'image/webp', 1);
INSERT INTO `sys_dict_data` VALUES (41, 1, 'video/mp4', 'video/mp4', 'primary', NULL, 4, 'video/mp4', 1);
INSERT INTO `sys_dict_data` VALUES (42, 1, 'application/pdf', 'application/pdf', 'info', NULL, 5, 'application/pdf', 1);
INSERT INTO `sys_dict_data` VALUES (43, 1, 'application/zip', 'application/zip', 'info', NULL, 6, 'application/zip', 1);
INSERT INTO `sys_dict_data` VALUES (44, 1, 'image/png', 'image/png', 'success', NULL, 0, 'image/png', 1);
INSERT INTO `sys_dict_data` VALUES (45, 1, 'text/plain', 'text/plain', 'info', NULL, 8, 'text/plain', 1);
INSERT INTO `sys_dict_data` VALUES (46, 34, 'QQ 邮箱', 'QQ', 'primary', '1', 1, 'QQ 邮箱', 1);
INSERT INTO `sys_dict_data` VALUES (47, 34, '163 邮箱', 'NETEASE_163', 'success', '0', 2, '网易 163 邮箱', 1);
INSERT INTO `sys_dict_data` VALUES (48, 34, '126 邮箱', 'NETEASE_126', 'warning', '0', 3, '网易 126 邮箱', 1);
INSERT INTO `sys_dict_data` VALUES (49, 34, 'yeah 邮箱', 'YEAH', 'info', '0', 4, '网易 yeah 邮箱', 1);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '上级资源ID',
  `path` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '路由路径',
  `component` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '组件路径',
  `title` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '菜单名称',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `icon` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '资源图标',
  `type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '类型 menu、button',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `redirect` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '重定向地址',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '跳转地址',
  `hidden` int NULL DEFAULT NULL COMMENT '是否隐藏',
  `perm` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `is_external` int NULL DEFAULT 0 COMMENT '是否外链 0:否  1:是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 135 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '权限资源表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '0', '/system', 'Layout', '系统管理', 4, 'Setting', 'CATALOG', '2019-03-28 18:51:08', '2021-12-17 15:26:06', '/system/user', '', 0, NULL, 0);
INSERT INTO `sys_menu` VALUES (2, '1', 'role', '/system/role/index', '角色管理', 2, 'Avatar', 'MENU', '2019-03-30 14:00:03', '2021-11-16 15:40:42', '', '', 0, NULL, 0);
INSERT INTO `sys_menu` VALUES (3, '2', NULL, NULL, '列表', 1, '', 'BUTTON', NULL, '2024-11-18 17:56:16', '', NULL, 1, 'system:role', 0);
INSERT INTO `sys_menu` VALUES (4, '0', '/monitor', 'Layout', '监控中心', 5, 'Monitor', 'CATALOG', NULL, '2024-11-17 21:38:25', '/monitor/server', '', 0, NULL, 0);
INSERT INTO `sys_menu` VALUES (7, '6', '', '', '新增', 1, '', 'BUTTON', NULL, '2024-11-16 14:18:24', NULL, '', 1, 'a:b:add', 0);
INSERT INTO `sys_menu` VALUES (8, '2', '', '', '新增', 1, '', 'BUTTON', NULL, '2024-11-21 22:16:56', NULL, '', 1, 'sys:role:add', 0);
INSERT INTO `sys_menu` VALUES (9, '1', 'dict', '/system/dict/index', '字典管理', 3, 'Memo', 'MENU', '2024-11-17 21:29:51', '2024-11-17 21:39:06', NULL, '', 0, NULL, 0);
INSERT INTO `sys_menu` VALUES (10, '9', '', '', '新增', 2, '', 'BUTTON', '2024-11-17 21:30:23', '2025-01-04 11:19:01', NULL, '', 1, 'sys:dict:add', 0);
INSERT INTO `sys_menu` VALUES (11, '9', '', '', '修改', 2, '', 'BUTTON', '2024-11-17 21:32:34', '2024-11-18 18:01:35', NULL, '', 1, 'sys:dict:update', 0);
INSERT INTO `sys_menu` VALUES (12, '9', '', '', '删除', 3, '', 'BUTTON', '2024-11-17 21:34:33', '2024-11-17 21:39:11', NULL, '', 1, 'sys:dict:delete', 0);
INSERT INTO `sys_menu` VALUES (13, '1', 'menu', '/system/menu/index', '菜单管理', 5, 'Menu', 'MENU', NULL, '2021-11-18 11:26:00', '', 'menu', 0, NULL, 0);
INSERT INTO `sys_menu` VALUES (14, '1', 'user', '/system/user/index', '用户管理', 1, 'User', 'MENU', '2024-11-18 01:10:52', '2024-11-18 01:11:13', NULL, '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (18, '4', 'job', '/monitor/job/index', '定时任务', 3, 'AlarmClock', 'MENU', '2024-11-18 06:57:38', NULL, NULL, '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (19, '4', 'job-log', '/monitor/job/log', '调度日志', 4, 'Document', 'MENU', '2024-11-18 06:58:43', '2026-07-17 17:25:22', NULL, '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (27, '14', '', '', '新增', 2, '', 'BUTTON', '2024-11-18 09:18:14', '2024-11-21 22:15:30', NULL, '', 1, 'sys:user:add', 0);
INSERT INTO `sys_menu` VALUES (28, '14', '', '', '编辑', 2, '', 'BUTTON', '2024-11-18 09:18:38', '2024-11-21 14:38:56', NULL, '', 1, 'sys:user:update', 0);
INSERT INTO `sys_menu` VALUES (29, '14', '', '', '删除', 3, '', 'BUTTON', '2024-11-18 09:18:53', '2024-11-21 22:15:43', NULL, '', 1, 'sys:user:delete', 0);
INSERT INTO `sys_menu` VALUES (32, '1', 'log', '', '日志管理', 5, 'DocumentCopy', 'MENU', '2024-11-18 10:52:47', '2024-11-18 10:58:00', '/system/log/operation', '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (33, '32', 'operation', '/system/log/operation/index', '操作日志', 1, 'CircleCheckFilled', 'MENU', '2024-11-18 10:53:10', '2024-11-18 11:05:22', NULL, '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (39, '14', '', '', '列表', 1, '', 'BUTTON', '2024-11-18 17:54:15', '2024-11-21 22:07:33', '', '', 1, 'sys:user', 0);
INSERT INTO `sys_menu` VALUES (40, '2', '', '', '修改', 2, '', 'BUTTON', '2024-11-18 17:56:10', '2024-11-21 22:04:52', '', '', 1, 'sys:role:update', 0);
INSERT INTO `sys_menu` VALUES (41, '2', '', '', '删除', 3, '', 'BUTTON', '2024-11-18 17:56:34', '2024-11-21 22:17:17', '', NULL, 1, 'sys:role:delete', 0);
INSERT INTO `sys_menu` VALUES (43, '2', '', '', '分配权限', 5, '', 'BUTTON', '2024-11-18 17:57:28', NULL, '', NULL, 1, 'sys:role:menus', 0);
INSERT INTO `sys_menu` VALUES (48, '13', '', '', '添加', 1, '', 'BUTTON', '2024-11-18 17:56:58', NULL, '', NULL, 1, 'sys:menu:add', 0);
INSERT INTO `sys_menu` VALUES (49, '13', '', '', '修改', 2, '', 'BUTTON', '2024-11-18 17:56:58', NULL, '', NULL, 1, 'sys:menu:update', 0);
INSERT INTO `sys_menu` VALUES (50, '13', '', '', '删除', 3, '', 'BUTTON', '2024-11-21 21:55:49', NULL, '', '', 1, 'sys:menu:delete', 0);
INSERT INTO `sys_menu` VALUES (51, '13', '', '', '列表', 1, '', 'BUTTON', '2024-11-21 21:58:29', NULL, '', '', 1, 'sys:menu', 0);
INSERT INTO `sys_menu` VALUES (53, '9', '', '', '列表', 1, '', 'BUTTON', '2024-11-18 17:58:16', NULL, '', '', 1, 'sys:dict', 0);
INSERT INTO `sys_menu` VALUES (54, '1', 'profile', '/system/user/profile/index', '个人中心', 99, 'Avatar', 'MENU', '2024-11-21 22:12:18', '2024-11-21 22:12:46', '', '', 1, '', 0);
INSERT INTO `sys_menu` VALUES (55, '33', '', '', '列表', 1, '', 'BUTTON', '2024-11-18 10:53:10', '2024-11-18 11:05:22', '', '', 1, 'sys:operateLog', 0);
INSERT INTO `sys_menu` VALUES (56, '33', '', '', '删除', 2, '', 'BUTTON', '2024-11-18 10:53:10', '2024-11-18 11:05:22', '', '', 1, 'sys:operateLog:delete', 0);
INSERT INTO `sys_menu` VALUES (58, '18', '', '', '列表', 1, '', 'BUTTON', '2024-11-18 17:56:58', '2024-11-21 22:04:42', '', NULL, 1, 'sys:job', 0);
INSERT INTO `sys_menu` VALUES (59, '18', '', '', '添加', 2, '', 'BUTTON', '2024-11-18 17:56:58', '2024-11-21 22:04:42', '', NULL, 1, 'sys:job:add', 0);
INSERT INTO `sys_menu` VALUES (60, '18', '', '', '修改', 3, '', 'BUTTON', '2024-11-18 17:56:58', '2024-11-21 22:04:42', '', NULL, 1, 'sys:job:update', 0);
INSERT INTO `sys_menu` VALUES (61, '18', '', '', '删除', 4, '', 'BUTTON', '2024-11-18 17:56:58', '2024-11-21 22:04:42', '', NULL, 1, 'sys:job:delete', 0);
INSERT INTO `sys_menu` VALUES (62, '18', '', '', '修改状态', 5, '', 'BUTTON', '2024-11-18 17:56:58', '2024-11-21 22:04:42', '', NULL, 0, 'sys:job:changeStatus', 0);
INSERT INTO `sys_menu` VALUES (63, '19', '', '', '删除', 1, '', 'BUTTON', '2024-11-21 22:26:56', NULL, '', '', 1, 'sys:jobLog:delete', 0);
INSERT INTO `sys_menu` VALUES (64, '19', '', '', '清空', 2, '', 'BUTTON', '2024-11-21 22:27:09', NULL, '', '', 1, 'sys:jobLog:clean', 0);
INSERT INTO `sys_menu` VALUES (66, '19', '', '', '列表', 1, '', 'BUTTON', '2024-11-21 23:09:47', NULL, '', '', 1, 'sys:jobLog', 0);
INSERT INTO `sys_menu` VALUES (111, '0', '/file', 'Layout', '文件管理', 10, 'FolderOpened', 'CATALOG', '2026-07-16 20:08:41', NULL, '/file/list', '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (112, '111', 'list', '/file/index', '文件列表', 1, 'Files', 'MENU', '2026-07-16 20:08:41', NULL, '', '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (113, '112', '', '', '文件列表', 1, '', 'BUTTON', '2026-07-16 20:08:41', NULL, '', '', 1, 'sys:file:list', 0);
INSERT INTO `sys_menu` VALUES (114, '112', '', '', '上传文件', 1, '', 'BUTTON', '2026-07-17 11:03:53', '2026-07-17 11:03:53', '', '', 1, 'sys:file:upload', 0);
INSERT INTO `sys_menu` VALUES (115, '112', '', '', '删除文件', 1, '', 'BUTTON', '2026-07-17 11:04:36', '2026-07-17 11:04:36', '', '', 1, 'sys:file:delete', 0);
INSERT INTO `sys_menu` VALUES (116, '112', '', '', '下载文件', 1, '', 'BUTTON', '2026-07-17 12:31:48', '2026-07-17 12:31:48', '', '', 1, 'sys:file:download', 0);
INSERT INTO `sys_menu` VALUES (117, '0', '/mail', 'Layout', '聚合邮箱', 9, 'Message', 'CATALOG', '2026-07-19 00:00:00', NULL, '/mail/inbox', '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (118, '117', 'inbox', '/mail/index', '最新邮件', 1, 'MessageBox', 'MENU', '2026-07-19 00:00:00', NULL, '', '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (119, '127', '', '', '邮箱账户列表', 1, '', 'BUTTON', '2026-07-19 00:00:00', NULL, '', '', 1, 'mail:account:list', 0);
INSERT INTO `sys_menu` VALUES (120, '127', '', '', '新增邮箱账户', 2, '', 'BUTTON', '2026-07-19 00:00:00', NULL, '', '', 1, 'mail:account:add', 0);
INSERT INTO `sys_menu` VALUES (121, '127', '', '', '修改邮箱账户', 3, '', 'BUTTON', '2026-07-19 00:00:00', NULL, '', '', 1, 'mail:account:update', 0);
INSERT INTO `sys_menu` VALUES (122, '127', '', '', '删除邮箱账户', 4, '', 'BUTTON', '2026-07-19 00:00:00', NULL, '', '', 1, 'mail:account:delete', 0);
INSERT INTO `sys_menu` VALUES (123, '127', '', '', '测试邮箱连接', 5, '', 'BUTTON', '2026-07-19 00:00:00', NULL, '', '', 1, 'mail:account:test', 0);
INSERT INTO `sys_menu` VALUES (124, '118', '', '', '邮件列表', 6, '', 'BUTTON', '2026-07-19 00:00:00', NULL, '', '', 1, 'mail:inbox:list', 0);
INSERT INTO `sys_menu` VALUES (125, '118', '', '', '查看邮件', 7, '', 'BUTTON', '2026-07-19 00:00:00', NULL, '', '', 1, 'mail:inbox:view', 0);
INSERT INTO `sys_menu` VALUES (126, '118', '', '', '下载附件', 8, '', 'BUTTON', '2026-07-19 00:00:00', NULL, '', '', 1, 'mail:inbox:download', 0);
INSERT INTO `sys_menu` VALUES (127, '117', 'account', '/mail/account/index', '邮箱列表', 2, 'Tickets', 'MENU', '2026-07-19 00:00:00', NULL, '', '', 0, '', 0);
INSERT INTO `sys_menu` VALUES (130, '1', 'config', '/system/config/index', '配置管理', 4, 'Tools', 'MENU', '2026-07-27 13:37:19', NULL, '', 'config', 0, '', 0);
INSERT INTO `sys_menu` VALUES (131, '130', '', '', '新增', 1, '', 'BUTTON', '2026-07-27 13:37:19', NULL, '', '', 1, 'sys:config:add', 0);
INSERT INTO `sys_menu` VALUES (132, '130', '', '', '修改', 2, '', 'BUTTON', '2026-07-27 13:37:19', NULL, '', '', 1, 'sys:config:update', 0);
INSERT INTO `sys_menu` VALUES (133, '130', '', '', '删除', 3, '', 'BUTTON', '2026-07-27 13:37:19', NULL, '', '', 1, 'sys:config:delete', 0);
INSERT INTO `sys_menu` VALUES (134, '130', '', '', '查看', 4, '', 'BUTTON', '2026-07-27 13:37:19', NULL, '', '', 1, 'sys:config:list', 0);

-- ----------------------------
-- Table structure for sys_operate_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operate_log`;
CREATE TABLE `sys_operate_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NULL DEFAULT NULL COMMENT '操作用户ID',
  `request_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求接口',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求方式',
  `operation_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作名称',
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'ip',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'ip来源',
  `spend_time` bigint NULL DEFAULT NULL COMMENT '请求接口耗时',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `params_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '请求参数',
  `class_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类地址',
  `method_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法名',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2380 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_operate_log
-- ----------------------------
INSERT INTO `sys_operate_log` VALUES (2363, NULL, '/sys/user/delete/1811', 'DELETE', '批量删除用户', '127.0.0.1', '内网IP|内网IP', 167, '2026-07-24 17:25:33', '{\"ids\":[1811]}', 'com.aurora.controller.system.SysUserController', 'delete', '2026-07-24 17:25:33');
INSERT INTO `sys_operate_log` VALUES (2364, NULL, '/sys/role/delete/2', 'DELETE', '批量删除角色', '127.0.0.1', '内网IP|内网IP', 18, '2026-07-24 17:25:39', '{\"ids\":[2]}', 'com.aurora.controller.system.SysRoleController', 'delete', '2026-07-24 17:25:39');
INSERT INTO `sys_operate_log` VALUES (2365, 1, '/sys/role/', 'POST', '新增角色', '127.0.0.1', '内网IP|内网IP', 94, '2026-07-28 10:34:01', '{\"form\":{\"id\":null,\"code\":\"user\",\"name\":\"普通\",\"remarks\":\"\",\"createTime\":null,\"updateTime\":null,\"params\":null}}', 'com.nexora.controller.system.SysRoleController', 'addRole', '2026-07-28 10:34:01');
INSERT INTO `sys_operate_log` VALUES (2366, 1, '/sys/user', 'POST', '新增用户', '127.0.0.1', '内网IP|内网IP', 163, '2026-07-28 10:34:20', '{\"form\":{\"user\":{\"nickname\":\"测试\",\"password\":\"123456\",\"email\":\"1289066006@qq.com\",\"status\":1,\"avatar\":null,\"mobile\":\"\",\"sex\":1},\"roleIds\":[20]}}', 'com.nexora.controller.system.SysUserController', 'addUser', '2026-07-28 10:34:20');
INSERT INTO `sys_operate_log` VALUES (2367, 1, '/sys/user/delete/1812', 'DELETE', '批量删除用户', '127.0.0.1', '内网IP|内网IP', 119, '2026-07-28 11:03:55', '{\"ids\":[1812]}', 'com.nexora.controller.system.SysUserController', 'delete', '2026-07-28 11:03:55');
INSERT INTO `sys_operate_log` VALUES (2368, 1, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 99, '2026-07-28 12:10:04', '{\"form\":{\"id\":null,\"nickname\":\"系统管理员\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":null,\"mobile\":\"18772916901\",\"sex\":2,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-28 12:10:04');
INSERT INTO `sys_operate_log` VALUES (2369, 1, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 72, '2026-07-28 14:48:47', '{\"form\":{\"id\":null,\"nickname\":\"系统管理员\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260728/6a6850cc101384ebf83b9e3b.jpg\",\"mobile\":\"18772916901\",\"sex\":2,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-28 14:48:47');
INSERT INTO `sys_operate_log` VALUES (2370, 1, '/sys/user/delete/1813', 'DELETE', '批量删除用户', '127.0.0.1', '内网IP|内网IP', 214, '2026-07-28 19:37:41', '{\"ids\":[1813]}', 'com.nexora.controller.system.SysUserController', 'delete', '2026-07-28 19:37:41');
INSERT INTO `sys_operate_log` VALUES (2371, 1, '/sys/role/menus/20', 'PUT', '修改角色权限', '127.0.0.1', '内网IP|内网IP', 69, '2026-07-29 09:53:42', '{\"id\":20,\"menuIds\":[117,118,124,125,126,127,119,120,121,122,123,111,112,116,115,114,113]}', 'com.nexora.controller.system.SysRoleController', 'updateRoleMenus', '2026-07-29 09:53:42');
INSERT INTO `sys_operate_log` VALUES (2372, 1, '/sys/user/delete/1814', 'DELETE', '批量删除用户', '127.0.0.1', '内网IP|内网IP', 156, '2026-07-30 13:19:00', '{\"ids\":[1814]}', 'com.nexora.controller.system.SysUserController', 'delete', '2026-07-30 13:19:00');
INSERT INTO `sys_operate_log` VALUES (2373, 1816, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 54, '2026-07-30 16:01:59', '{\"form\":{\"id\":null,\"nickname\":\"wuhobin\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260730/6a6b04f3101399263cde2e73.jpg\",\"mobile\":null,\"sex\":null,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-30 16:01:59');
INSERT INTO `sys_operate_log` VALUES (2374, 1816, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 16, '2026-07-30 16:04:13', '{\"form\":{\"id\":null,\"nickname\":\"wuhobin\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260730/6a6b057c101399263cde2e74.jpg\",\"mobile\":null,\"sex\":null,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-30 16:04:13');
INSERT INTO `sys_operate_log` VALUES (2375, 1, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 10, '2026-07-30 16:07:14', '{\"form\":{\"id\":null,\"nickname\":\"系统管理员\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260730/6a6b0631101399263cde2e75.jpg\",\"mobile\":\"18772916901\",\"sex\":2,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-30 16:07:14');
INSERT INTO `sys_operate_log` VALUES (2376, 1, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 5, '2026-07-30 16:09:34', '{\"form\":{\"id\":null,\"nickname\":\"系统管理员\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260730/6a6b06bd101399263cde2e76.png\",\"mobile\":\"18772916901\",\"sex\":2,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-30 16:09:34');
INSERT INTO `sys_operate_log` VALUES (2377, 1816, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 7, '2026-07-30 16:11:18', '{\"form\":{\"id\":null,\"nickname\":\"wuhobin\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260730/6a6b0725101399263cde2e77.jpg\",\"mobile\":null,\"sex\":null,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-30 16:11:18');
INSERT INTO `sys_operate_log` VALUES (2378, 1, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 8, '2026-07-30 16:30:10', '{\"form\":{\"id\":null,\"nickname\":\"系统管理员\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260730/6a6b0b91101399263cde2e78.jpg\",\"mobile\":\"18772916901\",\"sex\":2,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-30 16:30:10');
INSERT INTO `sys_operate_log` VALUES (2379, 1, '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 142, '2026-07-30 16:53:29', '{\"form\":{\"id\":null,\"nickname\":\"系统管理员\",\"email\":null,\"password\":null,\"oldPassword\":null,\"newPassword\":null,\"code\":null,\"status\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260730/6a6b1106101346deb9f8d8b7.jpg\",\"mobile\":\"18772916901\",\"sex\":2,\"roleIds\":null}}', 'com.nexora.controller.system.SysUserController', 'updateProfile', '2026-07-30 16:53:29');

-- ----------------------------
-- Table structure for sys_oss_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss_file`;
CREATE TABLE `sys_oss_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件唯一ID',
  `file_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'OSS访问地址',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'OSS保存文件名',
  `original_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'MIME类型',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `platform` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储平台',
  `thumbnail_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '缩略图地址',
  `uploader_id` bigint NOT NULL COMMENT '上传人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_oss_file_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_sys_oss_file_url`(`file_url`(255) ASC) USING BTREE,
  INDEX `idx_sys_oss_file_original_name`(`original_filename` ASC) USING BTREE,
  INDEX `idx_sys_oss_file_content_type`(`content_type` ASC) USING BTREE,
  INDEX `idx_sys_oss_file_uploader`(`uploader_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'OSS文件流水表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_oss_file
-- ----------------------------
INSERT INTO `sys_oss_file` VALUES (9, '2082739780126294017', 'https://oss.wuhobin.top/base/20260730/6a6b0631101399263cde2e75.jpg', '6a6b0631101399263cde2e75.jpg', '微信图片_20260728142844_53285_21.jpg', 'image/jpeg', 154452, 'qiniu-kodo-1', NULL, 1, '2026-07-30 16:07:14', '2026-07-30 16:07:14');
INSERT INTO `sys_oss_file` VALUES (10, '2082740367886696450', 'https://oss.wuhobin.top/base/20260730/6a6b06bd101399263cde2e76.png', '6a6b06bd101399263cde2e76.png', '微信图片_20260730160723_55651_21.png', 'image/png', 38851, 'qiniu-kodo-1', NULL, 1, '2026-07-30 16:09:34', '2026-07-30 16:09:34');
INSERT INTO `sys_oss_file` VALUES (11, '2082740804446633985', 'https://oss.wuhobin.top/base/20260730/6a6b0725101399263cde2e77.jpg', '6a6b0725101399263cde2e77.jpg', '微信图片_20260730160939_14180_31.jpg', 'image/jpeg', 256923, 'qiniu-kodo-1', NULL, 1816, '2026-07-30 16:11:18', '2026-07-30 16:11:18');
INSERT INTO `sys_oss_file` VALUES (13, '2082751416417431553', 'https://oss.wuhobin.top/base/20260730/6a6b1106101346deb9f8d8b7.jpg', '6a6b1106101346deb9f8d8b7.jpg', '微信图片_20260730161709_55684_21.jpg', 'image/jpeg', 330984, 'qiniu-kodo-1', NULL, 1, '2026-07-30 16:53:28', '2026-07-30 16:53:28');
INSERT INTO `sys_oss_file` VALUES (14, '2082787138621001730', 'https://oss.wuhobin.top/base/20260730/6a6b324b10132a48a1712b16.mp4', '6a6b324b10132a48a1712b16.mp4', '03dbf31cd8e55f8e8e96c4ddb9ddd0e9.mp4', 'video/mp4', 861604, 'qiniu-kodo-1', NULL, 1, '2026-07-30 19:15:25', '2026-07-30 19:15:25');
INSERT INTO `sys_oss_file` VALUES (15, '2082787781716856833', 'https://oss.wuhobin.top/base/20260730/6a6b32e410132a48a1712b17.pdf', '6a6b32e410132a48a1712b17.pdf', '58种姿势+高清无打码系列(1)(1)(1).pdf', 'application/pdf', 5895518, 'qiniu-kodo-1', NULL, 1, '2026-07-30 19:17:58', '2026-07-30 19:17:58');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '角色编码',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '角色名称',
  `remarks` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'admin', '超级管理员', '拥有一切权限\n', '2024-11-16 12:29:00', '2024-11-16 12:29:00');
INSERT INTO `sys_role` VALUES (20, 'user', '普通', '', '2026-07-28 10:34:01', '2026-07-28 10:34:01');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` int NULL DEFAULT NULL COMMENT '角色ID',
  `menu_id` int NULL DEFAULT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `role_id`(`role_id` ASC, `menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 549 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色-权限资源关联表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (475, 1, 1);
INSERT INTO `sys_role_menu` VALUES (482, 1, 2);
INSERT INTO `sys_role_menu` VALUES (484, 1, 3);
INSERT INTO `sys_role_menu` VALUES (503, 1, 4);
INSERT INTO `sys_role_menu` VALUES (483, 1, 8);
INSERT INTO `sys_role_menu` VALUES (488, 1, 9);
INSERT INTO `sys_role_menu` VALUES (490, 1, 10);
INSERT INTO `sys_role_menu` VALUES (491, 1, 11);
INSERT INTO `sys_role_menu` VALUES (492, 1, 12);
INSERT INTO `sys_role_menu` VALUES (493, 1, 13);
INSERT INTO `sys_role_menu` VALUES (476, 1, 14);
INSERT INTO `sys_role_menu` VALUES (504, 1, 18);
INSERT INTO `sys_role_menu` VALUES (510, 1, 19);
INSERT INTO `sys_role_menu` VALUES (478, 1, 27);
INSERT INTO `sys_role_menu` VALUES (479, 1, 28);
INSERT INTO `sys_role_menu` VALUES (480, 1, 29);
INSERT INTO `sys_role_menu` VALUES (514, 1, 30);
INSERT INTO `sys_role_menu` VALUES (498, 1, 32);
INSERT INTO `sys_role_menu` VALUES (499, 1, 33);
INSERT INTO `sys_role_menu` VALUES (477, 1, 39);
INSERT INTO `sys_role_menu` VALUES (485, 1, 40);
INSERT INTO `sys_role_menu` VALUES (486, 1, 41);
INSERT INTO `sys_role_menu` VALUES (487, 1, 43);
INSERT INTO `sys_role_menu` VALUES (494, 1, 48);
INSERT INTO `sys_role_menu` VALUES (496, 1, 49);
INSERT INTO `sys_role_menu` VALUES (497, 1, 50);
INSERT INTO `sys_role_menu` VALUES (495, 1, 51);
INSERT INTO `sys_role_menu` VALUES (481, 1, 52);
INSERT INTO `sys_role_menu` VALUES (489, 1, 53);
INSERT INTO `sys_role_menu` VALUES (502, 1, 54);
INSERT INTO `sys_role_menu` VALUES (500, 1, 55);
INSERT INTO `sys_role_menu` VALUES (501, 1, 56);
INSERT INTO `sys_role_menu` VALUES (505, 1, 58);
INSERT INTO `sys_role_menu` VALUES (506, 1, 59);
INSERT INTO `sys_role_menu` VALUES (507, 1, 60);
INSERT INTO `sys_role_menu` VALUES (508, 1, 61);
INSERT INTO `sys_role_menu` VALUES (509, 1, 62);
INSERT INTO `sys_role_menu` VALUES (511, 1, 63);
INSERT INTO `sys_role_menu` VALUES (513, 1, 64);
INSERT INTO `sys_role_menu` VALUES (512, 1, 66);
INSERT INTO `sys_role_menu` VALUES (515, 1, 111);
INSERT INTO `sys_role_menu` VALUES (516, 1, 112);
INSERT INTO `sys_role_menu` VALUES (518, 1, 113);
INSERT INTO `sys_role_menu` VALUES (519, 1, 114);
INSERT INTO `sys_role_menu` VALUES (520, 1, 115);
INSERT INTO `sys_role_menu` VALUES (517, 1, 116);
INSERT INTO `sys_role_menu` VALUES (527, 1, 130);
INSERT INTO `sys_role_menu` VALUES (528, 1, 131);
INSERT INTO `sys_role_menu` VALUES (529, 1, 132);
INSERT INTO `sys_role_menu` VALUES (530, 1, 133);
INSERT INTO `sys_role_menu` VALUES (531, 1, 134);
INSERT INTO `sys_role_menu` VALUES (370, 14, 1);
INSERT INTO `sys_role_menu` VALUES (373, 14, 2);
INSERT INTO `sys_role_menu` VALUES (374, 14, 3);
INSERT INTO `sys_role_menu` VALUES (383, 14, 4);
INSERT INTO `sys_role_menu` VALUES (375, 14, 9);
INSERT INTO `sys_role_menu` VALUES (380, 14, 13);
INSERT INTO `sys_role_menu` VALUES (371, 14, 14);
INSERT INTO `sys_role_menu` VALUES (384, 14, 16);
INSERT INTO `sys_role_menu` VALUES (385, 14, 17);
INSERT INTO `sys_role_menu` VALUES (387, 14, 18);
INSERT INTO `sys_role_menu` VALUES (389, 14, 19);
INSERT INTO `sys_role_menu` VALUES (391, 14, 30);
INSERT INTO `sys_role_menu` VALUES (392, 14, 31);
INSERT INTO `sys_role_menu` VALUES (377, 14, 32);
INSERT INTO `sys_role_menu` VALUES (378, 14, 33);
INSERT INTO `sys_role_menu` VALUES (393, 14, 34);
INSERT INTO `sys_role_menu` VALUES (372, 14, 39);
INSERT INTO `sys_role_menu` VALUES (381, 14, 51);
INSERT INTO `sys_role_menu` VALUES (376, 14, 53);
INSERT INTO `sys_role_menu` VALUES (382, 14, 54);
INSERT INTO `sys_role_menu` VALUES (379, 14, 55);
INSERT INTO `sys_role_menu` VALUES (388, 14, 58);
INSERT INTO `sys_role_menu` VALUES (390, 14, 66);
INSERT INTO `sys_role_menu` VALUES (353, 14, 68);
INSERT INTO `sys_role_menu` VALUES (358, 14, 69);
INSERT INTO `sys_role_menu` VALUES (356, 14, 70);
INSERT INTO `sys_role_menu` VALUES (354, 14, 71);
INSERT INTO `sys_role_menu` VALUES (360, 14, 72);
INSERT INTO `sys_role_menu` VALUES (363, 14, 73);
INSERT INTO `sys_role_menu` VALUES (361, 14, 74);
INSERT INTO `sys_role_menu` VALUES (355, 14, 75);
INSERT INTO `sys_role_menu` VALUES (359, 14, 80);
INSERT INTO `sys_role_menu` VALUES (357, 14, 84);
INSERT INTO `sys_role_menu` VALUES (362, 14, 88);
INSERT INTO `sys_role_menu` VALUES (364, 14, 90);
INSERT INTO `sys_role_menu` VALUES (386, 14, 95);
INSERT INTO `sys_role_menu` VALUES (365, 14, 96);
INSERT INTO `sys_role_menu` VALUES (366, 14, 97);
INSERT INTO `sys_role_menu` VALUES (367, 14, 98);
INSERT INTO `sys_role_menu` VALUES (368, 14, 100);
INSERT INTO `sys_role_menu` VALUES (369, 14, 101);
INSERT INTO `sys_role_menu` VALUES (543, 20, 111);
INSERT INTO `sys_role_menu` VALUES (544, 20, 112);
INSERT INTO `sys_role_menu` VALUES (548, 20, 113);
INSERT INTO `sys_role_menu` VALUES (547, 20, 114);
INSERT INTO `sys_role_menu` VALUES (546, 20, 115);
INSERT INTO `sys_role_menu` VALUES (545, 20, 116);
INSERT INTO `sys_role_menu` VALUES (532, 20, 117);
INSERT INTO `sys_role_menu` VALUES (533, 20, 118);
INSERT INTO `sys_role_menu` VALUES (538, 20, 119);
INSERT INTO `sys_role_menu` VALUES (539, 20, 120);
INSERT INTO `sys_role_menu` VALUES (540, 20, 121);
INSERT INTO `sys_role_menu` VALUES (541, 20, 122);
INSERT INTO `sys_role_menu` VALUES (542, 20, 123);
INSERT INTO `sys_role_menu` VALUES (534, 20, 124);
INSERT INTO `sys_role_menu` VALUES (535, 20, 125);
INSERT INTO `sys_role_menu` VALUES (536, 20, 126);
INSERT INTO `sys_role_menu` VALUES (537, 20, 127);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `password` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录密码',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` int NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  `ip` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'ip地址',
  `ip_location` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'ip来源',
  `os` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录系统',
  `last_login_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
  `browser` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '浏览器',
  `nickname` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '头像',
  `mobile` varchar(15) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '登录邮箱',
  `sex` int NULL DEFAULT NULL COMMENT '性别',
  `login_type` int NULL DEFAULT NULL COMMENT '登录方式',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1817 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, '$2a$10$Olf5IWFsSL5uw6lQQxCCX.Yx9gFvdIbiyXrXPmCm9N9OVesHYMBQy', '2024-12-27 14:16:17', '2026-07-30 16:53:28', 1, '127.0.0.1', '内网IP|内网IP', 'Windows', '2026-07-03 14:17:04', 'Chrome', '系统管理员', 'https://oss.wuhobin.top/base/20260730/6a6b1106101346deb9f8d8b7.jpg', '18772916901', 'wuhongbinyos@163.com', 2, NULL);
INSERT INTO `sys_user` VALUES (1816, '$2a$10$w/0p1TOFjL5jwxV57aPIsuhurq6A28I4nMZMKQ/IKlWzNoJ3eSJxi', '2026-07-30 13:58:01', '2026-07-30 16:11:18', 1, NULL, NULL, NULL, '2026-07-30 13:58:01', NULL, 'wuhobin', 'https://oss.wuhobin.top/base/20260730/6a6b0725101399263cde2e77.jpg', NULL, 'wuhobin@126.com', NULL, NULL);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` int NULL DEFAULT NULL COMMENT '角色ID',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 63 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '系统管理 - 用户角色关联表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (56, 1, 1);
INSERT INTO `sys_user_role` VALUES (61, 20, 1815);
INSERT INTO `sys_user_role` VALUES (62, 20, 1816);

SET FOREIGN_KEY_CHECKS = 1;
