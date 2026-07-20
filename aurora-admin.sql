/*
 Navicat Premium Data Transfer

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80036
 Source Host           : 127.0.0.1:3306
 Source Schema         : easyadmin

 Target Server Type    : MySQL
 Target Server Version : 80036
 File Encoding         : 65001

 Date: 19/07/2026 23:12:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for mail_account
-- ----------------------------
DROP TABLE IF EXISTS `mail_account`;
CREATE TABLE `mail_account`  (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
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
                                 UNIQUE INDEX `uk_mail_account_email`(`email` ASC) USING BTREE,
                                 INDEX `idx_mail_account_enabled_sort`(`enabled` ASC, `sort` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '聚合邮箱账户' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of mail_account
-- ----------------------------
INSERT INTO `mail_account` VALUES (1, 'QQ邮箱', 'QQ', '1289066006@qq.com', 'v1:2jdaO+DSabB/rHuv:YDskUtAw10RUebiHvDOO8SEOIeoYacs47qcdPRFayfI=', 1, 0, NULL, NULL, '2026-07-19 23:11:18', '', '2026-07-19 21:23:12', '2026-07-19 23:11:18');
INSERT INTO `mail_account` VALUES (2, '网易邮箱', 'NETEASE_163', 'wuhongbinyos@163.com', 'v1:PraA3Styz5bgaZlp:ep5JyA4rcv3kDWYmOpVbrVwfjkRx9F68dB70h5B0NpY=', 1, 1, NULL, NULL, '2026-07-19 23:11:19', '', '2026-07-19 21:54:58', '2026-07-19 23:11:19');

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quartz_job
-- ----------------------------
INSERT INTO `quartz_job` VALUES (1, '测试任务', 'DEFAULT', '* * * * * ? *', 'task.neatNoParams()', '1', '1', '1');
INSERT INTO `quartz_job` VALUES (2, '11', 'DEFAULT', 'null', '11', '1', '1', '1');

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
) ENGINE = InnoDB AUTO_INCREMENT = 54 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务执行日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quartz_job_log
-- ----------------------------
INSERT INTO `quartz_job_log` VALUES (1, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (2, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (3, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 4, '测试任务 总共耗时：4毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (4, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (5, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 3, '测试任务 总共耗时：3毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (6, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 2, '测试任务 总共耗时：2毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (7, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (8, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (9, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (10, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (11, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (12, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (13, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (14, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (15, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (16, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:30', '2026-07-03 15:07:30', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (17, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:31', '2026-07-03 15:07:31', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (18, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:32', '2026-07-03 15:07:32', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (19, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:33', '2026-07-03 15:07:33', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (20, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:34', '2026-07-03 15:07:34', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (21, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:35', '2026-07-03 15:07:35', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (22, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:36', '2026-07-03 15:07:36', 1, '测试任务 总共耗时：1毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (23, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:37', '2026-07-03 15:07:37', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (24, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:38', '2026-07-03 15:07:38', 1, '测试任务 总共耗时：1毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (25, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:07:39', '2026-07-03 15:07:39', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (26, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:10:35', '2026-07-03 15:10:35', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (27, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:10:47', '2026-07-03 15:10:47', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (28, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (29, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (30, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (31, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (32, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (33, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (34, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (35, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (36, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:07', '2026-07-03 15:11:07', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (37, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:08', '2026-07-03 15:11:08', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (38, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:09', '2026-07-03 15:11:09', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (39, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:10', '2026-07-03 15:11:10', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (40, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:11', '2026-07-03 15:11:11', 0, '测试任务1111 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (41, 1, '测试任务1111', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:12', '2026-07-03 15:11:12', 1, '测试任务1111 总共耗时：1毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (42, 1, '测试任务1111232', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:13', '2026-07-03 15:11:13', 0, '测试任务1111232 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (43, 1, '测试任务1111232', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:13', '2026-07-03 15:11:13', 0, '测试任务1111232 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (44, 1, '测试任务1111232', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:14', '2026-07-03 15:11:14', 0, '测试任务1111232 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (45, 1, '测试任务1111232', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:15', '2026-07-03 15:11:15', 0, '测试任务1111232 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (46, 1, '测试任务1111232', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:16', '2026-07-03 15:11:16', 0, '测试任务1111232 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (47, 1, '测试任务1111232', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:17', '2026-07-03 15:11:17', 0, '测试任务1111232 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (48, 1, '测试任务1111232', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:18', '2026-07-03 15:11:18', 0, '测试任务1111232 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (49, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:19', '2026-07-03 15:11:19', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (50, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:19', '2026-07-03 15:11:19', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (51, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:20', '2026-07-03 15:11:20', 0, '测试任务 总共耗时：0毫秒', '0', NULL);
INSERT INTO `quartz_job_log` VALUES (52, 1, '测试任务', 'DEFAULT', 'task.neatNoParams()', '2026-07-03 15:11:21', '2026-07-03 15:11:21', 0, '测试任务 总共耗时：0毫秒', '0', NULL);

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
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (1, '文件类型', 'file_content_type', 1, '文件MIME类型分类', '2026-07-17 11:50:39', '2026-07-17 11:50:39', 1);

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
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 128 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '权限资源表 ' ROW_FORMAT = DYNAMIC;

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
INSERT INTO `sys_menu` VALUES (30, '0', '/tool', 'Layout', '系统工具', 6, 'Suitcase', 'CATALOG', '2024-11-18 09:21:10', '2026-07-08 16:03:04', '/tool/gen', '', 1, '', 0);
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

-- ----------------------------
-- Table structure for sys_operate_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operate_log`;
CREATE TABLE `sys_operate_log`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作用户',
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
) ENGINE = InnoDB AUTO_INCREMENT = 2351 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_operate_log
-- ----------------------------
INSERT INTO `sys_operate_log` VALUES (2310, NULL, '/sys/menu/34', 'DELETE', '删除菜单', '127.0.0.1', '内网IP|内网IP', 169, '2026-07-06 10:57:27', '{\"id\":34}', 'com.aurora.controller.system.SysMenuController', 'deleteMenu', '2026-07-06 14:42:33');
INSERT INTO `sys_operate_log` VALUES (2311, 'admin', '/sys/user', 'PUT', '修改用户', '127.0.0.1', '内网IP|内网IP', 39, '2026-07-06 13:16:35', '{\"user\":{\"user\":{\"id\":1,\"username\":\"admin\",\"status\":1,\"ip\":\"127.0.0.1\",\"ipLocation\":\"内网IP|内网IP\",\"nickname\":\"系统管理员1\",\"avatar\":\"https://oss.wuhobin.top/base/20260706/6a4b30fe55547cb3b50897a7.png\",\"mobile\":\"17608485482\",\"email\":\"1289066006@qq.com\",\"sex\":1,\"lastLoginTime\":1783059424000,\"createTime\":1735280177000,\"updateTime\":1783314995248},\"roleIds\":[1]}}', 'com.aurora.controller.system.SysUserController', 'update', '2026-07-06 14:42:33');
INSERT INTO `sys_operate_log` VALUES (2312, 'admin', '/sys/user', 'PUT', '修改用户', '127.0.0.1', '内网IP|内网IP', 12, '2026-07-06 13:17:11', '{\"user\":{\"user\":{\"id\":1,\"username\":\"admin\",\"status\":1,\"ip\":\"127.0.0.1\",\"ipLocation\":\"内网IP|内网IP\",\"nickname\":\"系统管理员\",\"avatar\":\"https://oss.wuhobin.top/base/20260706/6a4b30fe55547cb3b50897a7.png\",\"mobile\":\"17608485482\",\"email\":\"1289066006@qq.com\",\"sex\":1,\"lastLoginTime\":1783059424000,\"createTime\":1735280177000,\"updateTime\":1783315031982},\"roleIds\":[1]}}', 'com.aurora.controller.system.SysUserController', 'update', '2026-07-06 14:42:33');
INSERT INTO `sys_operate_log` VALUES (2313, 'admin', '/sys/user', 'PUT', '修改用户', '127.0.0.1', '内网IP|内网IP', 20, '2026-07-06 14:42:55', '{\"user\":{\"user\":{\"id\":1,\"username\":\"admin\",\"status\":1,\"ip\":\"127.0.0.1\",\"ipLocation\":\"内网IP|内网IP\",\"nickname\":\"系统管理员1\",\"avatar\":\"https://oss.wuhobin.top/base/20260706/6a4b30fe55547cb3b50897a7.png\",\"mobile\":\"17608485482\",\"email\":\"1289066006@qq.com\",\"sex\":1,\"lastLoginTime\":1783059424000,\"createTime\":1735280177000,\"updateTime\":1783320174697},\"roleIds\":[1]}}', 'com.aurora.controller.system.SysUserController', 'update', '2026-07-06 14:42:55');
INSERT INTO `sys_operate_log` VALUES (2314, 'admin', '/sys/user', 'PUT', '修改用户', '127.0.0.1', '内网IP|内网IP', 13, '2026-07-06 14:43:01', '{\"user\":{\"user\":{\"id\":1,\"username\":\"admin\",\"status\":1,\"ip\":\"127.0.0.1\",\"ipLocation\":\"内网IP|内网IP\",\"nickname\":\"系统管理员\",\"avatar\":\"https://oss.wuhobin.top/base/20260706/6a4b30fe55547cb3b50897a7.png\",\"mobile\":\"17608485482\",\"email\":\"1289066006@qq.com\",\"sex\":1,\"lastLoginTime\":1783059424000,\"createTime\":1735280177000,\"updateTime\":1783320180781},\"roleIds\":[1]}}', 'com.aurora.controller.system.SysUserController', 'update', '2026-07-06 14:43:01');
INSERT INTO `sys_operate_log` VALUES (2315, 'admin', '/sys/menu', 'PUT', '修改菜单', '127.0.0.1', '内网IP|内网IP', 199, '2026-07-06 15:57:57', '{\"menu\":{\"id\":106,\"parentId\":0,\"path\":\"/file\",\"component\":\"Layout\",\"title\":\"文件管理\",\"sort\":10,\"icon\":\"\",\"type\":\"CATALOG\",\"redirect\":\"\",\"name\":\"\",\"hidden\":0,\"isExternal\":0,\"perm\":\"\",\"children\":[{\"id\":107,\"parentId\":106,\"path\":\"\",\"component\":\"\",\"title\":\"上传文件\",\"sort\":1,\"icon\":\"\",\"type\":\"BUTTON\",\"redirect\":\"\",\"name\":\"\",\"hidden\":1,\"isExternal\":0,\"perm\":\"sys:file:upload\",\"createTime\":1735963705000},{\"id\":108,\"parentId\":106,\"path\":\"\",\"component\":\"\",\"title\":\"删除文件\",\"sort\":2,\"icon\":\"\",\"type\":\"BUTTON\",\"redirect\":\"\",\"name\":\"\",\"hidden\":1,\"isExternal\":0,\"perm\":\"sys:file:delete\",\"createTime\":1735963716000}],\"createTime\":1735963683000,\"updateTime\":1783324676803}}', 'com.aurora.controller.system.SysMenuController', 'updateMenu', '2026-07-06 15:57:57');
INSERT INTO `sys_operate_log` VALUES (2316, 'admin', '/sys/menu', 'PUT', '修改菜单', '127.0.0.1', '内网IP|内网IP', 61, '2026-07-06 15:58:57', '{\"menu\":{\"id\":107,\"parentId\":106,\"path\":\"\",\"component\":\"\",\"title\":\"上传文件\",\"sort\":1,\"icon\":\"\",\"type\":\"BUTTON\",\"redirect\":\"\",\"name\":\"\",\"hidden\":0,\"isExternal\":0,\"perm\":\"sys:file:upload\",\"createTime\":1735963705000,\"updateTime\":1783324736716}}', 'com.aurora.controller.system.SysMenuController', 'updateMenu', '2026-07-06 15:58:57');
INSERT INTO `sys_operate_log` VALUES (2317, 'admin', '/sys/menu', 'PUT', '修改菜单', '127.0.0.1', '内网IP|内网IP', 30, '2026-07-06 15:59:00', '{\"menu\":{\"id\":108,\"parentId\":106,\"path\":\"\",\"component\":\"\",\"title\":\"删除文件\",\"sort\":2,\"icon\":\"\",\"type\":\"BUTTON\",\"redirect\":\"\",\"name\":\"\",\"hidden\":0,\"isExternal\":0,\"perm\":\"sys:file:delete\",\"createTime\":1735963716000,\"updateTime\":1783324739849}}', 'com.aurora.controller.system.SysMenuController', 'updateMenu', '2026-07-06 15:59:00');
INSERT INTO `sys_operate_log` VALUES (2318, 'admin', '/sys/menu/107', 'DELETE', '删除菜单', '127.0.0.1', '内网IP|内网IP', 72, '2026-07-06 15:59:40', '{\"id\":107}', 'com.aurora.controller.system.SysMenuController', 'deleteMenu', '2026-07-06 15:59:40');
INSERT INTO `sys_operate_log` VALUES (2319, 'admin', '/sys/menu/108', 'DELETE', '删除菜单', '127.0.0.1', '内网IP|内网IP', 19, '2026-07-06 15:59:43', '{\"id\":108}', 'com.aurora.controller.system.SysMenuController', 'deleteMenu', '2026-07-06 15:59:43');
INSERT INTO `sys_operate_log` VALUES (2320, 'admin', '/sys/menu/106', 'DELETE', '删除菜单', '127.0.0.1', '内网IP|内网IP', 27, '2026-07-06 15:59:45', '{\"id\":106}', 'com.aurora.controller.system.SysMenuController', 'deleteMenu', '2026-07-06 15:59:45');
INSERT INTO `sys_operate_log` VALUES (2321, 'admin', '/sys/menu/31', 'DELETE', '删除菜单', '127.0.0.1', '内网IP|内网IP', 179, '2026-07-08 16:02:26', '{\"id\":31}', 'com.aurora.controller.system.SysMenuController', 'deleteMenu', '2026-07-08 16:02:26');
INSERT INTO `sys_operate_log` VALUES (2322, 'admin', '/sys/menu', 'PUT', '修改菜单', '127.0.0.1', '内网IP|内网IP', 113, '2026-07-08 16:03:05', '{\"menu\":{\"id\":30,\"parentId\":0,\"path\":\"/tool\",\"component\":\"Layout\",\"title\":\"系统工具\",\"sort\":6,\"icon\":\"Suitcase\",\"type\":\"CATALOG\",\"redirect\":\"/tool/gen\",\"name\":\"\",\"hidden\":1,\"isExternal\":0,\"perm\":\"\",\"createTime\":1731892870000,\"updateTime\":1783497784396}}', 'com.aurora.controller.system.SysMenuController', 'updateMenu', '2026-07-08 16:03:05');
INSERT INTO `sys_operate_log` VALUES (2323, 'admin', '/sys/menu', 'POST', '添加菜单', '127.0.0.1', '内网IP|内网IP', 125, '2026-07-16 20:03:49', '{\"menu\":{\"createTime\":\"2026-07-16 20:03:48\",\"updateTime\":\"2026-07-16 20:03:48\",\"params\":null,\"id\":110,\"parentId\":0,\"path\":\"/file\",\"component\":\"\",\"title\":\"文件管理\",\"sort\":1,\"icon\":\"Files\",\"type\":\"MENU\",\"redirect\":\"\",\"name\":\"\",\"hidden\":0,\"isExternal\":0,\"perm\":\"\",\"children\":null}}', 'com.aurora.controller.system.SysMenuController', 'addMenu', '2026-07-16 20:03:49');
INSERT INTO `sys_operate_log` VALUES (2324, 'admin', '/sys/menu/110', 'DELETE', '删除菜单', '127.0.0.1', '内网IP|内网IP', 60, '2026-07-16 20:03:54', '{\"id\":110}', 'com.aurora.controller.system.SysMenuController', 'deleteMenu', '2026-07-16 20:03:54');
INSERT INTO `sys_operate_log` VALUES (2325, 'admin', '/sys/menu', 'PUT', '修改菜单', '127.0.0.1', '内网IP|内网IP', 25, '2026-07-16 20:06:14', '{\"menu\":{\"createTime\":\"2026-07-16 19:59:19\",\"updateTime\":\"2026-07-16 20:06:13\",\"params\":null,\"id\":109,\"parentId\":0,\"path\":\"file\",\"component\":\"/file/index\",\"title\":\"文件管理\",\"sort\":1,\"icon\":\"Files\",\"type\":\"MENU\",\"redirect\":\"\",\"name\":\"\",\"hidden\":0,\"isExternal\":0,\"perm\":\"\",\"children\":null}}', 'com.aurora.controller.system.SysMenuController', 'updateMenu', '2026-07-16 20:06:14');
INSERT INTO `sys_operate_log` VALUES (2326, 'admin', '/sys/menu', 'POST', '添加菜单', '127.0.0.1', '内网IP|内网IP', 87, '2026-07-17 11:03:54', '{\"menu\":{\"createTime\":\"2026-07-17 11:03:53\",\"updateTime\":\"2026-07-17 11:03:53\",\"params\":null,\"id\":114,\"parentId\":112,\"path\":\"\",\"component\":\"\",\"title\":\"上传文件\",\"sort\":1,\"icon\":\"\",\"type\":\"BUTTON\",\"redirect\":\"\",\"name\":\"\",\"hidden\":1,\"isExternal\":0,\"perm\":\"sys:file:upload\",\"children\":null}}', 'com.aurora.controller.system.SysMenuController', 'addMenu', '2026-07-17 11:03:54');
INSERT INTO `sys_operate_log` VALUES (2327, 'admin', '/sys/menu', 'POST', '添加菜单', '127.0.0.1', '内网IP|内网IP', 9, '2026-07-17 11:04:36', '{\"menu\":{\"createTime\":\"2026-07-17 11:04:35\",\"updateTime\":\"2026-07-17 11:04:35\",\"params\":null,\"id\":115,\"parentId\":112,\"path\":\"\",\"component\":\"\",\"title\":\"删除文件\",\"sort\":1,\"icon\":\"\",\"type\":\"BUTTON\",\"redirect\":\"\",\"name\":\"\",\"hidden\":1,\"isExternal\":0,\"perm\":\"sys:file:delete\",\"children\":null}}', 'com.aurora.controller.system.SysMenuController', 'addMenu', '2026-07-17 11:04:36');
INSERT INTO `sys_operate_log` VALUES (2328, 'admin', '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 29, '2026-07-17 11:04:50', '{\"user\":{\"createTime\":null,\"updateTime\":\"2026-07-17 11:04:50\",\"params\":null,\"id\":1,\"username\":null,\"password\":null,\"status\":null,\"ip\":null,\"ipLocation\":null,\"os\":null,\"browser\":null,\"nickname\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260717/6a599bd05554dc0c71e4db80.jpg\",\"mobile\":null,\"email\":null,\"sex\":null,\"loginType\":null,\"lastLoginTime\":null}}', 'com.aurora.controller.system.SysUserController', 'updateProfile', '2026-07-17 11:04:50');
INSERT INTO `sys_operate_log` VALUES (2329, 'admin', '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 90, '2026-07-17 11:29:07', '{\"user\":{\"createTime\":null,\"updateTime\":\"2026-07-17 11:29:07\",\"params\":null,\"id\":1,\"username\":null,\"password\":null,\"status\":null,\"ip\":null,\"ipLocation\":null,\"os\":null,\"browser\":null,\"nickname\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260717/6a59a182555419c251de00f2.jpg\",\"mobile\":null,\"email\":null,\"sex\":null,\"loginType\":null,\"lastLoginTime\":null}}', 'com.aurora.controller.system.SysUserController', 'updateProfile', '2026-07-17 11:29:07');
INSERT INTO `sys_operate_log` VALUES (2330, 'admin', '/sys/dict/add', 'POST', '添加字典', '127.0.0.1', '内网IP|内网IP', 12, '2026-07-17 11:47:22', '{\"dict\":{\"createTime\":\"2026-07-17 11:47:22\",\"updateTime\":\"2026-07-17 11:47:22\",\"params\":null,\"id\":31,\"name\":\"file_type\",\"type\":\"1\",\"status\":1,\"remark\":\"\",\"sort\":null}}', 'com.aurora.controller.system.SysDictController', 'addDict', '2026-07-17 11:47:22');
INSERT INTO `sys_operate_log` VALUES (2331, 'admin', '/sys/dict/add', 'POST', '添加字典', '127.0.0.1', '内网IP|内网IP', 14, '2026-07-17 11:47:42', '{\"dict\":{\"createTime\":\"2026-07-17 11:47:42\",\"updateTime\":\"2026-07-17 11:47:42\",\"params\":null,\"id\":32,\"name\":\"11\",\"type\":\"11\",\"status\":1,\"remark\":\"\",\"sort\":null}}', 'com.aurora.controller.system.SysDictController', 'addDict', '2026-07-17 11:47:42');
INSERT INTO `sys_operate_log` VALUES (2332, 'admin', '/sys/dict/add', 'POST', '添加字典', '127.0.0.1', '内网IP|内网IP', 63, '2026-07-17 11:47:49', '{\"dict\":{\"createTime\":\"2026-07-17 11:47:48\",\"updateTime\":\"2026-07-17 11:47:48\",\"params\":null,\"id\":33,\"name\":\"11\",\"type\":\"打\",\"status\":1,\"remark\":\"\",\"sort\":null}}', 'com.aurora.controller.system.SysDictController', 'addDict', '2026-07-17 11:47:49');
INSERT INTO `sys_operate_log` VALUES (2333, 'admin', '/sys/dict/delete/31', 'DELETE', '删除字典', '127.0.0.1', '内网IP|内网IP', 61, '2026-07-17 11:47:55', '{\"ids\":[31]}', 'com.aurora.controller.system.SysDictController', 'delete', '2026-07-17 11:47:55');
INSERT INTO `sys_operate_log` VALUES (2334, 'admin', '/sys/dict/delete/32', 'DELETE', '删除字典', '127.0.0.1', '内网IP|内网IP', 10, '2026-07-17 11:47:59', '{\"ids\":[32]}', 'com.aurora.controller.system.SysDictController', 'delete', '2026-07-17 11:47:59');
INSERT INTO `sys_operate_log` VALUES (2335, 'admin', '/sys/dictData/update', 'PUT', '修改字典数据', '127.0.0.1', '内网IP|内网IP', 15, '2026-07-17 11:57:52', '{\"dictData\":{\"id\":38,\"dictId\":1,\"label\":\"图片\",\"value\":\"image/jpeg\",\"style\":\"success\",\"isDefault\":null,\"sort\":1,\"remark\":\"image/jpeg\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'updateDictData', '2026-07-17 11:57:52');
INSERT INTO `sys_operate_log` VALUES (2336, 'admin', '/sys/dictData/add', 'POST', '新增字典数据', '127.0.0.1', '内网IP|内网IP', 7, '2026-07-17 11:58:17', '{\"dictData\":{\"id\":44,\"dictId\":1,\"label\":\"image/png\",\"value\":\"image/png\",\"style\":\"success\",\"isDefault\":null,\"sort\":0,\"remark\":\"image/png\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'addDictData', '2026-07-17 11:58:17');
INSERT INTO `sys_operate_log` VALUES (2337, 'admin', '/sys/dictData/update', 'PUT', '修改字典数据', '127.0.0.1', '内网IP|内网IP', 10, '2026-07-17 11:58:36', '{\"dictData\":{\"id\":38,\"dictId\":1,\"label\":\"image/jpeg\",\"value\":\"image/jpeg\",\"style\":\"success\",\"isDefault\":null,\"sort\":1,\"remark\":\"image/jpeg\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'updateDictData', '2026-07-17 11:58:36');
INSERT INTO `sys_operate_log` VALUES (2338, 'admin', '/sys/dictData/update', 'PUT', '修改字典数据', '127.0.0.1', '内网IP|内网IP', 43, '2026-07-17 11:58:52', '{\"dictData\":{\"id\":39,\"dictId\":1,\"label\":\"image/gif\",\"value\":\"image/gif\",\"style\":\"danger\",\"isDefault\":null,\"sort\":2,\"remark\":\"image/gif\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'updateDictData', '2026-07-17 11:58:52');
INSERT INTO `sys_operate_log` VALUES (2339, 'admin', '/sys/dictData/update', 'PUT', '修改字典数据', '127.0.0.1', '内网IP|内网IP', 23, '2026-07-17 11:59:13', '{\"dictData\":{\"id\":40,\"dictId\":1,\"label\":\"image/webp\",\"value\":\"image/webp\",\"style\":\"warning\",\"isDefault\":null,\"sort\":3,\"remark\":\"image/webp\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'updateDictData', '2026-07-17 11:59:13');
INSERT INTO `sys_operate_log` VALUES (2340, 'admin', '/sys/dictData/update', 'PUT', '修改字典数据', '127.0.0.1', '内网IP|内网IP', 8, '2026-07-17 11:59:37', '{\"dictData\":{\"id\":41,\"dictId\":1,\"label\":\"video/mp4\",\"value\":\"video/mp4\",\"style\":\"primary\",\"isDefault\":null,\"sort\":4,\"remark\":\"video/mp4\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'updateDictData', '2026-07-17 11:59:37');
INSERT INTO `sys_operate_log` VALUES (2341, 'admin', '/sys/dictData/update', 'PUT', '修改字典数据', '127.0.0.1', '内网IP|内网IP', 48, '2026-07-17 11:59:57', '{\"dictData\":{\"id\":42,\"dictId\":1,\"label\":\"application/pdf\",\"value\":\"application/pdf\",\"style\":\"info\",\"isDefault\":null,\"sort\":5,\"remark\":\"application/pdf\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'updateDictData', '2026-07-17 11:59:57');
INSERT INTO `sys_operate_log` VALUES (2342, 'admin', '/sys/dictData/update', 'PUT', '修改字典数据', '127.0.0.1', '内网IP|内网IP', 9, '2026-07-17 12:00:23', '{\"dictData\":{\"id\":43,\"dictId\":1,\"label\":\"application/zip\",\"value\":\"application/zip\",\"style\":\"info\",\"isDefault\":null,\"sort\":6,\"remark\":\"application/zip\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'updateDictData', '2026-07-17 12:00:23');
INSERT INTO `sys_operate_log` VALUES (2343, 'admin', '/sys/dictData/add', 'POST', '新增字典数据', '127.0.0.1', '内网IP|内网IP', 48, '2026-07-17 12:00:48', '{\"dictData\":{\"id\":45,\"dictId\":1,\"label\":\"text/plain\",\"value\":\"text/plain\",\"style\":\"info\",\"isDefault\":null,\"sort\":8,\"remark\":\"text/plain\",\"status\":1}}', 'com.aurora.controller.system.SysDictDataController', 'addDictData', '2026-07-17 12:00:48');
INSERT INTO `sys_operate_log` VALUES (2344, 'admin', '/sys/menu', 'POST', '添加菜单', '127.0.0.1', '内网IP|内网IP', 129, '2026-07-17 12:31:48', '{\"menu\":{\"createTime\":\"2026-07-17 12:31:48\",\"updateTime\":\"2026-07-17 12:31:48\",\"params\":null,\"id\":116,\"parentId\":112,\"path\":\"\",\"component\":\"\",\"title\":\"下载文件\",\"sort\":1,\"icon\":\"\",\"type\":\"BUTTON\",\"redirect\":\"\",\"name\":\"\",\"hidden\":1,\"isExternal\":0,\"perm\":\"sys:file:download\",\"children\":null}}', 'com.aurora.controller.system.SysMenuController', 'addMenu', '2026-07-17 12:31:48');
INSERT INTO `sys_operate_log` VALUES (2345, 'admin', '/sys/role/menus/1', 'PUT', '修改角色权限', '127.0.0.1', '内网IP|内网IP', 86, '2026-07-17 21:09:51', '{\"id\":1,\"menuIds\":[4,18,58,59,60,61,62,19,63,66,64,111,112,116,113,114,115]}', 'com.aurora.controller.system.SysRoleController', 'updateRoleMenus', '2026-07-17 21:09:51');
INSERT INTO `sys_operate_log` VALUES (2346, 'admin', '/sys/role/menus/1', 'PUT', '修改角色权限', '127.0.0.1', '内网IP|内网IP', 30, '2026-07-17 21:26:27', '{\"id\":1,\"menuIds\":[1,14,39,27,28,29,52,2,8,3,40,41,43,9,53,10,11,12,13,48,51,49,50,32,33,55,56,54,4,18,58,59,60,61,62,19,63,66,64,30,111,112,116,113,114,115]}', 'com.aurora.controller.system.SysRoleController', 'updateRoleMenus', '2026-07-17 21:26:27');
INSERT INTO `sys_operate_log` VALUES (2347, 'admin', '/sys/role/menus/2', 'PUT', '修改角色权限', '127.0.0.1', '内网IP|内网IP', 19, '2026-07-17 21:27:42', '{\"id\":2,\"menuIds\":[111,112,116,113,114,115]}', 'com.aurora.controller.system.SysRoleController', 'updateRoleMenus', '2026-07-17 21:27:42');
INSERT INTO `sys_operate_log` VALUES (2348, 'user', '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 151, '2026-07-19 17:08:08', '{\"form\":{\"nickname\":\"普通用户1\",\"avatar\":null,\"mobile\":\"18772916901\",\"email\":\"1289066006@qq.com\",\"sex\":1}}', 'com.aurora.controller.system.SysUserController', 'updateProfile', '2026-07-19 17:08:08');
INSERT INTO `sys_operate_log` VALUES (2349, 'user', '/sys/user/updProfile', 'PUT', '修改个人信息', '127.0.0.1', '内网IP|内网IP', 13, '2026-07-19 17:08:15', '{\"form\":{\"nickname\":null,\"avatar\":\"https://oss.wuhobin.top/base/20260719/6a5c93fd80e6bd74478afdc1.jpg\",\"mobile\":null,\"email\":null,\"sex\":null}}', 'com.aurora.controller.system.SysUserController', 'updateProfile', '2026-07-19 17:08:15');
INSERT INTO `sys_operate_log` VALUES (2350, 'admin', '/sys/menu/52', 'DELETE', '删除菜单', '127.0.0.1', '内网IP|内网IP', 21, '2026-07-19 17:09:05', '{\"id\":52}', 'com.aurora.controller.system.SysMenuController', 'deleteMenu', '2026-07-19 17:09:05');

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
                                 `uploader_id` bigint NULL DEFAULT NULL COMMENT '上传人ID',
                                 `uploader_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上传人用户名',
                                 `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                 `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 UNIQUE INDEX `uk_sys_oss_file_file_id`(`file_id` ASC) USING BTREE,
                                 INDEX `idx_sys_oss_file_url`(`file_url`(255) ASC) USING BTREE,
                                 INDEX `idx_sys_oss_file_original_name`(`original_filename` ASC) USING BTREE,
                                 INDEX `idx_sys_oss_file_content_type`(`content_type` ASC) USING BTREE,
                                 INDEX `idx_sys_oss_file_uploader`(`uploader_id` ASC, `uploader_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'OSS文件流水表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_oss_file
-- ----------------------------
INSERT INTO `sys_oss_file` VALUES (2, '2077958748180103170', 'https://oss.wuhobin.top/base/20260717/6a59a182555419c251de00f2.jpg', '6a59a182555419c251de00f2.jpg', '03ac9bca77b33fcc8dc3d99529ee3553.jpg', 'image/jpeg', 59285, 'qiniu-kodo-1', NULL, 1, '系统管理员', '2026-07-17 11:29:07', '2026-07-17 11:29:07');
INSERT INTO `sys_oss_file` VALUES (5, '2078768869686726657', 'https://oss.wuhobin.top/base/20260719/6a5c93fd80e6bd74478afdc1.jpg', '6a5c93fd80e6bd74478afdc1.jpg', '微信图片_20260716221806_14559_11.jpg', 'image/jpeg', 35213, 'qiniu-kodo-1', NULL, 1811, '普通用户', '2026-07-19 17:08:15', '2026-07-19 17:08:15');

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
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'admin', '超级管理员', '拥有一切权限\n', '2024-11-16 12:29:00', '2024-11-16 12:29:00');
INSERT INTO `sys_role` VALUES (2, 'demo', '演示账号', '仅提供演示用，所有按钮权限可看到但不能操作', '2024-11-21 22:59:30', '2024-11-21 22:59:29');

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
) ENGINE = InnoDB AUTO_INCREMENT = 527 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色-权限资源关联表 ' ROW_FORMAT = DYNAMIC;

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
INSERT INTO `sys_role_menu` VALUES (521, 2, 111);
INSERT INTO `sys_role_menu` VALUES (522, 2, 112);
INSERT INTO `sys_role_menu` VALUES (524, 2, 113);
INSERT INTO `sys_role_menu` VALUES (525, 2, 114);
INSERT INTO `sys_role_menu` VALUES (526, 2, 115);
INSERT INTO `sys_role_menu` VALUES (523, 2, 116);
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

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
                             `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             `username` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '账号',
                             `password` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录密码',
                             `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                             `status` int NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
                             `ip` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'ip地址',
                             `ip_location` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'ip来源',
                             `os` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录系统',
                             `last_login_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
                             `browser` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '浏览器',
                             `nickname` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '昵称',
                             `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '头像',
                             `mobile` varchar(15) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '手机号',
                             `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
                             `sex` int NULL DEFAULT NULL COMMENT '性别',
                             `login_type` int NULL DEFAULT NULL COMMENT '登录方式',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1812 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$Olf5IWFsSL5uw6lQQxCCX.Yx9gFvdIbiyXrXPmCm9N9OVesHYMBQy', '2024-12-27 14:16:17', '2026-07-17 11:29:07', 1, '127.0.0.1', '内网IP|内网IP', 'Windows', '2026-07-03 14:17:04', 'Chrome', '系统管理员', 'https://oss.wuhobin.top/base/20260717/6a59a182555419c251de00f2.jpg', '17608485482', '1289066006@qq.com', 1, NULL);
INSERT INTO `sys_user` VALUES (1811, 'user', '$2a$10$V6wNWTljL/OQUcyhIy22QONd7TOrWS0rtoFJV.G1N.p8Uv7QP1i2a', '2026-07-17 20:30:15', '2026-07-19 17:08:15', 1, NULL, NULL, NULL, '2026-07-17 20:30:14', NULL, '普通用户1', 'https://oss.wuhobin.top/base/20260719/6a5c93fd80e6bd74478afdc1.jpg', '18772916901', '1289066006@qq.com', 1, NULL);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
                                  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `role_id` int NULL DEFAULT NULL COMMENT '角色ID',
                                  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '系统管理 - 用户角色关联表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (56, 1, 1);
INSERT INTO `sys_user_role` VALUES (57, 2, 1811);

SET FOREIGN_KEY_CHECKS = 1;
