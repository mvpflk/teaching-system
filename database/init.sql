-- ============================================================================
-- 教学管理系统 数据库初始化 (Docker自动执行)
-- 生成时间: 2026-05-25 13:39:51
-- ============================================================================
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: teaching_system
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `achievement_definitions`
--

DROP TABLE IF EXISTS `achievement_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `achievement_definitions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成就ID',
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成就代码',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成就名称',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '成就描述',
  `icon_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标URL',
  `category` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类: homework/exam/credits/social/system',
  `credit_reward` int DEFAULT '0' COMMENT '成就奖励积分',
  `unlock_conditions` json NOT NULL COMMENT '解锁条件(JSON)',
  `rarity_level` int DEFAULT '1' COMMENT '稀有度(1-5)',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  CONSTRAINT `achievement_definitions_ibfk_1` FOREIGN KEY (`category`) REFERENCES `dict_question_type` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `achievement_definitions`
--

LOCK TABLES `achievement_definitions` WRITE;
/*!40000 ALTER TABLE `achievement_definitions` DISABLE KEYS */;
/*!40000 ALTER TABLE `achievement_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_call_log`
--

DROP TABLE IF EXISTS `ai_call_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint DEFAULT '1',
  `user_id` bigint DEFAULT NULL,
  `capability` varchar(100) DEFAULT NULL,
  `provider` varchar(50) DEFAULT NULL,
  `prompt_hash` varchar(64) DEFAULT NULL,
  `tokens_used` int DEFAULT '0',
  `latency_ms` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_call_log`
--

LOCK TABLES `ai_call_log` WRITE;
/*!40000 ALTER TABLE `ai_call_log` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_outputs`
--

DROP TABLE IF EXISTS `ai_outputs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_outputs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `node_id` bigint NOT NULL COMMENT '关联知识节点ID → knowledge_nodes.id',
  `teacher_id` bigint NOT NULL COMMENT '教师用户ID',
  `output_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产出类型（如 lesson_plan/quiz/slide/summary 等）',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT 'AI生成的完整内容',
  `is_latest` tinyint(1) DEFAULT '1' COMMENT '是否最新版本（1=最新, 0=历史）',
  `version_seq` int DEFAULT '1' COMMENT '版本序号 1=最新',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0=草稿, 1=已发布, 2=已归档',
  `tokens_used` int DEFAULT '0' COMMENT '消耗token数',
  `latency_ms` int DEFAULT '0' COMMENT '响应延迟（毫秒）',
  `rating` tinyint DEFAULT NULL COMMENT '教师评分 1-5',
  `feedback` text COLLATE utf8mb4_unicode_ci COMMENT '教师文字反馈',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_node_type_latest` (`node_id`,`output_type`,`is_latest`),
  KEY `idx_teacher_id` (`teacher_id`),
  -- FK fk_ao_node removed (v147): node_id reused for taskId in DIAGNOSIS/CONSOLIDATION_MATERIAL
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI教学产出表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_outputs`
--

LOCK TABLES `ai_outputs` WRITE;
/*!40000 ALTER TABLE `ai_outputs` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alert_last_scan`
--

DROP TABLE IF EXISTS `alert_last_scan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_last_scan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scan_type` varchar(30) NOT NULL COMMENT 'FULL | INCREMENTAL',
  `last_submission_id` bigint DEFAULT NULL COMMENT '增量扫描截止的提交记录ID',
  `last_scan_time` datetime DEFAULT NULL COMMENT '上次扫描时间',
  `scanned_count` int DEFAULT '0' COMMENT '扫描学生数',
  `alert_count` int DEFAULT '0' COMMENT '触发预警数',
  `status` varchar(20) NOT NULL DEFAULT 'COMPLETED' COMMENT 'RUNNING|COMPLETED|FAILED',
  `error_msg` text COMMENT '失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scan_type` (`scan_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='扫描状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert_last_scan`
--

LOCK TABLES `alert_last_scan` WRITE;
/*!40000 ALTER TABLE `alert_last_scan` DISABLE KEYS */;
/*!40000 ALTER TABLE `alert_last_scan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alert_records`
--

DROP TABLE IF EXISTS `alert_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_id` bigint NOT NULL COMMENT '关联 alert_rules.id',
  `student_id` bigint NOT NULL COMMENT '触发学生ID → students.id',
  `class_id` bigint DEFAULT NULL COMMENT '学生所在班级ID',
  `matched_submission_ids` json DEFAULT NULL COMMENT '触发的提交记录ID数组',
  `alert_summary` varchar(500) DEFAULT NULL COMMENT '预警摘要',
  `notified_teacher` tinyint NOT NULL DEFAULT '0' COMMENT '是否已通知班主任',
  `notified_parents` tinyint NOT NULL DEFAULT '0' COMMENT '是否已通知家长',
  `handled_status` varchar(20) NOT NULL DEFAULT 'UNREAD' COMMENT 'UNREAD|READ|CONTACTED|IGNORED',
  `handled_by` bigint DEFAULT NULL COMMENT '处理人用户ID',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_class` (`class_id`),
  KEY `idx_rule_student` (`rule_id`,`student_id`),
  KEY `idx_handled_status` (`handled_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert_records`
--

LOCK TABLES `alert_records` WRITE;
/*!40000 ALTER TABLE `alert_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `alert_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alert_rules`
--

DROP TABLE IF EXISTS `alert_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '规则名称',
  `description` varchar(500) DEFAULT NULL COMMENT '规则说明',
  `alert_type` varchar(30) NOT NULL COMMENT 'LOW_SCORE | MISSING | CUSTOM',
  `task_types` varchar(200) DEFAULT NULL COMMENT '适用任务类型，逗号分隔',
  `min_consecutive` int NOT NULL DEFAULT '3' COMMENT '连续触发次数',
  `score_threshold` decimal(6,1) DEFAULT '60.0' COMMENT '分数阈值(LOW_SCORE用)',
  `days_lookback` int NOT NULL DEFAULT '90' COMMENT '回溯天数',
  `cooldown_days` int NOT NULL DEFAULT '7' COMMENT '同规则同学生冷却天数',
  `is_builtin` tinyint NOT NULL DEFAULT '0' COMMENT '1=内置不可删',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1=启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`alert_type`),
  KEY `idx_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预警规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert_rules`
--

LOCK TABLES `alert_rules` WRITE;
/*!40000 ALTER TABLE `alert_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `alert_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作用户名',
  `role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作时角色',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型: CREATE/UPDATE/DELETE/LOGIN/LOGOUT/EXPORT/IMPORT/AUTH',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作描述(中文)',
  `operation` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '具体操作标识, 如 exam.create / homework.grade',
  `target_table` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标表名',
  `target_id` bigint DEFAULT NULL COMMENT '目标记录ID',
  `request_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HTTP方法: GET/POST/PUT/DELETE',
  `params` json DEFAULT NULL COMMENT '请求参数(JSON)',
  `old_value` json DEFAULT NULL COMMENT '变更前值(JSON)',
  `new_value` json DEFAULT NULL COMMENT '变更后值(JSON)',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS' COMMENT '操作结果: SUCCESS/FAIL/ERROR',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_target` (`target_table`,`target_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
-- audit_log data removed: contained development login credentials in plaintext
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bbs_bookmarks`
--

DROP TABLE IF EXISTS `bbs_bookmarks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bbs_bookmarks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `post_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`,`post_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BBS收藏';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bbs_bookmarks`
--

LOCK TABLES `bbs_bookmarks` WRITE;
/*!40000 ALTER TABLE `bbs_bookmarks` DISABLE KEYS */;
/*!40000 ALTER TABLE `bbs_bookmarks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bbs_categories`
--

DROP TABLE IF EXISTS `bbs_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bbs_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版块名称',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '版块描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `post_count` int DEFAULT '0' COMMENT '帖子数',
  `status` tinyint DEFAULT '1' COMMENT '0隐藏 1显示',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BBS版块';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bbs_categories`
--

LOCK TABLES `bbs_categories` WRITE;
/*!40000 ALTER TABLE `bbs_categories` DISABLE KEYS */;
/*!40000 ALTER TABLE `bbs_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bbs_likes`
--

DROP TABLE IF EXISTS `bbs_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bbs_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_id` bigint NOT NULL COMMENT '帖子ID或回复ID',
  `target_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'post/reply',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_id`,`target_type`),
  KEY `idx_target` (`target_id`,`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BBS点赞';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bbs_likes`
--

LOCK TABLES `bbs_likes` WRITE;
/*!40000 ALTER TABLE `bbs_likes` DISABLE KEYS */;
/*!40000 ALTER TABLE `bbs_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bbs_muted_users`
--

DROP TABLE IF EXISTS `bbs_muted_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bbs_muted_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '被禁言用户(users.id)',
  `muted_by` bigint NOT NULL COMMENT '操作教师(users.id)',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL COMMENT '禁言过期时间，NULL表示永久',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  KEY `idx_muted_by` (`muted_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BBS禁言用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bbs_muted_users`
--

LOCK TABLES `bbs_muted_users` WRITE;
/*!40000 ALTER TABLE `bbs_muted_users` DISABLE KEYS */;
/*!40000 ALTER TABLE `bbs_muted_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bbs_posts`
--

DROP TABLE IF EXISTS `bbs_posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bbs_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `images` json DEFAULT NULL COMMENT '图片URL数组',
  `author_id` bigint NOT NULL COMMENT '发帖人(users.id)',
  `is_sticky` tinyint DEFAULT '0' COMMENT '0普通 1置顶',
  `is_highlighted` tinyint DEFAULT '0' COMMENT '0普通 1加精',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'normal' COMMENT 'normal正常/deleted已删/blocked锁定',
  `view_count` int DEFAULT '0',
  `like_count` int DEFAULT '0',
  `reply_count` int DEFAULT '0',
  `last_reply_time` datetime DEFAULT NULL COMMENT '最后回复时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_moral_behavior` int DEFAULT '0' COMMENT '是否德育行为表扬帖: 0=否, 1=是',
  `praised_student_id` bigint DEFAULT NULL COMMENT '受表扬学生ID, 关联 students.id',
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_author` (`author_id`),
  KEY `idx_sticky` (`is_sticky`,`last_reply_time`),
  KEY `idx_status` (`status`),
  KEY `idx_category_time` (`category_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BBS帖子';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bbs_posts`
--

LOCK TABLES `bbs_posts` WRITE;
/*!40000 ALTER TABLE `bbs_posts` DISABLE KEYS */;
/*!40000 ALTER TABLE `bbs_posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bbs_replies`
--

DROP TABLE IF EXISTS `bbs_replies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bbs_replies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL COMMENT '回复的回复ID(@引用)',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `author_id` bigint NOT NULL,
  `like_count` int DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'normal',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_post` (`post_id`),
  KEY `idx_author` (`author_id`),
  CONSTRAINT `bbs_replies_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `bbs_posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BBS回复';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bbs_replies`
--

LOCK TABLES `bbs_replies` WRITE;
/*!40000 ALTER TABLE `bbs_replies` DISABLE KEYS */;
/*!40000 ALTER TABLE `bbs_replies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_album`
--

DROP TABLE IF EXISTS `class_album`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_album` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `uploader_id` bigint NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `caption` varchar(200) DEFAULT NULL,
  `like_count` int DEFAULT '0',
  `status` varchar(20) DEFAULT 'APPROVED' COMMENT '审核状态: PENDING/APPROVED/REJECTED',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人用户ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_class` (`class_id`),
  KEY `idx_status` (`status`),
  KEY `idx_class_status` (`class_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级相册';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_album`
--

LOCK TABLES `class_album` WRITE;
/*!40000 ALTER TABLE `class_album` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_album` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_album_comments`
--

DROP TABLE IF EXISTS `class_album_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_album_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `photo_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_photo` (`photo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级相册评论';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_album_comments`
--

LOCK TABLES `class_album_comments` WRITE;
/*!40000 ALTER TABLE `class_album_comments` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_album_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_album_likes`
--

DROP TABLE IF EXISTS `class_album_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_album_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `photo_id` bigint NOT NULL COMMENT '照片ID class_album.id',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID users.id',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_photo_user` (`photo_id`,`user_id`),
  KEY `idx_photo` (`photo_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级相册点赞记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_album_likes`
--

LOCK TABLES `class_album_likes` WRITE;
/*!40000 ALTER TABLE `class_album_likes` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_album_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_students`
--

DROP TABLE IF EXISTS `class_students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_students` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `join_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `leave_time` datetime DEFAULT NULL COMMENT '离开时间',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0已离班 1在班',
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_student` (`class_id`,`student_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_student_id` (`student_id`),
  CONSTRAINT `class_students_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `class_students_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级学生关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_students`
--

LOCK TABLES `class_students` WRITE;
/*!40000 ALTER TABLE `class_students` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_teachers`
--

DROP TABLE IF EXISTS `class_teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_teachers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `subject` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任教科目',
  `assign_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_teacher` (`class_id`,`teacher_id`,`subject`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  CONSTRAINT `class_teachers_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `class_teachers_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级教师关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_teachers`
--

LOCK TABLES `class_teachers` WRITE;
/*!40000 ALTER TABLE `class_teachers` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_teachers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_type_config`
--

DROP TABLE IF EXISTS `class_type_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_type_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stage_id` bigint NOT NULL COMMENT '学段ID → stages.id',
  `type_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型编码（如 GENERAL, PUGAO_PURE, VOCATIONAL_ACADEMIC）',
  `type_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称（如 普通班, 纯普高, 升学型职高）',
  `default_major` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '默认专业仅职高类型使用',
  `category` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GENERAL' COMMENT '大类: GENERAL=普通, PUGAO=普高, VOCATIONAL=职高',
  `sort_order` int DEFAULT '0' COMMENT '排序，越小越靠前',
  `school_id` bigint DEFAULT '0' COMMENT '学校ID，0=全局默认配置',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_type` (`stage_id`,`type_code`,`school_id`),
  KEY `idx_stage_id` (`stage_id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级类型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_type_config`
--

LOCK TABLES `class_type_config` WRITE;
/*!40000 ALTER TABLE `class_type_config` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `class_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级名称',
  `class_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级编码',
  `grade` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '年级',
  `major` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业',
  `academic_year` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学年(格式: 2025-2026)',
  `semester` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学期(上/下)',
  `head_teacher_id` bigint DEFAULT NULL COMMENT '班主任ID',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0已毕业 1在读',
  `student_count` int DEFAULT '0' COMMENT '学生人数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  `stage_id` bigint DEFAULT '4',
  `class_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'VOCATIONAL' COMMENT '班级类型编码 → class_type_config.type_code',
  PRIMARY KEY (`id`),
  UNIQUE KEY `class_code` (`class_code`),
  KEY `idx_class_code` (`class_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classroom_participations`
--

DROP TABLE IF EXISTS `classroom_participations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classroom_participations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `participation_type` varchar(30) NOT NULL COMMENT 'QUIZZED/BUZZED/VOTED',
  `is_correct` tinyint DEFAULT NULL COMMENT '0=错 1=对 NULL=未评分(投票)',
  `score_earned` int DEFAULT '0',
  `response` text COMMENT '学生回答内容',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_student_type` (`session_id`,`student_id`,`participation_type`),
  KEY `idx_session` (`session_id`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classroom_participations`
--

LOCK TABLES `classroom_participations` WRITE;
/*!40000 ALTER TABLE `classroom_participations` DISABLE KEYS */;
/*!40000 ALTER TABLE `classroom_participations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classroom_questions`
--

DROP TABLE IF EXISTS `classroom_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classroom_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL,
  `subject` varchar(50) DEFAULT NULL,
  `chapter` varchar(100) DEFAULT NULL,
  `tag` varchar(50) DEFAULT NULL COMMENT '题目标签',
  `content` varchar(1000) NOT NULL,
  `reference_answer` varchar(1000) DEFAULT NULL COMMENT '参考答案',
  `difficulty` tinyint DEFAULT '2' COMMENT '难度1-5',
  `source` varchar(20) DEFAULT 'MANUAL' COMMENT 'MANUAL/IMPORT/QUESTION_BANK',
  `source_question_id` bigint DEFAULT NULL COMMENT '从question_bank选取时记录原题ID',
  `usage_count` int DEFAULT '0' COMMENT '被抽问次数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_subject_chapter` (`subject`,`chapter`),
  KEY `idx_tag` (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classroom_questions`
--

LOCK TABLES `classroom_questions` WRITE;
/*!40000 ALTER TABLE `classroom_questions` DISABLE KEYS */;
/*!40000 ALTER TABLE `classroom_questions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classroom_sessions`
--

DROP TABLE IF EXISTS `classroom_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classroom_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `session_type` varchar(30) NOT NULL COMMENT 'QUIZ/BUZZ/POLL',
  `scene_mode` varchar(20) NOT NULL DEFAULT 'LAB' COMMENT 'LAB/CLASSROOM',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED',
  `question_text` varchar(500) DEFAULT NULL,
  `question_id` bigint DEFAULT NULL COMMENT '关联 classroom_questions.id 或 question_bank.id',
  `poll_data` json DEFAULT NULL COMMENT '投票选项JSON',
  `buzz_winner_id` bigint DEFAULT NULL COMMENT '抢答胜出学生ID',
  `buzz_winner_time` bigint DEFAULT NULL COMMENT '抢答毫秒时间戳',
  `task_id` bigint DEFAULT NULL COMMENT '绑定的任务ID（可选）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_class` (`class_id`,`created_at`),
  KEY `idx_teacher` (`teacher_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classroom_sessions`
--

LOCK TABLES `classroom_sessions` WRITE;
/*!40000 ALTER TABLE `classroom_sessions` DISABLE KEYS */;
/*!40000 ALTER TABLE `classroom_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credit_redemption_orders`
--

DROP TABLE IF EXISTS `credit_redemption_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credit_redemption_orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `item_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `credit_cost` int NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `processed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分商城兑换工单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credit_redemption_orders`
--

LOCK TABLES `credit_redemption_orders` WRITE;
/*!40000 ALTER TABLE `credit_redemption_orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `credit_redemption_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credit_rules`
--

DROP TABLE IF EXISTS `credit_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credit_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则代码',
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `action_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作类型: homework作业/exam考试/sign签到/assist互助/daily每日',
  `credit_value` int NOT NULL COMMENT '积分值(可为负数)',
  `max_daily_count` int DEFAULT NULL COMMENT '每日最大次数(null表示无限制)',
  `conditions` json DEFAULT NULL COMMENT '触发条件(JSON)',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规则说明',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `rule_code` (`rule_code`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credit_rules`
--

LOCK TABLES `credit_rules` WRITE;
/*!40000 ALTER TABLE `credit_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `credit_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credit_shop_items`
--

DROP TABLE IF EXISTS `credit_shop_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credit_shop_items` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `item_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品代码',
  `item_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `item_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品类型: coupon券/card卡/badge徽章/privilege特权',
  `credit_price` int NOT NULL COMMENT '所需积分',
  `stock_count` int DEFAULT '-1' COMMENT '库存(-1表示无限)',
  `sold_count` int DEFAULT '0' COMMENT '已兑换数量',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '商品说明',
  `usage_rules` text COLLATE utf8mb4_unicode_ci COMMENT '使用规则',
  `valid_days` int DEFAULT NULL COMMENT '有效期(天)',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品图片',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0下架 1上架',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `item_code` (`item_code`),
  KEY `idx_type` (`item_type`),
  KEY `idx_status` (`status`),
  KEY `idx_price` (`credit_price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分商城表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credit_shop_items`
--

LOCK TABLES `credit_shop_items` WRITE;
/*!40000 ALTER TABLE `credit_shop_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `credit_shop_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credit_term_snapshots`
--

DROP TABLE IF EXISTS `credit_term_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credit_term_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `term_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `credits_earned` int DEFAULT '0',
  `term_total` int DEFAULT '0',
  `snapshot_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_term` (`student_id`,`term_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分学期快照表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credit_term_snapshots`
--

LOCK TABLES `credit_term_snapshots` WRITE;
/*!40000 ALTER TABLE `credit_term_snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `credit_term_snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credit_transactions`
--

DROP TABLE IF EXISTS `credit_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credit_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `rule_id` bigint DEFAULT NULL COMMENT '规则ID',
  `transaction_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交易类型: earn获得/consume消耗',
  `credit_amount` int NOT NULL COMMENT '积分变动数量',
  `balance_after` int DEFAULT NULL COMMENT '变动后余额',
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源类型: homework/exam/sign/assist/redeem/adjust',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID(作业ID/考试ID等)',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  `expired` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_type` (`transaction_type`),
  KEY `idx_time` (`create_time`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `rule_id` (`rule_id`),
  KEY `idx_student_time` (`student_id`,`create_time`),
  CONSTRAINT `credit_transactions_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `credit_transactions_ibfk_2` FOREIGN KEY (`rule_id`) REFERENCES `credit_rules` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分交易记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credit_transactions`
--

LOCK TABLES `credit_transactions` WRITE;
/*!40000 ALTER TABLE `credit_transactions` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_class_status`
--

DROP TABLE IF EXISTS `dict_class_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_class_status` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '状态ID',
  `status_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态代码',
  `status_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `status_code` (`status_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级状态字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_class_status`
--

LOCK TABLES `dict_class_status` WRITE;
/*!40000 ALTER TABLE `dict_class_status` DISABLE KEYS */;
INSERT INTO `dict_class_status` VALUES (1,'GRADUATED','已毕业','2026-05-01 06:28:44'),(2,'ACTIVE','在读','2026-05-01 06:28:44'),(3,'SUSPENDED','休学','2026-05-01 06:28:44');
/*!40000 ALTER TABLE `dict_class_status` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_grade`
--

DROP TABLE IF EXISTS `dict_grade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_grade` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `grade_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年级名称',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '0=禁用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `stage_id` bigint DEFAULT NULL COMMENT '学段ID(关联edu_stage_config.id)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_grade`
--

LOCK TABLES `dict_grade` WRITE;
/*!40000 ALTER TABLE `dict_grade` DISABLE KEYS */;
INSERT INTO `dict_grade` VALUES (1,'2024级',1,1,'2026-05-04 01:21:22','2026-05-04 01:21:22',NULL),(2,'2025级',2,1,'2026-05-04 01:21:22','2026-05-04 01:21:22',NULL),(3,'2026级',3,1,'2026-05-04 01:21:22','2026-05-04 01:21:22',NULL);
/*!40000 ALTER TABLE `dict_grade` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_major`
--

DROP TABLE IF EXISTS `dict_major`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_major` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `major_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业名称',
  `sort_order` int DEFAULT '0' COMMENT '排序，越小越靠前',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_major`
--

LOCK TABLES `dict_major` WRITE;
/*!40000 ALTER TABLE `dict_major` DISABLE KEYS */;
INSERT INTO `dict_major` VALUES (1,'计算机',1,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(2,'财会',2,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(3,'旅游',3,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(4,'农学',4,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(5,'机械',5,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(6,'建筑',6,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(7,'烹饪',7,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(8,'电子',8,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(9,'教育',9,1,'2026-05-10 13:57:34','2026-05-10 13:57:34'),(10,'服装',10,1,'2026-05-10 13:57:34','2026-05-10 13:57:34');
/*!40000 ALTER TABLE `dict_major` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_question_type`
--

DROP TABLE IF EXISTS `dict_question_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_question_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `type_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型代码',
  `type_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
  `is_selectable` tinyint DEFAULT '1' COMMENT '是否可选(考试时)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `type_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目类型字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_question_type`
--

LOCK TABLES `dict_question_type` WRITE;
/*!40000 ALTER TABLE `dict_question_type` DISABLE KEYS */;
INSERT INTO `dict_question_type` VALUES (1,'SINGLE_CHOICE','单选题',1,'2026-05-01 06:28:44'),(2,'MULTI_CHOICE','多选题',1,'2026-05-01 06:28:44'),(3,'TRUE_FALSE','判断题',1,'2026-05-01 06:28:44'),(4,'FILL_BLANK','填空题',1,'2026-05-01 06:28:44'),(5,'SHORT_ANSWER','简答题',0,'2026-05-01 06:28:44');
/*!40000 ALTER TABLE `dict_question_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_subject`
--

DROP TABLE IF EXISTS `dict_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学科名称',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '0=禁用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学科字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_subject`
--

LOCK TABLES `dict_subject` WRITE;
/*!40000 ALTER TABLE `dict_subject` DISABLE KEYS */;
INSERT INTO `dict_subject` VALUES (1,'语文[初中]',1,1,'2026-05-04 01:21:22','2026-05-04 01:31:53'),(2,'数学[初中]',2,1,'2026-05-04 01:21:22','2026-05-04 01:31:59'),(3,'英语[初中]',3,1,'2026-05-04 01:21:22','2026-05-04 01:32:05'),(4,'信息技术应用基础',4,1,'2026-05-04 01:21:22','2026-05-04 01:32:19'),(5,'网络应用基础',5,1,'2026-05-04 01:21:22','2026-05-04 01:32:28'),(6,'办公应用基础',6,1,'2026-05-04 01:21:22','2026-05-04 01:32:37'),(17,'Access',7,1,'2026-05-04 01:47:23','2026-05-04 01:47:29'),(20,'语文[职高]',0,1,'2026-05-10 14:43:02','2026-05-10 14:43:02'),(21,'语文[普高]',0,1,'2026-05-10 14:43:11','2026-05-10 14:43:11'),(22,'数学[职高]',0,1,'2026-05-10 14:43:47','2026-05-10 14:43:47'),(23,'数学[普高]',0,1,'2026-05-10 14:44:08','2026-05-10 14:44:08'),(24,'英语[职高]',0,1,'2026-05-10 14:44:41','2026-05-10 14:44:41'),(25,'英语[普高]',0,1,'2026-05-10 14:45:07','2026-05-10 14:45:07');
/*!40000 ALTER TABLE `dict_subject` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_user_role`
--

DROP TABLE IF EXISTS `dict_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色代码',
  `role_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_level` int DEFAULT '0' COMMENT '角色级别(数值越大权限越高)',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_user_role`
--

LOCK TABLES `dict_user_role` WRITE;
/*!40000 ALTER TABLE `dict_user_role` DISABLE KEYS */;
INSERT INTO `dict_user_role` VALUES (1,'ADMIN','系统管理员',100,'系统最高权限',1,'2026-05-01 06:28:44','2026-05-01 06:28:44'),(2,'TEACHER','教师',50,'任课教师',1,'2026-05-01 06:28:44','2026-05-01 06:28:44'),(3,'HEAD_TEACHER','班主任',60,'班主任权限',1,'2026-05-01 06:28:44','2026-05-01 06:28:44'),(4,'STUDENT','学生',10,'学生权限',1,'2026-05-01 06:28:44','2026-05-01 06:28:44'),(5,'SUPER_ADMIN','超级管理员',200,'超级管理员（系统设置/数据管理）',1,'2026-05-04 00:35:48','2026-05-04 00:35:48');
/*!40000 ALTER TABLE `dict_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_template`
--

DROP TABLE IF EXISTS `document_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `fields_json` text,
  `school_id` bigint DEFAULT '1',
  `stage_id` bigint DEFAULT '4',
  `created_by` bigint DEFAULT NULL,
  `scope` varchar(50) DEFAULT 'school',
  `version` int DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_template`
--

LOCK TABLES `document_template` WRITE;
/*!40000 ALTER TABLE `document_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `document_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drafts`
--

DROP TABLE IF EXISTS `drafts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drafts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `content` json DEFAULT NULL COMMENT '草稿内容JSON',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_task` (`student_id`,`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生作答草稿';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drafts`
--

LOCK TABLES `drafts` WRITE;
/*!40000 ALTER TABLE `drafts` DISABLE KEYS */;
/*!40000 ALTER TABLE `drafts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_stage_config`
--

DROP TABLE IF EXISTS `edu_stage_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_stage_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL DEFAULT '1',
  `capability_key` varchar(50) NOT NULL,
  `enabled` tinyint DEFAULT '0',
  `config_json` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_cap` (`school_id`,`capability_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_stage_config`
--

LOCK TABLES `edu_stage_config` WRITE;
/*!40000 ALTER TABLE `edu_stage_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `edu_stage_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_shares`
--

DROP TABLE IF EXISTS `exam_shares`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_shares` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `share_code` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分享码',
  `exam_id` bigint DEFAULT NULL,
  `task_id` bigint DEFAULT NULL COMMENT '任务ID(Task)',
  `task_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务类型(FORMATIVE/SUMMATIVE)',
  `creator_id` bigint NOT NULL COMMENT '分享者教师ID',
  `creator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分享者姓名',
  `exam_title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '试卷标题',
  `exam_subject` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '试卷学科',
  `question_count` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  `max_uses` int DEFAULT '10' COMMENT '最大使用次数',
  `use_count` int DEFAULT '0' COMMENT '已使用次数',
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `share_code` (`share_code`),
  KEY `idx_code` (`share_code`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_shares`
--

LOCK TABLES `exam_shares` WRITE;
/*!40000 ALTER TABLE `exam_shares` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_shares` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_syllabus`
--

DROP TABLE IF EXISTS `exam_syllabus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_syllabus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` bigint NOT NULL COMMENT '所属学科ID → dict_subject.id',
  `exam_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GENERAL' COMMENT '考试类型: SINGLE_RECRUIT(单招)/COUNTERPART(对口升学)/GENERAL(通用)',
  `knowledge_dim` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BOTH' COMMENT '维度: THEORY(应知)/PRACTICE(应会)/BOTH(综合)',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '考纲标题',
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '考纲正文(Markdown)',
  `version` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '1.0' COMMENT '考纲版本号',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0=禁用 1=启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人用户ID',
  `updated_by` bigint DEFAULT NULL COMMENT '最后修改人用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_exam` (`subject_id`,`exam_type`),
  KEY `idx_subject_id` (`subject_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_es_subject` FOREIGN KEY (`subject_id`) REFERENCES `dict_subject` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='升学考试考纲表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_syllabus`
--

LOCK TABLES `exam_syllabus` WRITE;
/*!40000 ALTER TABLE `exam_syllabus` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_syllabus_node_relation`
--

DROP TABLE IF EXISTS `exam_syllabus_node_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_syllabus_node_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `syllabus_id` bigint NOT NULL COMMENT '考纲ID → exam_syllabus.id',
  `node_id` bigint NOT NULL COMMENT '知识节点ID → knowledge_nodes.id',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_syllabus_node` (`syllabus_id`,`node_id`),
  KEY `idx_syllabus_id` (`syllabus_id`),
  KEY `idx_node_id` (`node_id`),
  CONSTRAINT `fk_esnr_node` FOREIGN KEY (`node_id`) REFERENCES `knowledge_nodes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_esnr_syllabus` FOREIGN KEY (`syllabus_id`) REFERENCES `exam_syllabus` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考纲-知识节点关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_syllabus_node_relation`
--

LOCK TABLES `exam_syllabus_node_relation` WRITE;
/*!40000 ALTER TABLE `exam_syllabus_node_relation` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `external_review`
--

DROP TABLE IF EXISTS `external_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `external_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `submission_id` bigint DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `reviewer_name` varchar(100) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'pending',
  `score_json` text,
  `submitted_at` datetime DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `external_review`
--

LOCK TABLES `external_review` WRITE;
/*!40000 ALTER TABLE `external_review` DISABLE KEYS */;
/*!40000 ALTER TABLE `external_review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_member`
--

DROP TABLE IF EXISTS `group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '组类型: TEACHING=教研组, LESSON_PREP=备课组',
  `group_id` bigint NOT NULL COMMENT '组ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID → teachers.id',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEMBER' COMMENT '角色: LEADER=组长, MEMBER=组员',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member` (`group_type`,`group_id`,`teacher_id`),
  KEY `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_member`
--

LOCK TABLES `group_member` WRITE;
/*!40000 ALTER TABLE `group_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `group_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_members`
--

DROP TABLE IF EXISTS `group_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL COMMENT '关联 student_groups.id',
  `student_id` bigint NOT NULL COMMENT '学生ID → students.id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_student` (`group_id`,`student_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_members`
--

LOCK TABLES `group_members` WRITE;
/*!40000 ALTER TABLE `group_members` DISABLE KEYS */;
/*!40000 ALTER TABLE `group_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jwt_blacklist`
--

DROP TABLE IF EXISTS `jwt_blacklist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jwt_blacklist` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `jti` varchar(128) NOT NULL COMMENT 'JWT Token ID (jti)',
  `expires_at` datetime NOT NULL COMMENT '黑名单过期时间（token原exp时间）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (`id`),
  KEY `idx_jti` (`jti`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='JWT黑名单（Redis回退/降级存储）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jwt_blacklist`
--

LOCK TABLES `jwt_blacklist` WRITE;
/*!40000 ALTER TABLE `jwt_blacklist` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `knowledge_nodes`
--

DROP TABLE IF EXISTS `knowledge_nodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_nodes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint DEFAULT NULL COMMENT '父节点ID → knowledge_nodes.id',
  `subject_id` bigint DEFAULT NULL COMMENT '所属学科ID → dict_subject.id',
  `level` int NOT NULL DEFAULT '2' COMMENT '层级: 1=学科, 2=章节, 3=任务, 4=知识点',
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点名称',
  `content` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'Markdown知识库内容',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `version` int DEFAULT '1' COMMENT '乐观锁版本号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_subject_id` (`subject_id`),
  KEY `idx_level` (`level`),
  CONSTRAINT `fk_kn_parent` FOREIGN KEY (`parent_id`) REFERENCES `knowledge_nodes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=822 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库节点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `knowledge_nodes`
--

LOCK TABLES `knowledge_nodes` WRITE;
/*!40000 ALTER TABLE `knowledge_nodes` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `late_submit_requests`
--

DROP TABLE IF EXISTS `late_submit_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `late_submit_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `submission_id` bigint NOT NULL COMMENT '提交ID',
  `reason` text COLLATE utf8mb4_unicode_ci COMMENT '补交原因',
  `request_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'pending' COMMENT '状态: pending待审批/approved已批准/rejected已拒绝',
  `handler_id` bigint DEFAULT NULL COMMENT '处理教师ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_comment` text COLLATE utf8mb4_unicode_ci COMMENT '处理意见',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_submission_id` (`submission_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补交申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `late_submit_requests`
--

LOCK TABLES `late_submit_requests` WRITE;
/*!40000 ALTER TABLE `late_submit_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `late_submit_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lesson_prep_group`
--

DROP TABLE IF EXISTS `lesson_prep_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lesson_prep_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '备课组名称',
  `teaching_group_id` bigint DEFAULT NULL COMMENT '所属教研组ID → teaching_group.id，可空',
  `grade_id` bigint DEFAULT NULL COMMENT '年级ID → dict_grade.id',
  `subject_id` bigint DEFAULT NULL COMMENT '学科ID → dict_subject.id',
  `class_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级类型编码 → class_type_config.type_code，可空',
  `school_id` bigint DEFAULT '0',
  `stage_id` bigint DEFAULT NULL COMMENT '学段ID',
  `leader_ids` json DEFAULT NULL COMMENT '组长ID列表（最多2人）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_school` (`school_id`),
  KEY `idx_stage` (`stage_id`),
  KEY `idx_teaching` (`teaching_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备课组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lesson_prep_group`
--

LOCK TABLES `lesson_prep_group` WRITE;
/*!40000 ALTER TABLE `lesson_prep_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `lesson_prep_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '内容',
  `type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'system' COMMENT '类型: system系统/homework作业/exam考试/credit积分/achievement成就',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读: 0否 1是',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_time` (`create_time`),
  KEY `idx_user_read_time` (`user_id`,`is_read`,`create_time`),
  KEY `idx_type_related` (`type`,`related_id`),
  KEY `idx_user_read` (`user_id`,`is_read`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parent_child_relations`
--

DROP TABLE IF EXISTS `parent_child_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parent_child_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NOT NULL COMMENT '家长用户ID → users.id',
  `student_id` bigint NOT NULL COMMENT '学生ID → students.id',
  `relation` varchar(20) NOT NULL DEFAULT 'FATHER' COMMENT '关系: FATHER/MOTHER/GUARDIAN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parent_student` (`parent_id`,`student_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='亲子关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parent_child_relations`
--

LOCK TABLES `parent_child_relations` WRITE;
/*!40000 ALTER TABLE `parent_child_relations` DISABLE KEYS */;
/*!40000 ALTER TABLE `parent_child_relations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `peer_review`
--

DROP TABLE IF EXISTS `peer_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `peer_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `submission_id` bigint NOT NULL,
  `score_json` json DEFAULT NULL,
  `submitted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `peer_review`
--

LOCK TABLES `peer_review` WRITE;
/*!40000 ALTER TABLE `peer_review` DISABLE KEYS */;
/*!40000 ALTER TABLE `peer_review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `practice_session_items`
--

DROP TABLE IF EXISTS `practice_session_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `practice_session_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `question_id` bigint DEFAULT NULL COMMENT 'question_bank.id，AI生成的为NULL',
  `question_type` varchar(30) NOT NULL,
  `question_text` text NOT NULL,
  `options` text,
  `correct_answer` varchar(500) DEFAULT NULL,
  `explanation` text,
  `source` varchar(20) DEFAULT 'bank' COMMENT 'bank/ai',
  `student_answer` varchar(2000) DEFAULT NULL,
  `is_correct` tinyint DEFAULT NULL,
  `auto_score` decimal(10,1) DEFAULT NULL COMMENT '自动评分得分',
  `answered_at` datetime DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='衍生练习题明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `practice_session_items`
--

LOCK TABLES `practice_session_items` WRITE;
/*!40000 ALTER TABLE `practice_session_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `practice_session_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `practice_sessions`
--

DROP TABLE IF EXISTS `practice_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `practice_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `source_type` varchar(20) DEFAULT 'wrong_book' COMMENT 'wrong_book/manual/ai_supplement',
  `total_questions` int DEFAULT '0',
  `correct_count` int DEFAULT '0',
  `wrong_count` int DEFAULT '0',
  `status` varchar(20) DEFAULT 'ongoing' COMMENT 'ongoing/finished',
  `weak_points` json DEFAULT NULL COMMENT '薄弱知识点 [{name,frequency}]',
  `started_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='衍生练习会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `practice_sessions`
--

LOCK TABLES `practice_sessions` WRITE;
/*!40000 ALTER TABLE `practice_sessions` DISABLE KEYS */;
/*!40000 ALTER TABLE `practice_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `practice_step_files`
--

DROP TABLE IF EXISTS `practice_step_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `practice_step_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `step_id` bigint NOT NULL COMMENT '关联步骤ID',
  `file_url` varchar(500) NOT NULL COMMENT '文件URL',
  `original_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  PRIMARY KEY (`id`),
  KEY `idx_step_id` (`step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训步骤附件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `practice_step_files`
--

LOCK TABLES `practice_step_files` WRITE;
/*!40000 ALTER TABLE `practice_step_files` DISABLE KEYS */;
/*!40000 ALTER TABLE `practice_step_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `practice_step_grades`
--

DROP TABLE IF EXISTS `practice_step_grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `practice_step_grades` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL COMMENT '关联提交ID',
  `step_id` bigint NOT NULL COMMENT '关联步骤ID',
  `step_score` decimal(5,1) DEFAULT NULL COMMENT '该步得分',
  `step_comment` varchar(500) DEFAULT NULL COMMENT '该步评语',
  PRIMARY KEY (`id`),
  KEY `idx_submission_id` (`submission_id`),
  KEY `idx_step_id` (`step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训步骤评分明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `practice_step_grades`
--

LOCK TABLES `practice_step_grades` WRITE;
/*!40000 ALTER TABLE `practice_step_grades` DISABLE KEYS */;
/*!40000 ALTER TABLE `practice_step_grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `practice_step_images`
--

DROP TABLE IF EXISTS `practice_step_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `practice_step_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `step_id` bigint NOT NULL COMMENT '关联步骤ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `order_index` int NOT NULL DEFAULT '0' COMMENT '排序序号',
  PRIMARY KEY (`id`),
  KEY `idx_step_id` (`step_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训步骤配图';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `practice_step_images`
--

LOCK TABLES `practice_step_images` WRITE;
/*!40000 ALTER TABLE `practice_step_images` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `practice_steps`
--

DROP TABLE IF EXISTS `practice_steps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `practice_steps` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT '关联任务ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（教师预设则为NULL）',
  `step_index` int NOT NULL DEFAULT '0' COMMENT '步骤序号',
  `title` varchar(200) NOT NULL COMMENT '步骤名称',
  `description` text COMMENT '步骤描述/操作说明',
  `images` json DEFAULT NULL COMMENT '配图URL数组',
  `files` json DEFAULT NULL COMMENT '附件数组[{name,url,size}]',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号(修改递增)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_task_student` (`task_id`,`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训步骤主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `practice_steps`
--

LOCK TABLES `practice_steps` WRITE;
/*!40000 ALTER TABLE `practice_steps` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `practice_submissions`
--

DROP TABLE IF EXISTS `practice_submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `practice_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT '关联任务ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'PENDING/SUBMITTED/GRADED/RETURNED',
  `overall_score` decimal(5,1) DEFAULT NULL COMMENT '总分',
  `overall_comment` text COMMENT '总评语',
  `graded_at` datetime DEFAULT NULL COMMENT '评分时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_student` (`task_id`,`student_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训提交评分记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `practice_submissions`
--

LOCK TABLES `practice_submissions` WRITE;
/*!40000 ALTER TABLE `practice_submissions` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question_bank`
--

DROP TABLE IF EXISTS `question_bank`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_bank` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学科/分类路径',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `question_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_text` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `options` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON选项',
  `correct_answer` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '正确答案',
  `explanation` text COLLATE utf8mb4_unicode_ci COMMENT '解析',
  `attachment_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件地址',
  `difficulty_level` int DEFAULT '1' COMMENT '1简单 2中等 3困难',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID',
  `status` int DEFAULT '1' COMMENT '1启用 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  `stage_id` bigint DEFAULT '4',
  `content_json` json DEFAULT NULL,
  `answer_schema` json DEFAULT NULL,
  `knowledge_points` json DEFAULT NULL,
  `version` int DEFAULT '1',
  `is_latest` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_subject` (`subject`),
  KEY `idx_question_type` (`question_type`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2458 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question_bank`
--

LOCK TABLES `question_bank` WRITE;
/*!40000 ALTER TABLE `question_bank` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `re_review_request`
--

DROP TABLE IF EXISTS `re_review_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `re_review_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `reason` text,
  `status` varchar(20) DEFAULT 'pending',
  `teacher_comment` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `re_review_request`
--

LOCK TABLES `re_review_request` WRITE;
/*!40000 ALTER TABLE `re_review_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `re_review_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `redeem_deliveries`
--

DROP TABLE IF EXISTS `redeem_deliveries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `redeem_deliveries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `item_name` varchar(100) DEFAULT NULL,
  `credit_cost` int DEFAULT '0',
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending/delivered',
  `delivered_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `delivered_at` datetime DEFAULT NULL,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `redeem_deliveries`
--

LOCK TABLES `redeem_deliveries` WRITE;
/*!40000 ALTER TABLE `redeem_deliveries` DISABLE KEYS */;
/*!40000 ALTER TABLE `redeem_deliveries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `redemption_codes`
--

DROP TABLE IF EXISTS `redemption_codes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `redemption_codes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '兑换码ID',
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '兑换码',
  `item_id` bigint NOT NULL COMMENT '商品ID',
  `student_id` bigint DEFAULT NULL COMMENT '领取学生ID',
  `redeem_time` datetime DEFAULT NULL COMMENT '兑换时间',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `use_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'unused' COMMENT '状态: unused未使用/redeemed已领取/used已使用/expired已过期',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_code` (`code`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_status` (`use_status`),
  KEY `item_id` (`item_id`),
  CONSTRAINT `redemption_codes_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `credit_shop_items` (`id`),
  CONSTRAINT `redemption_codes_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分兑换码表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `redemption_codes`
--

LOCK TABLES `redemption_codes` WRITE;
/*!40000 ALTER TABLE `redemption_codes` DISABLE KEYS */;
/*!40000 ALTER TABLE `redemption_codes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rubric`
--

DROP TABLE IF EXISTS `rubric`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rubric` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL COMMENT '量规名称',
  `school_id` bigint DEFAULT NULL COMMENT '学校ID（多租户）',
  `stage_id` bigint DEFAULT NULL COMMENT '学段ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建者(教师ID)',
  `scope` varchar(32) DEFAULT 'TEACHER' COMMENT '可见范围: TEACHER/SCHOOL/PUBLIC',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评分量规';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rubric`
--

LOCK TABLES `rubric` WRITE;
/*!40000 ALTER TABLE `rubric` DISABLE KEYS */;
/*!40000 ALTER TABLE `rubric` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rubric_dimension`
--

DROP TABLE IF EXISTS `rubric_dimension`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rubric_dimension` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rubric_id` bigint NOT NULL COMMENT '所属量规ID',
  `name` varchar(100) NOT NULL COMMENT '维度名称',
  `weight` decimal(5,2) NOT NULL COMMENT '权重(0.00~1.00)',
  `description` text COMMENT '维度描述',
  `levels_json` json DEFAULT NULL COMMENT '评分等级定义JSON',
  PRIMARY KEY (`id`),
  KEY `idx_rubric` (`rubric_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评分量规维度';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rubric_dimension`
--

LOCK TABLES `rubric_dimension` WRITE;
/*!40000 ALTER TABLE `rubric_dimension` DISABLE KEYS */;
/*!40000 ALTER TABLE `rubric_dimension` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `school_stages`
--

DROP TABLE IF EXISTS `school_stages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `school_stages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL,
  `stage_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_stage` (`school_id`,`stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校学段关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `school_stages`
--

LOCK TABLES `school_stages` WRITE;
/*!40000 ALTER TABLE `school_stages` DISABLE KEYS */;
/*!40000 ALTER TABLE `school_stages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `school_term`
--

DROP TABLE IF EXISTS `school_term`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `school_term` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL DEFAULT '1',
  `name` varchar(50) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `is_current` tinyint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `school_term`
--

LOCK TABLES `school_term` WRITE;
/*!40000 ALTER TABLE `school_term` DISABLE KEYS */;
/*!40000 ALTER TABLE `school_term` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schools`
--

DROP TABLE IF EXISTS `schools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schools` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学校名称',
  `code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学校编码',
  `region_id` bigint DEFAULT NULL COMMENT '所属区域ID(预留)',
  `status` tinyint DEFAULT '1' COMMENT '1=正常 0=停用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schools`
--

LOCK TABLES `schools` WRITE;
/*!40000 ALTER TABLE `schools` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shedlock`
--

DROP TABLE IF EXISTS `shedlock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shedlock` (
  `name` varchar(64) NOT NULL,
  `lock_until` timestamp(3) NOT NULL,
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(255) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ShedLock 分布式定时任务锁';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shedlock`
--

LOCK TABLES `shedlock` WRITE;
/*!40000 ALTER TABLE `shedlock` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `showcase_comments`
--

DROP TABLE IF EXISTS `showcase_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `showcase_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `work_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) NOT NULL,
  `status` varchar(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人',
  `review_time` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_work_status` (`work_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `showcase_comments`
--

LOCK TABLES `showcase_comments` WRITE;
/*!40000 ALTER TABLE `showcase_comments` DISABLE KEYS */;
/*!40000 ALTER TABLE `showcase_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `showcase_works`
--

DROP TABLE IF EXISTS `showcase_works`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `showcase_works` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作品标题',
  `source_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源: HOMEWORK/EXAM/PRACTICAL',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID',
  `student_id` bigint NOT NULL COMMENT '被推荐学生ID',
  `class_id` bigint DEFAULT NULL COMMENT '学生所在班级ID',
  `subject` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学科',
  `teacher_id` bigint NOT NULL COMMENT '推荐教师ID(users.id)',
  `teacher_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教师评语',
  `show_scope` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CLASS' COMMENT 'CLASS/MULTI_CLASS/SCHOOL',
  `target_class_ids` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标班级ID JSON数组',
  `credit_awarded` int NOT NULL DEFAULT '0' COMMENT '已发放积分',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1展示 0下架',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_source` (`source_type`),
  KEY `idx_scope` (`show_scope`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优秀作品展示墙';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `showcase_works`
--

LOCK TABLES `showcase_works` WRITE;
/*!40000 ALTER TABLE `showcase_works` DISABLE KEYS */;
/*!40000 ALTER TABLE `showcase_works` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sign_records`
--

DROP TABLE IF EXISTS `sign_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sign_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `sign_date` date NOT NULL COMMENT '签到日期',
  `sign_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `credit_earned` int DEFAULT '0' COMMENT '获得积分',
  `streak_day` int DEFAULT NULL COMMENT '连续签到天数',
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_date` (`student_id`,`sign_date`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_sign_date` (`sign_date`),
  CONSTRAINT `sign_records_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sign_records`
--

LOCK TABLES `sign_records` WRITE;
/*!40000 ALTER TABLE `sign_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `sign_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stages`
--

DROP TABLE IF EXISTS `stages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PRIMARY/JUNIOR/SENIOR/VOCATIONAL',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '小学/初中/普高/职高',
  `sort_order` int DEFAULT '0',
  `grade_years` int DEFAULT '3' COMMENT '该学段年制',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stages`
--

LOCK TABLES `stages` WRITE;
/*!40000 ALTER TABLE `stages` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_achievements`
--

DROP TABLE IF EXISTS `student_achievements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_achievements` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `achievement_id` bigint NOT NULL COMMENT '成就ID',
  `unlock_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
  `notification_shown` tinyint DEFAULT '0' COMMENT '是否已显示通知',
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_achievement` (`student_id`,`achievement_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_unlock_time` (`unlock_time`),
  KEY `achievement_id` (`achievement_id`),
  CONSTRAINT `student_achievements_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `student_achievements_ibfk_2` FOREIGN KEY (`achievement_id`) REFERENCES `achievement_definitions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生成就表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_achievements`
--

LOCK TABLES `student_achievements` WRITE;
/*!40000 ALTER TABLE `student_achievements` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_achievements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_answers`
--

DROP TABLE IF EXISTS `student_answers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_answers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '答题ID',
  `submission_id` bigint DEFAULT NULL COMMENT '提交ID → task_submissions.id',
  `task_id` bigint DEFAULT NULL COMMENT '任务ID（冗余，加速按任务统计）',
  `result_id` bigint DEFAULT NULL COMMENT '考试结果ID（旧链路，已废弃）',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `student_answer` text COLLATE utf8mb4_unicode_ci COMMENT '学生答案',
  `is_correct` tinyint DEFAULT '0' COMMENT '是否正确: 0否 1是',
  `auto_score` decimal(6,1) DEFAULT NULL COMMENT '机器自动评分（客观题）',
  `teacher_score` decimal(6,1) DEFAULT NULL COMMENT '教师评分（主观题/覆写客观题）',
  `added_to_wrong` tinyint NOT NULL DEFAULT '0' COMMENT '是否已收录错题本: 0=否 1=是',
  `score` int DEFAULT NULL COMMENT '得分（旧链路，已废弃）',
  `answer_time` datetime DEFAULT NULL COMMENT '答题时间（旧链路）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  `stage_id` bigint DEFAULT '4',
  PRIMARY KEY (`id`),
  KEY `idx_submission` (`submission_id`),
  KEY `idx_task_q` (`task_id`,`question_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生答题记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_answers`
--

LOCK TABLES `student_answers` WRITE;
/*!40000 ALTER TABLE `student_answers` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_answers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_class_history`
--

DROP TABLE IF EXISTS `student_class_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_class_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `stage_id` bigint NOT NULL,
  `school_id` bigint NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `change_reason` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_class_history`
--

LOCK TABLES `student_class_history` WRITE;
/*!40000 ALTER TABLE `student_class_history` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_groups`
--

DROP TABLE IF EXISTS `student_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL COMMENT '所属班级ID → classes.id',
  `name` varchar(50) NOT NULL COMMENT '分组名称（如A组、B组）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_name` (`class_id`,`name`),
  KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生分组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_groups`
--

LOCK TABLES `student_groups` WRITE;
/*!40000 ALTER TABLE `student_groups` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_remarks`
--

DROP TABLE IF EXISTS `student_remarks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_remarks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `semester` varchar(20) NOT NULL,
  `remark` text,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_semester` (`student_id`,`semester`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班主任寄语';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_remarks`
--

LOCK TABLES `student_remarks` WRITE;
/*!40000 ALTER TABLE `student_remarks` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_stage_change_log`
--

DROP TABLE IF EXISTS `student_stage_change_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_stage_change_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `from_stage_id` bigint DEFAULT NULL,
  `to_stage_id` bigint NOT NULL,
  `from_class_id` bigint DEFAULT NULL,
  `to_class_id` bigint DEFAULT NULL,
  `change_date` date NOT NULL,
  `reason` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_stage_change_log`
--

LOCK TABLES `student_stage_change_log` WRITE;
/*!40000 ALTER TABLE `student_stage_change_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_stage_change_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_timeline`
--

DROP TABLE IF EXISTS `student_timeline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_timeline` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `event_type` varchar(50) NOT NULL,
  `title` varchar(200) DEFAULT NULL,
  `description` text,
  `link` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `visibility` varchar(20) DEFAULT 'PUBLIC',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_timeline_visibility` (`visibility`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生成长足迹';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_timeline`
--

LOCK TABLES `student_timeline` WRITE;
/*!40000 ALTER TABLE `student_timeline` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `student_number` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号',
  `gender` tinyint DEFAULT NULL COMMENT '性别: 1男 2女',
  `birthday` date DEFAULT NULL COMMENT '出生日期',
  `enrollment_year` int DEFAULT NULL,
  `class_id` bigint DEFAULT NULL COMMENT '所属班级ID',
  `total_credits` int DEFAULT '0' COMMENT '当前积分',
  `title_level` int DEFAULT '1' COMMENT '称号等级(1-5)',
  `current_streak` int DEFAULT '0' COMMENT '连续签到天数',
  `custom_title` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `custom_title_set_at` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '状态: active/leave/withdraw/transfer/retain/graduated',
  `school_id` bigint DEFAULT '1',
  `current_stage_id` bigint DEFAULT '4',
  `original_stage_id` bigint DEFAULT '4',
  `stage_id` bigint DEFAULT '4',
  `current_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'VOCATIONAL' COMMENT '当前年级类型编码 → class_type_config.type_code',
  `enrollment_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入学时年级类型编码 → class_type_config.type_code',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `student_number` (`student_number`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_student_number` (`student_number`),
  KEY `idx_class_id` (`class_id`),
  CONSTRAINT `students_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subject_category`
--

DROP TABLE IF EXISTS `subject_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subject_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `level` tinyint NOT NULL DEFAULT '1',
  `stage_id` bigint DEFAULT NULL,
  `school_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subject_category`
--

LOCK TABLES `subject_category` WRITE;
/*!40000 ALTER TABLE `subject_category` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `survey_questions`
--

DROP TABLE IF EXISTS `survey_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `survey_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `question_order` int NOT NULL DEFAULT '0',
  `question_type` varchar(20) NOT NULL COMMENT 'radio/checkbox/textarea',
  `title` varchar(500) NOT NULL,
  `options` json DEFAULT NULL COMMENT '选项数组(radio/checkbox)',
  `required` tinyint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问卷题目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `survey_questions`
--

LOCK TABLES `survey_questions` WRITE;
/*!40000 ALTER TABLE `survey_questions` DISABLE KEYS */;
/*!40000 ALTER TABLE `survey_questions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_configs`
--

DROP TABLE IF EXISTS `system_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `config_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'string' COMMENT '类型: string/number/boolean/json',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  `group_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'general' COMMENT '配置分组',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_key` (`config_key`),
  KEY `idx_group` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_configs`
--

LOCK TABLES `system_configs` WRITE;
/*!40000 ALTER TABLE `system_configs` DISABLE KEYS */;
/*!40000 ALTER TABLE `system_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_settings`
--

DROP TABLE IF EXISTS `system_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `setting_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `setting_value` text COLLATE utf8mb4_unicode_ci,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数用途说明',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'general' COMMENT '分类: exam/homework/credit/bbs/security/system',
  `is_editable` tinyint(1) DEFAULT '1' COMMENT '是否可在后台编辑: 0=只读 1=可编辑',
  `default_value` text COLLATE utf8mb4_unicode_ci,
  `value_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'string' COMMENT '值类型: string/number/boolean/json',
  `options` json DEFAULT NULL COMMENT '可选值(JSON数组), 用于下拉单选等',
  `validation_rule` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '验证规则, 如 regex:/^\\d+$/ 或 range:1~100',
  `order_num` int DEFAULT '0' COMMENT '显示顺序, 越小越靠前',
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `setting_key` (`setting_key`),
  KEY `idx_category` (`category`),
  KEY `idx_order_num` (`order_num`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_settings`
--

LOCK TABLES `system_settings` WRITE;
/*!40000 ALTER TABLE `system_settings` DISABLE KEYS */;
INSERT INTO `system_settings` VALUES (1,'homework.late_hours_limit','24','允许迟交小时数','2026-05-07 18:18:23','task',1,'24','number',NULL,NULL,1,1),(2,'homework.late_penalty','0.8','迟交扣分比例','2026-05-07 18:18:23','task',1,'0.8','number',NULL,NULL,2,1),(3,'exam.max_duration','180','最大考试时长(分钟)','2026-05-07 18:18:23','task',1,'180','number',NULL,NULL,1,1),(4,'exam.min_duration','10','最小时长(分钟)','2026-05-07 18:18:23','task',1,'10','number',NULL,NULL,2,1),(5,'exam.max_cheat_warnings','3','最大切屏警告次数','2026-05-07 18:18:23','task',1,'3','number',NULL,NULL,3,1),(6,'credit.leaderboard_enabled','true','启用排行榜','2026-05-05 15:36:01','credit',1,'true','boolean',NULL,NULL,3,1),(7,'credit.daily_sign_reset_hour','4','签到重置小时','2026-05-05 15:36:01','credit',1,'4','number',NULL,NULL,2,1),(8,'exam.cheat_detection_enabled','true','是否启用防作弊检测','2026-05-07 18:18:23','task',1,'true','boolean',NULL,NULL,5,1),(9,'credit.daily_sign_credits','5','每日签到获得积分数','2026-05-05 15:36:01','credit',1,'5','number',NULL,NULL,1,1),(10,'bbs.allow_anonymous','false','是否允许匿名发帖','2026-05-05 15:36:01','bbs',1,'false','boolean',NULL,NULL,1,1),(11,'bbs.post_credit_reward','5','发帖奖励积分','2026-05-05 15:36:01','bbs',1,'5','number',NULL,NULL,2,1),(12,'bbs.reply_credit_reward','2','回复奖励积分','2026-05-05 15:36:01','bbs',1,'2','number',NULL,NULL,3,1),(13,'exam.auto_grade_enabled','true','是否启用自动评分(客观题)','2026-05-07 18:18:23','task',1,'true','boolean','[\"true\", \"false\"]',NULL,5,1),(14,'homework.max_file_size_mb','10','作业附件最大上传大小(MB)','2026-05-07 18:18:23','task',1,'10','number',NULL,'range:1~100',3,1),(15,'credit.moral_behavior_score','5','德育行为表扬单次积分','2026-05-05 15:34:12','credit',1,'5','number',NULL,'range:1~50',4,1),(16,'bbs.post_audit_enabled','false','发帖是否需要审核','2026-05-05 15:34:12','bbs',1,'false','boolean','[\"true\", \"false\"]',NULL,4,1),(17,'jwt.expiration_hours','24','JWT Token 过期时间(小时)','2026-05-05 15:34:12','security',0,'24','number',NULL,'range:1~720',1,1),(18,'security.password_min_length','6','密码最小长度','2026-05-05 15:34:12','security',0,'6','number',NULL,'range:4~32',2,1),(19,'security.login_fail_lock_count','5','登录失败锁定次数(0=不锁定)','2026-05-05 15:34:12','security',0,'5','number',NULL,'range:0~20',3,1),(20,'security.session_max_concurrent','3','同一账号最大并发登录数','2026-05-05 15:34:12','security',0,'3','number',NULL,'range:1~10',4,1),(21,'system.enable_register','false','是否允许自主注册','2026-05-05 15:34:12','system',1,'false','boolean','[\"true\", \"false\"]',NULL,1,1),(22,'system.site_title','示例县综合高级中学教学管理系统','站点标题','2026-05-05 16:11:03','system',1,'职高计算机教学管理系统','string',NULL,NULL,2,1),(23,'system.page_size_default','20','默认分页大小','2026-05-05 15:34:12','system',1,'20','number',NULL,'range:5~100',3,1),(24,'feature.review_enabled','false','双审功能（考试任务两级审核）','2026-05-14 16:29:00','feature',1,NULL,'boolean',NULL,NULL,1,1),(25,'feature.shop_enabled','false','积分商城兑换功能','2026-05-12 07:15:56','feature',1,NULL,'boolean',NULL,NULL,2,1),(26,'feature.external_review_enabled','false','外部评阅链接生成','2026-05-12 07:15:56','feature',1,NULL,'boolean',NULL,NULL,3,1),(27,'feature.re_review_enabled','false','复议/互评功能','2026-05-12 07:15:56','feature',1,NULL,'boolean',NULL,NULL,4,1),(28,'feature.ai_question_enabled','true','AI题目生成','2026-05-14 16:29:00','feature',1,NULL,'boolean',NULL,NULL,5,1),(29,'feature.ai_grading_enabled','true','AI辅助评分','2026-05-14 16:29:00','feature',1,NULL,'boolean',NULL,NULL,6,1),(30,'feature.template_enabled','false','任务模板功能','2026-05-12 07:15:56','feature',1,NULL,'boolean',NULL,NULL,7,1),(31,'feature.inspector_legacy_enabled','false','巡视员旧端点兼容（已废弃）','2026-05-12 07:15:56','feature',1,NULL,'boolean',NULL,NULL,8,1),(39,'survey.templates','[{\"key\": \"moral_eval\", \"label\": \"u5fb7u80b2u8bc4u4ef7u95eeu5377\", \"questions\": \"[{\"type\":\"radio\",\"title\":\"u53c2u4e0eu5fb7u80b2u6d3bu52a8\",\"options\":[\"u4e3bu9898u73edu4f1a\",\"u5fd7u613fu8005u6d3bu52a8\",\"u793eu533au670du52a1\",\"u8bb2u5ea7\",\"u5176u4ed6\"]},{\"type\":\"radio\",\"title\":\"u5fb7u80b2u81eau8bc4\",\"options\":[\"u4f18u79c0\",\"u826fu597d\",\"u4e00u822c\",\"u5f85u6539u8fdb\"]},{\"type\":\"text\",\"title\":\"u6700u53d7u76cau7684u5fb7u80b2u6d3bu52a8\"}]\"}, {\"key\": \"labor_practice\", \"label\": \"u52b3u52a8u5b9eu8df5u95eeu5377\", \"questions\": \"[{\"type\":\"checkbox\",\"title\":\"u53c2u4e0eu52b3u52a8\",\"options\":[\"u6559u5ba4u503cu65e5\",\"u6821u56edu5927u626bu9664\",\"u5bb6u52a1u52b3u52a8\",\"u793eu533au670du52a1\",\"u5b9eu8bad\"]},{\"type\":\"radio\",\"title\":\"u9891u7387\",\"options\":[\"u6bcfu5929\",\"u6bcfu5468\",\"u5076u5c14\"]},{\"type\":\"text\",\"title\":\"u52b3u52a8u6536u83b7\"}]\"}, {\"key\": \"class_satisfaction\", \"label\": \"u8bfeu5802u6ee1u610fu5ea6\", \"questions\": \"[{\"type\":\"radio\",\"title\":\"u6574u4f53u6ee1u610fu5ea6\",\"options\":[\"u975eu5e38u6ee1u610f\",\"u6ee1u610f\",\"u4e00u822c\",\"u4e0du6ee1u610f\"]},{\"type\":\"radio\",\"title\":\"u96beu6613u5ea6\",\"options\":[\"u504fu96be\",\"u9002u4e2d\",\"u504fu6613\"]},{\"type\":\"radio\",\"title\":\"u6388u8bfeu65b9u5f0fu5339u914d\",\"options\":[\"u5b8cu5168u7b26u5408\",\"u57fau672cu7b26u5408\",\"u4e0du592au7b26u5408\"]},{\"type\":\"text\",\"title\":\"u6539u8fdbu5efau8bae\"}]\"}, {\"key\": \"class_activity\", \"label\": \"u73edu7ea7u6d3bu52a8u8c03u67e5\", \"questions\": \"[{\"type\":\"checkbox\",\"title\":\"u5e0cu671bu7684u6d3bu52a8\",\"options\":[\"u6625u6e38u79cbu6e38\",\"u4f53u80b2u6bd4u8d5b\",\"u6587u827au6c47u6f14\",\"u8bfbu4e66u5206u4eabu4f1a\",\"u804cu4e1au4f53u9a8c\"]},{\"type\":\"radio\",\"title\":\"u8d39u7528u8303u56f4\",\"options\":[\"50u5143u5185\",\"50-100\",\"100-200\",\"200u4ee5u4e0a\"]},{\"type\":\"text\",\"title\":\"u5176u4ed6u5efau8bae\"}]\"}, {\"key\": \"study_habit\", \"label\": \"u5b66u4e60u4e60u60efu81eau8bc4\", \"questions\": \"[{\"type\":\"radio\",\"title\":\"u8bfeu540eu5b66u4e60u65f6u957f\",\"options\":[\"u4e0du8db330u5206u949f\",\"30-60u5206u949f\",\"1-2u5c0fu65f6\",\"2u5c0fu65f6u4ee5u4e0a\"]},{\"type\":\"radio\",\"title\":\"u96beu9898u5904u7406\",\"options\":[\"u81eau5df1u67e5u8d44u6599\",\"u95eeu540cu5b66\",\"u95eeu8001u5e08\",\"u653eu5f03\"]},{\"type\":\"radio\",\"title\":\"u7b14u8bb0u4e60u60ef\",\"options\":[\"u8be6u7ec6\",\"u7b80u5355\",\"u5076u5c14\",\"u4eceu4e0d\"]},{\"type\":\"radio\",\"title\":\"u81eau8bc4\",\"options\":[\"u5f88u597d\",\"u8f83u597d\",\"u4e00u822c\",\"u5f85u6539u8fdb\"]}]\"}]','预置问卷模板库','2026-05-12 23:13:37','general',1,NULL,'json',NULL,NULL,0,1),(40,'ai.provider','deepseek',NULL,'2026-05-13 00:22:17','general',1,NULL,'string',NULL,NULL,0,1),(41,'ai.openai.base-url','https://api.openai.com/v1',NULL,'2026-05-13 00:18:31','general',1,NULL,'string',NULL,NULL,0,1),(42,'ai.openai.model','gpt-4o-mini',NULL,'2026-05-13 00:18:31','general',1,NULL,'string',NULL,NULL,0,1),(43,'ai.openai.api-key','nFsy7pN/4Cbq9BrQ2e1ZwQnjMuurPb2ZhR/OkMcqW2rWQAcUrMsCx9AO3+y9B2bW',NULL,'2026-05-13 00:18:31','general',1,NULL,'string',NULL,NULL,0,1),(44,'ai.deepseek.api-key','your-deepseek-api-key',NULL,'2026-05-13 00:22:17','general',1,NULL,'string',NULL,NULL,0,1),(45,'feature.credit_enabled','true','积分中心功能开关','2026-05-13 13:41:16','feature',1,'true','boolean',NULL,NULL,9,1),(46,'updates','[{id=feature.credit_enabled, newValue=false}]',NULL,'2026-05-13 12:49:15','general',1,NULL,'string',NULL,NULL,0,1),(47,'typing_allowed_majors','[1]',NULL,'2026-05-13 14:51:07','feature',1,NULL,'string',NULL,NULL,0,1),(48,'ai.deepseek.timeout-seconds','60',NULL,'2026-05-14 10:44:44','general',1,NULL,'string',NULL,NULL,0,1),(49,'ai.deepseek.model','deepseek-chat',NULL,'2026-05-14 10:44:44','general',1,NULL,'string',NULL,NULL,0,1),(50,'ai.deepseek.base-url','https://api.deepseek.com/v1',NULL,'2026-05-14 10:44:44','general',1,NULL,'string',NULL,NULL,0,1),(54,'feature.ai_content_enabled','true','AI 教学助手功能开关（知识库驱动的教学设计/知识清单/实训方案/练习生成）','2026-05-20 22:34:39','AI',1,'false','boolean',NULL,NULL,0,1),(55,'ai.teacher.daily_quota','30','教师每日AI内容生成次数上限(0=不限制)','2026-05-21 21:20:06','ai',1,'30','number',NULL,NULL,0,1),(56,'ai.student.daily_quota','6','学生每日衍生练习次数上限(0=不限制)','2026-05-21 21:20:06','ai',1,'6','number',NULL,NULL,0,1),(57,'feature.syllabus_node_mapping_enabled','true','考纲精确关联知识点(开启后AI按节点注射考纲,关闭后全学科注入)','2026-05-21 21:49:13','ai',1,'false','boolean',NULL,NULL,0,1),(58,'feature.security_filter_enabled','true','AI安全过滤开关(输入prompt注入检测+输出敏感词过滤)','2026-05-21 21:55:18','ai',1,'true','boolean',NULL,NULL,0,1),(59,'feature.sse_enabled','true','SSE实时推送(替代轮询,灰度测试)','2026-05-21 22:09:59','ai',1,'false','boolean',NULL,NULL,0,1);
/*!40000 ALTER TABLE `system_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_group_visibility`
--

DROP TABLE IF EXISTS `task_group_visibility`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_group_visibility` (
  `task_id` bigint NOT NULL COMMENT '任务ID → tasks.id',
  `group_id` bigint NOT NULL COMMENT '分组ID → student_groups.id',
  PRIMARY KEY (`task_id`,`group_id`),
  KEY `idx_tgv_task_id` (`task_id`),
  KEY `idx_tgv_group_id` (`group_id`),
  CONSTRAINT `fk_tgv_group` FOREIGN KEY (`group_id`) REFERENCES `student_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tgv_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务分组可见性关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_group_visibility`
--

LOCK TABLES `task_group_visibility` WRITE;
/*!40000 ALTER TABLE `task_group_visibility` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_group_visibility` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_questions`
--

DROP TABLE IF EXISTS `task_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `question_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `score` decimal(6,1) DEFAULT '1.0',
  `school_id` bigint DEFAULT '1',
  `stage_id` bigint DEFAULT '4',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_questions`
--

LOCK TABLES `task_questions` WRITE;
/*!40000 ALTER TABLE `task_questions` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_questions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_submissions`
--

DROP TABLE IF EXISTS `task_submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `school_id` bigint DEFAULT '1',
  `stage_id` bigint DEFAULT '4',
  `content` text COLLATE utf8mb4_unicode_ci,
  `attachments` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `score` decimal(6,1) DEFAULT NULL,
  `grade_level` varchar(5) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `score_json` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `graded_by` bigint DEFAULT NULL,
  `grade_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resubmission_of` bigint DEFAULT NULL,
  `include_in_portfolio` tinyint DEFAULT '0',
  `peer_score` decimal(10,2) DEFAULT NULL,
  `is_exemplar` tinyint(1) DEFAULT '0',
  `submitted_at` datetime DEFAULT NULL,
  `graded_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `cheat_warnings` int NOT NULL DEFAULT '0' COMMENT '切屏警告次数',
  `cheat_terminated` tinyint NOT NULL DEFAULT '0' COMMENT '是否因切屏被终止',
  `extra_submit_allowed` tinyint DEFAULT '0' COMMENT '教师特许补交',
  PRIMARY KEY (`id`),
  KEY `idx_student_task` (`student_id`,`task_id`),
  KEY `idx_task_status` (`task_id`,`status`),
  KEY `idx_student_status` (`student_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_submissions`
--

LOCK TABLES `task_submissions` WRITE;
/*!40000 ALTER TABLE `task_submissions` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_templates`
--

DROP TABLE IF EXISTS `task_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '模板名称',
  `description` text COMMENT '任务描述',
  `subject` varchar(100) DEFAULT NULL COMMENT '学科',
  `task_type` varchar(30) NOT NULL COMMENT '任务类型',
  `score_type` varchar(30) DEFAULT 'POINT_100' COMMENT '评分体系',
  `category` varchar(30) DEFAULT 'TEACHING' COMMENT '用途分类:TEACHING/CLASS_MGMT/SCHOOL_NOTICE',
  `question_ids` json DEFAULT NULL COMMENT '题目ID列表',
  `task_config` json DEFAULT NULL COMMENT '任务配置(考试设置等)',
  `wuyu_tag` varchar(50) DEFAULT NULL COMMENT '五育标签',
  `total_score` decimal(6,1) DEFAULT '100.0' COMMENT '总分',
  `scope` varchar(20) DEFAULT 'PRIVATE' COMMENT '共享范围: PRIVATE/LESSON_PREP/TEACHING_GROUP',
  `teaching_group_id` bigint DEFAULT NULL COMMENT '教研组ID(共享时关联)',
  `lesson_prep_group_id` bigint DEFAULT NULL COMMENT '备课组ID(共享时关联)',
  `use_count` int DEFAULT '0' COMMENT '使用次数',
  `created_by` bigint DEFAULT NULL COMMENT '创建者userId',
  `school_id` bigint DEFAULT NULL,
  `stage_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_templates`
--

LOCK TABLES `task_templates` WRITE;
/*!40000 ALTER TABLE `task_templates` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL DEFAULT '1',
  `stage_id` bigint NOT NULL DEFAULT '4',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `subject` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `grade_id` bigint DEFAULT NULL,
  `teacher_id` bigint NOT NULL,
  `total_score` decimal(6,1) DEFAULT '100.0',
  `score_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'POINT_100',
  `passing_score` decimal(6,1) DEFAULT NULL,
  `task_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'AFTER_CLASS',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'CLASS',
  `target_id` bigint DEFAULT NULL,
  `view_scope` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'CLASS',
  `deadline` datetime DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT',
  `task_config` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notify_parents` tinyint DEFAULT '0',
  `allow_resubmit` tinyint DEFAULT '0',
  `is_required` tinyint DEFAULT '1',
  `wuyu_tag` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_competition_mode` tinyint(1) DEFAULT '0',
  `term_id` bigint DEFAULT NULL,
  `rubric_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `review_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'NOT_SUBMITTED' COMMENT '审核状态',
  `auto_wrongbook` tinyint DEFAULT '1' COMMENT '是否自动收录错题到错题本 1=是 0=否',
  `is_forced` tinyint DEFAULT '0' COMMENT '是否全校强制',
  `is_anonymous` tinyint(1) DEFAULT '0' COMMENT '匿名作答：1=匿名，0=实名',
  `survey_schema` json DEFAULT NULL COMMENT '问卷题目结构',
  `allow_custom_steps` tinyint DEFAULT '1' COMMENT '是否允许学生自定义步骤: 0=否 1=是',
  `reference_images` json DEFAULT NULL COMMENT '参考图片URL数组，如 ["url1","url2"]',
  `scheduled_publish_at` datetime DEFAULT NULL COMMENT '定时发布时间(NULL=立即发布)',
  `source_task_id` bigint DEFAULT NULL COMMENT '复制来源任务ID → tasks.id',
  `difficulty_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '难度等级: EASY/MEDIUM/HARD',
  PRIMARY KEY (`id`),
  KEY `idx_type_status_deadline` (`task_type`,`status`,`deadline`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_scheduled_publish` (`status`,`scheduled_publish_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tasks`
--

LOCK TABLES `tasks` WRITE;
/*!40000 ALTER TABLE `tasks` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_classes`
--

DROP TABLE IF EXISTS `teacher_classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师记录ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `subject` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任教科目',
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_class` (`class_id`),
  CONSTRAINT `teacher_classes_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `teacher_classes_ibfk_2` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师任课关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_classes`
--

LOCK TABLES `teacher_classes` WRITE;
/*!40000 ALTER TABLE `teacher_classes` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_quick_comments`
--

DROP TABLE IF EXISTS `teacher_quick_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_quick_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师ID→teachers.id',
  `comment_text` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '快捷评语内容',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师快捷评语';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_quick_comments`
--

LOCK TABLES `teacher_quick_comments` WRITE;
/*!40000 ALTER TABLE `teacher_quick_comments` DISABLE KEYS */;
/*!40000 ALTER TABLE `teacher_quick_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teachers`
--

DROP TABLE IF EXISTS `teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teachers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `teacher_number` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工号',
  `gender` tinyint DEFAULT NULL COMMENT '性别: 1男 2女',
  `subject` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主要任教学科',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `teacher_number` (`teacher_number`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_teacher_number` (`teacher_number`),
  CONSTRAINT `teachers_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teachers`
--

LOCK TABLES `teachers` WRITE;
/*!40000 ALTER TABLE `teachers` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaching_group`
--

DROP TABLE IF EXISTS `teaching_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教研组名称',
  `subject_ids` json DEFAULT NULL COMMENT '多学科ID数组 → dict_subject.id',
  `school_id` bigint DEFAULT '0' COMMENT '学校ID',
  `stage_ids` json DEFAULT NULL COMMENT '学段ID列表（多选）',
  `leader_ids` json DEFAULT NULL COMMENT '组长ID列表（最多2人）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_school` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教研组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaching_group`
--

LOCK TABLES `teaching_group` WRITE;
/*!40000 ALTER TABLE `teaching_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `teaching_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `title_levels`
--

DROP TABLE IF EXISTS `title_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `title_levels` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `level_number` int NOT NULL COMMENT '等级数字',
  `level_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级名称',
  `min_credits` int NOT NULL COMMENT '最低积分要求',
  `max_credits` int DEFAULT NULL COMMENT '最高积分要求(null表示无上限)',
  `badge_icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '徽章图标',
  `privileges` text COLLATE utf8mb4_unicode_ci COMMENT '特权说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `level_number` (`level_number`),
  KEY `idx_level` (`level_number`),
  KEY `idx_credits` (`min_credits`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='称号等级表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `title_levels`
--

LOCK TABLES `title_levels` WRITE;
/*!40000 ALTER TABLE `title_levels` DISABLE KEYS */;
/*!40000 ALTER TABLE `title_levels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `typing_competition_results`
--

DROP TABLE IF EXISTS `typing_competition_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `typing_competition_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL COMMENT '竞赛ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `total_chars` int DEFAULT '0' COMMENT '总字符数',
  `correct_chars` int DEFAULT '0' COMMENT '正确字符数',
  `wrong_chars` int DEFAULT '0' COMMENT '错误字符数',
  `backspace_count` int DEFAULT '0' COMMENT '退格次数',
  `duration_seconds` int DEFAULT '0' COMMENT '用时（秒）',
  `speed_wpm` decimal(5,2) DEFAULT NULL COMMENT '速度（字/分钟）',
  `accuracy` decimal(5,2) DEFAULT NULL COMMENT '正确率 0-100',
  `error_details` json DEFAULT NULL COMMENT '错误明细 [{char,expected,typed,position}]',
  `score` decimal(8,2) DEFAULT NULL COMMENT '综合得分',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `keystroke_data` longtext COMMENT '击键回放数据JSON',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comp_student` (`competition_id`,`student_id`),
  KEY `idx_competition` (`competition_id`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打字竞赛成绩';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `typing_competition_results`
--

LOCK TABLES `typing_competition_results` WRITE;
/*!40000 ALTER TABLE `typing_competition_results` DISABLE KEYS */;
/*!40000 ALTER TABLE `typing_competition_results` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `typing_competitions`
--

DROP TABLE IF EXISTS `typing_competitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `typing_competitions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '竞赛标题',
  `text_id` bigint NOT NULL COMMENT '关联打字文本ID',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `allowed_class_ids` json DEFAULT NULL COMMENT '允许参赛的班级ID数组',
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending/ongoing/finished',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_text_id` (`text_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打字竞赛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `typing_competitions`
--

LOCK TABLES `typing_competitions` WRITE;
/*!40000 ALTER TABLE `typing_competitions` DISABLE KEYS */;
/*!40000 ALTER TABLE `typing_competitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `typing_levels`
--

DROP TABLE IF EXISTS `typing_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `typing_levels` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `level_id` int DEFAULT '1' COMMENT '当前等级',
  `exp` int DEFAULT '0' COMMENT '当前经验值',
  `unlocked_maps` json DEFAULT NULL COMMENT '解锁的地图ID数组',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打字游戏化等级';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `typing_levels`
--

LOCK TABLES `typing_levels` WRITE;
/*!40000 ALTER TABLE `typing_levels` DISABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `typing_records`
--

DROP TABLE IF EXISTS `typing_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `typing_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `text_id` bigint NOT NULL COMMENT '打字文本ID',
  `mode` varchar(20) DEFAULT 'practice' COMMENT 'practice/competition',
  `total_chars` int DEFAULT '0',
  `correct_chars` int DEFAULT '0',
  `wrong_chars` int DEFAULT '0',
  `backspace_count` int DEFAULT '0',
  `duration_seconds` int DEFAULT '0',
  `speed_wpm` decimal(5,2) DEFAULT NULL,
  `accuracy` decimal(5,2) DEFAULT NULL,
  `error_details` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_time` (`student_id`,`created_at` DESC),
  KEY `idx_text_id` (`text_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打字练习记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `typing_records`
--

LOCK TABLES `typing_records` WRITE;
/*!40000 ALTER TABLE `typing_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `typing_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `typing_texts`
--

DROP TABLE IF EXISTS `typing_texts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `typing_texts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '文本标题',
  `content` text NOT NULL COMMENT '原文（支持中英文混合）',
  `language` varchar(20) DEFAULT 'mixed' COMMENT '语言类型: en/zh/mixed',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度 1-5',
  `category` varchar(50) DEFAULT NULL COMMENT '素材分类',
  `type` varchar(20) DEFAULT 'practice' COMMENT 'practice/competition',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打字文本库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `typing_texts`
--

LOCK TABLES `typing_texts` WRITE;
/*!40000 ALTER TABLE `typing_texts` DISABLE KEYS */;
/*!40000 ALTER TABLE `typing_texts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码(加密存储)',
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '真实姓名',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `role_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'STUDENT',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  `current_stage_id` bigint DEFAULT '4',
  `external_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `external_source` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stage_id` bigint DEFAULT '4',
  `must_change_password` tinyint DEFAULT '0' COMMENT '强制修改密码标记: 0=否 1=是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_status` (`status`),
  KEY `idx_role_name` (`role_name`),
  KEY `idx_last_login_ip` (`last_login_ip`)
) ENGINE=InnoDB AUTO_INCREMENT=1011 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

-- ⚠️ 内置管理员账号 admin — 密码已自定义（非 admin123）
--    - id=1 是系统初始管理员，很多维护操作依赖此账号
--    - 密码通过 /api/profile/password 自行修改，或直接操作数据库
--    - 如修改密码，必须同时更新此 SQL 文件 + CLAUDE.md + fix_admin_pwd.sql 中的密码记录
--    - 生成时间: 2026-06-07 · 密码: BrightDawn492#
LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
-- ========================================
-- 种子数据（演示账号，首次部署后请修改密码）
-- ========================================
-- 管理员账号

--
-- Table structure for table `wrong_questions`
--

DROP TABLE IF EXISTS `wrong_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrong_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `wrong_count` int DEFAULT '1' COMMENT '错误次数',
  `last_wrong_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最近错误时间',
  `is_mastered` tinyint DEFAULT '0' COMMENT '已掌握: 0否 1是',
  `source_type` varchar(30) DEFAULT NULL COMMENT '来源类型',
  `source_task_id` bigint DEFAULT NULL COMMENT '来源任务ID',
  `last_practice_time` datetime DEFAULT NULL COMMENT '最近练习时间',
  `practice_count` int DEFAULT '0' COMMENT '练习次数',
  `mastered_at` datetime DEFAULT NULL COMMENT '掌握时间',
  `mastered_source` varchar(20) DEFAULT NULL COMMENT '掌握来源',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_question` (`student_id`,`question_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_student_question` (`student_id`,`question_id`),
  CONSTRAINT `wrong_questions_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `wrong_questions_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question_bank` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题本';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrong_questions`
--

LOCK TABLES `wrong_questions` WRITE;
/*!40000 ALTER TABLE `wrong_questions` DISABLE KEYS */;
/*!40000 ALTER TABLE `wrong_questions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'teaching_system'
--

--
-- Dumping routines for database 'teaching_system'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


--
-- Create application user
--
CREATE USER IF NOT EXISTS 'teaching_app'@'%' IDENTIFIED BY 'kUwb6in6sOvW23mXY1Irk6qUngh9vPZ8';
GRANT SELECT, INSERT, UPDATE, DELETE ON teaching_system.* TO 'teaching_app'@'%';
FLUSH PRIVILEGES;


-- Dump completed on 2026-05-23  6:21:10

-- ============================================================================
-- 增量迁移 v2~v82
-- ============================================================================

-- ---------- v2_knowledge_nodes.sql ----------
-- ============================================================================
-- v2: 知识库节点 + AI产出表
-- 替换旧的 question_categories 和 ai_generated_content
-- ============================================================================

DROP TABLE IF EXISTS `ai_generated_content`;
DROP TABLE IF EXISTS `question_categories`;

-- ----------------------------------------------------------------------------
-- knowledge_nodes: 知识库节点（学科→章节→任务→知识点 四级树）
-- ----------------------------------------------------------------------------
CREATE TABLE `knowledge_nodes` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`   BIGINT       DEFAULT NULL COMMENT '父节点ID → knowledge_nodes.id',
  `subject_id`  BIGINT       DEFAULT NULL COMMENT '所属学科ID → dict_subject.id',
  `level`       INT          NOT NULL DEFAULT 2 COMMENT '层级: 1=学科, 2=章节, 3=任务, 4=知识点',
  `name`        VARCHAR(200) NOT NULL COMMENT '节点名称',
  `content`     MEDIUMTEXT   DEFAULT NULL COMMENT 'Markdown知识库内容',
  `sort_order`  INT          DEFAULT 0 COMMENT '排序',
  `version`     INT          DEFAULT 1 COMMENT '乐观锁版本号',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_parent_id`  (`parent_id`),
  INDEX `idx_subject_id` (`subject_id`),
  INDEX `idx_level`      (`level`),
  CONSTRAINT `fk_kn_parent` FOREIGN KEY (`parent_id`) REFERENCES `knowledge_nodes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库节点表';

-- ----------------------------------------------------------------------------
-- ai_outputs: AI教学产出记录
-- ----------------------------------------------------------------------------
CREATE TABLE `ai_outputs` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `node_id`     BIGINT       NOT NULL COMMENT '关联知识节点ID → knowledge_nodes.id',
  `teacher_id`  BIGINT       NOT NULL COMMENT '教师用户ID',
  `output_type` VARCHAR(30)  NOT NULL COMMENT '产出类型（如 lesson_plan/quiz/slide/summary 等）',
  `content`     LONGTEXT     DEFAULT NULL COMMENT 'AI生成的完整内容',
  `is_latest`   TINYINT(1)   DEFAULT 1 COMMENT '是否最新版本（1=最新, 0=历史）',
  `version_seq` INT          DEFAULT 1 COMMENT '版本序号（1=最新，同node+type内递增，>5自动清理）',
  `status`      TINYINT      DEFAULT 0 COMMENT '状态: 0=草稿, 1=已发布, 2=已归档',
  `tokens_used` INT          DEFAULT 0 COMMENT '消耗token数',
  `latency_ms`  INT          DEFAULT 0 COMMENT '响应延迟（毫秒）',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_node_type_latest` (`node_id`, `output_type`, `is_latest`),
  INDEX `idx_teacher_id`       (`teacher_id`),
  -- FK fk_ao_node removed (v147): node_id reused for taskId in DIAGNOSIS/CONSOLIDATION_MATERIAL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI教学产出表';

-- ----------------------------------------------------------------------------
-- 种子数据: 从 dict_subject 表拉取 level=1 学科节点
-- ----------------------------------------------------------------------------

-- ----------------------------------------------------------------------------
-- 清理旧引用: question_bank.category_id 原本指向已删除的 question_categories 表
-- ----------------------------------------------------------------------------
UPDATE `question_bank` SET `category_id` = NULL;

-- ----------------------------------------------------------------------------
-- 完成标记
-- ----------------------------------------------------------------------------
SELECT 'knowledge_nodes和ai_outputs表创建完成' AS result;

-- ---------- v13_classroom_question_fields.sql ----------
-- v13: question_bank 新增 intent/category 字段（课堂提问意图+认知层次）
ALTER TABLE question_bank ADD COLUMN intent VARCHAR(200) DEFAULT NULL COMMENT '课堂提问意图';
ALTER TABLE question_bank ADD COLUMN category VARCHAR(50) DEFAULT NULL COMMENT '认知层次 RECALL/COMPREHEND/APPLY/EXTEND';

-- ---------- v13a_classroom_question_sync.sql ----------
-- v13a: classroom_questions 新增 synced_question_bank_id 字段
-- 课堂手动创建的题目，其 ID 不在 question_bank 中，但 wrong_questions.question_id 有外键约束 REFERENCES question_bank(id)
-- 因此在答错收录时，需将 classroom_question 同步到 question_bank，并记录对应 ID
ALTER TABLE classroom_questions ADD COLUMN synced_question_bank_id BIGINT DEFAULT NULL COMMENT '同步到 question_bank 后的对应 ID';

-- ---------- v15_reflection.sql ----------
-- v15: task_submissions 新增 reflection 字段，学习反思持久化
ALTER TABLE `task_submissions`
  ADD COLUMN `reflection` TEXT DEFAULT NULL COMMENT '学生学习反思' AFTER `extra_submit_allowed`;

-- ---------- v16_absent_students.sql ----------
-- v16: 新增 classroom_absent_students 表，缺席管理持久化
CREATE TABLE IF NOT EXISTS `classroom_absent_students` (
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `session_id` BIGINT DEFAULT NULL COMMENT '标记时的课堂会话ID',
  `marked_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '标记时间',
  PRIMARY KEY (`class_id`, `student_id`),
  KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂缺席学生表';

-- ---------- v17_wrong_source.sql ----------
-- v17: wrong_questions 新增 source_type, source_task_id 用于展示错题来源
ALTER TABLE `wrong_questions`
  ADD COLUMN `source_type` VARCHAR(30) DEFAULT NULL COMMENT '来源类型: EXAM/HOMEWORK/QUIZ/BUZZ/PRACTICE' AFTER `is_mastered`,
  ADD COLUMN `source_task_id` BIGINT DEFAULT NULL COMMENT '来源任务ID(考试/作业ID)' AFTER `source_type`;

-- ---------- v18_credit_biz_key.sql ----------
-- v18: credit_transactions 新增 biz_key 防重业务键
ALTER TABLE `credit_transactions`
  ADD COLUMN `biz_key` VARCHAR(100) DEFAULT NULL COMMENT '业务唯一键(如 TASK_GRADED:1:5)' AFTER `description`;

ALTER TABLE `credit_transactions`
  ADD UNIQUE INDEX `uk_biz_key` (`biz_key`);

-- ---------- v19_classroom_ai_fields.sql ----------
-- v19: AI教学助手→大屏数据管道 — classroom_questions新增AI字段
-- 日期: 2026-05-24
-- 说明: 为 classroom_questions 表新增 questionType/fromAi/intent/aiCategory 字段，
--       支持AI生成的课堂提问自动推送到大屏的"AI推荐"Tab

ALTER TABLE `classroom_questions`
    ADD COLUMN `question_type` VARCHAR(50) DEFAULT NULL COMMENT '题目类型(SHORT_ANSWER/TRUE_FALSE等)',
    ADD COLUMN `from_ai` TINYINT(1) DEFAULT 0 COMMENT 'AI教学助手推送标记(0=非AI, 1=AI生成)',
    ADD COLUMN `intent` VARCHAR(200) DEFAULT NULL COMMENT 'AI题目意图(如"检查IP概念理解")',
    ADD COLUMN `ai_category` VARCHAR(50) DEFAULT NULL COMMENT 'AI题目分类(RECALL/COMPREHEND/APPLY/EXTEND)';

-- 为 source 字段添加索引（支持 fromAi 筛选性能）
ALTER TABLE `classroom_questions`
    ADD INDEX `idx_from_ai` (`from_ai`),
    ADD INDEX `idx_source` (`source`);

-- ---------- v61_drop_backup_tables.sql ----------
-- v61: 删除全部旧备份表（exam + homework 备份，已由统一任务系统取代）
-- 执行前请确保全库已备份！mysqldump -u root -p teaching_system > /tmp/teaching_system_$(date +%F).sql
-- ============================================================

-- 第0步：查看当前数据量
SELECT 'backup_exams' AS tbl, COUNT(*) AS cnt FROM backup_exams
UNION ALL SELECT 'backup_exam_questions', COUNT(*) FROM backup_exam_questions
UNION ALL SELECT 'backup_exam_results', COUNT(*) FROM backup_exam_results
UNION ALL SELECT 'backup_homework_assignments', COUNT(*) FROM backup_homework_assignments
UNION ALL SELECT 'backup_homework_submissions', COUNT(*) FROM backup_homework_submissions;

-- 第1步：解除 late_submit_requests 的外键约束（引用了 backup_homework_submissions）
ALTER TABLE late_submit_requests DROP FOREIGN KEY late_submit_requests_ibfk_1;

-- 第2步：删除 5 张备份表（子表先于父表）
DROP TABLE IF EXISTS backup_homework_submissions;
DROP TABLE IF EXISTS backup_homework_assignments;
DROP TABLE IF EXISTS backup_exam_results;
DROP TABLE IF EXISTS backup_exam_questions;
DROP TABLE IF EXISTS backup_exams;

-- 第3步：验证删除
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'teaching_system' AND TABLE_NAME LIKE 'backup_%';
-- 期望：空结果

-- ---------- v62_dict_grade_stage_id.sql ----------
-- v62: dict_grade 新增 stage_id 列，支持按学段筛选年级
-- 年级→学段映射：小学=1, 初中=2, 普高=3, 职高=4
ALTER TABLE dict_grade ADD COLUMN stage_id BIGINT NULL COMMENT '学段ID(关联edu_stage_config.id)';

-- 种子更新：常见年级按学段归类（如已存在请按实际调整）
-- 小学段
UPDATE dict_grade SET stage_id = 1 WHERE grade_name IN ('一年级','二年级','三年级','四年级','五年级','六年级');
-- 初中段
UPDATE dict_grade SET stage_id = 2 WHERE grade_name IN ('七年级','八年级','九年级','初一','初二','初三');
-- 普高段
UPDATE dict_grade SET stage_id = 3 WHERE grade_name IN ('高一','高二','高三','高中一年级','高中二年级','高中三年级');
-- 职高段
UPDATE dict_grade SET stage_id = 4 WHERE grade_name IN ('职高一','职高二','职高三','中职一年级','中职二年级','中职三年级');

-- ---------- v63_auto_score_column_for_practice_session_items.sql ----------
-- 衍生练习：为 practice_session_items 表补充 auto_score 列
-- 用于记录每题自动评分的得分

ALTER TABLE practice_session_items
  ADD COLUMN auto_score decimal(10,1) DEFAULT NULL COMMENT '自动评分得分' AFTER is_correct;

-- ---------- v64_add_is_anonymous_to_tasks.sql ----------
-- 问卷任务：新增 is_anonymous 字段
-- 开启后学生提交问卷时不记录 studentId，实现匿名作答

ALTER TABLE tasks
  ADD COLUMN is_anonymous tinyint(1) DEFAULT 0 COMMENT '匿名作答：1=匿名，0=实名' AFTER is_forced;

-- ---------- v65_typing_plan_c.sql ----------
-- v65: 打字模块 Plan C — 素材分类 + 多班级竞赛列重命名 + 竞赛回放
-- Applied: 2026-05-18

-- 1. 打字文本：新增素材分类字段
ALTER TABLE typing_texts ADD COLUMN category VARCHAR(50) DEFAULT NULL COMMENT '素材分类' AFTER difficulty;
CREATE INDEX idx_category ON typing_texts(category);

-- 2. 竞赛表：allowed_major_ids 始终存储班级ID，修正列名语义
ALTER TABLE typing_competitions
  CHANGE COLUMN allowed_major_ids allowed_class_ids JSON COMMENT '允许参赛的班级ID数组';

-- 3. 竞赛结果：新增击键回放数据列
ALTER TABLE typing_competition_results
  ADD COLUMN keystroke_data LONGTEXT DEFAULT NULL COMMENT '击键回放数据JSON';

-- 4. 种子数据：给已有文本分配默认分类
UPDATE typing_texts SET category = '入门' WHERE difficulty = 1;
UPDATE typing_texts SET category = '进阶' WHERE difficulty >= 2;

-- ============================================================
-- 回滚脚本（参考，不执行）:
-- ALTER TABLE typing_texts DROP COLUMN category, DROP INDEX idx_category;
-- ALTER TABLE typing_competitions CHANGE COLUMN allowed_class_ids allowed_major_ids JSON COMMENT '允许参赛的专业ID数组';
-- ALTER TABLE typing_competition_results DROP COLUMN keystroke_data;
-- ============================================================

-- ---------- v66_class_album_review.sql ----------
-- v66: 班级相册审核 — 学生上传需教师审核
-- Applied: 2026-05-19

ALTER TABLE class_album
  ADD COLUMN status VARCHAR(20) DEFAULT 'APPROVED' COMMENT '审核状态: PENDING/APPROVED/REJECTED' AFTER like_count,
  ADD COLUMN reviewer_id BIGINT DEFAULT NULL COMMENT '审核人用户ID' AFTER status,
  ADD COLUMN reviewed_at DATETIME DEFAULT NULL COMMENT '审核时间' AFTER reviewer_id;

CREATE INDEX idx_status ON class_album(status);
CREATE INDEX idx_class_status ON class_album(class_id, status);

-- ---------- v83_class_album_likes.sql ----------
-- v83: 班级相册点赞去重 — class_album_likes 表防止无限点赞
-- Applied: 2026-06-05

CREATE TABLE IF NOT EXISTS class_album_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    photo_id BIGINT NOT NULL COMMENT '照片ID class_album.id',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID users.id',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_photo_user (photo_id, user_id),
    INDEX idx_photo (photo_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级相册点赞记录';

-- ---------- v68_ai_generated_content.sql ----------
-- v68: AI generated teaching content (non-question types)
-- Stores: 教学设计, 知识清单, 实训方案
-- Question types (同步练习, 综合练习, 课堂提问) continue to use question_bank (status=0 AI_DRAFT)

CREATE TABLE IF NOT EXISTS ai_generated_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT DEFAULT 1,
    teacher_id BIGINT NOT NULL COMMENT '生成者 users.id',
    content_type VARCHAR(30) NOT NULL COMMENT 'TEACHING_DESIGN/KNOWLEDGE_CHECKLIST/PRACTICE_PLAN',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content LONGTEXT COMMENT 'Markdown 正文',
    subject VARCHAR(100) COMMENT '学科名称',
    category_id BIGINT COMMENT '关联知识点 question_categories.id',
    category_path VARCHAR(500) COMMENT '学科→章节→知识点完整路径',
    knowledge_point VARCHAR(200) COMMENT '知识点名称',
    params_json JSON COMMENT '生成参数快照(供复现)',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开（同教研组/备课组可见）',
    status INT DEFAULT 0 COMMENT '0=DRAFT 1=PUBLISHED 2=ARCHIVED',
    tokens_used INT DEFAULT 0 COMMENT '消耗的 tokens 数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_teacher (teacher_id),
    KEY idx_type (content_type),
    KEY idx_category (category_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI生成的非题目类教学内容';

-- Feature flag: AI 教学助手入口
INSERT IGNORE INTO system_settings (setting_key, setting_value, default_value, value_type, category, description, is_editable)
VALUES ('feature.ai_content_enabled', 'false', 'false', 'boolean', 'AI', 'AI 教学助手功能开关（知识库驱动的教学设计/知识清单/实训方案/练习生成）', 1);

-- ---------- v69_parent_feedback_feature_flag.sql ----------
-- v69: 家长反馈汇总功能开关
-- 默认关闭，第四期真数据上线后开启

INSERT IGNORE INTO system_settings (setting_key, setting_value, value_type, category, is_editable, description, order_num)
VALUES ('feature.parent_feedback_enabled', 'false', 'boolean', 'feature', 1, '家长反馈汇总功能开关', 9);

-- ---------- v70_drop_parent_student_bind.sql ----------
-- v70: 清理废弃的 parent_student_bind 表
-- parent_child_relations (v55) 已替代其功能

-- 迁移 parent_student_bind 中尚不存在于 parent_child_relations 的记录
-- parent_student_bind.student_user_id → students.user_id → students.id → parent_child_relations.student_id
INSERT IGNORE INTO parent_child_relations (parent_id, student_id, relation, create_time, update_time)
SELECT psb.parent_user_id, s.id,
       COALESCE(psb.relation, 'GUARDIAN'),
       COALESCE(psb.created_at, NOW()),
       COALESCE(psb.created_at, NOW())
FROM parent_student_bind psb
JOIN students s ON s.user_id = psb.student_user_id;

-- 删除旧表
DROP TABLE IF EXISTS parent_student_bind;

-- ---------- v73_practice_simple_mode.sql ----------
ALTER TABLE practice_plans ADD COLUMN simple_mode TINYINT(1) DEFAULT 0 COMMENT '简易模式：1=单步骤快速提交';

-- ---------- v74_practice_plan_share.sql ----------
ALTER TABLE practice_plans ADD COLUMN shared TINYINT(1) DEFAULT 0 COMMENT '是否共享到学科库';
ALTER TABLE practice_plans ADD COLUMN subject VARCHAR(50) DEFAULT NULL COMMENT '所属学科（用于共享库过滤）';

-- ---------- v75_culture_subject_knowledge_nodes.sql ----------
-- ============================================================================
-- v75: 语文/英语文化课知识节点框架
-- 普高→全国考纲结构 / 职高→四川省对口升学考纲结构
-- 仅创建level2(章节)+level3(任务)框架，知识点留待教师填充
-- 幂等：使用 INSERT IGNORE 防重复
-- ============================================================================

SET @yy_pg = (SELECT id FROM dict_subject WHERE subject_name = '语文[普高]' AND status = 1 LIMIT 1);
SET @yy_zg = (SELECT id FROM dict_subject WHERE subject_name = '语文[职高]' AND status = 1 LIMIT 1);
SET @en_pg = (SELECT id FROM dict_subject WHERE subject_name = '英语[普高]' AND status = 1 LIMIT 1);
SET @en_zg = (SELECT id FROM dict_subject WHERE subject_name = '英语[职高]' AND status = 1 LIMIT 1);

-- 获取各学科level=1根节点ID
SET @yy_pg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_pg AND level = 1 LIMIT 1);
SET @yy_zg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_zg AND level = 1 LIMIT 1);
SET @en_pg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @en_pg AND level = 1 LIMIT 1);
SET @en_zg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @en_zg AND level = 1 LIMIT 1);

-- ============================================================================
-- 普高语文[全国考纲] (subject_id=@yy_pg, root=@yy_pg_root)
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
-- 现代文阅读
(@yy_pg_root, @yy_pg, 2, '现代文阅读', 1, NOW(), NOW()),
(@yy_pg_root, @yy_pg, 2, '古代诗文阅读', 2, NOW(), NOW()),
(@yy_pg_root, @yy_pg, 2, '语言文字运用', 3, NOW(), NOW()),
(@yy_pg_root, @yy_pg, 2, '写作', 4, NOW(), NOW());

-- 获取刚创建的章节ID
SET @yy_pg_ch1 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_pg_root AND name = '现代文阅读' LIMIT 1);
SET @yy_pg_ch2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_pg_root AND name = '古代诗文阅读' LIMIT 1);
SET @yy_pg_ch3 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_pg_root AND name = '语言文字运用' LIMIT 1);
SET @yy_pg_ch4 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_pg_root AND name = '写作' LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@yy_pg_ch1, @yy_pg, 3, '论述类文本阅读', 1, NOW(), NOW()),
(@yy_pg_ch1, @yy_pg, 3, '实用类文本阅读', 2, NOW(), NOW()),
(@yy_pg_ch1, @yy_pg, 3, '文学类文本阅读', 3, NOW(), NOW()),
(@yy_pg_ch2, @yy_pg, 3, '文言文阅读', 1, NOW(), NOW()),
(@yy_pg_ch2, @yy_pg, 3, '古代诗歌鉴赏', 2, NOW(), NOW()),
(@yy_pg_ch2, @yy_pg, 3, '名篇名句默写', 3, NOW(), NOW()),
(@yy_pg_ch3, @yy_pg, 3, '正确使用词语(成语)', 1, NOW(), NOW()),
(@yy_pg_ch3, @yy_pg, 3, '辨析并修改病句', 2, NOW(), NOW()),
(@yy_pg_ch3, @yy_pg, 3, '语言表达简明连贯得体', 3, NOW(), NOW()),
(@yy_pg_ch4, @yy_pg, 3, '审题立意', 1, NOW(), NOW()),
(@yy_pg_ch4, @yy_pg, 3, '议论文写作', 2, NOW(), NOW()),
(@yy_pg_ch4, @yy_pg, 3, '记叙文写作', 3, NOW(), NOW());

-- ============================================================================
-- 普高英语[全国考纲] (subject_id=@en_pg, root=@en_pg_root)
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@en_pg_root, @en_pg, 2, '听力理解', 1, NOW(), NOW()),
(@en_pg_root, @en_pg, 2, '阅读理解', 2, NOW(), NOW()),
(@en_pg_root, @en_pg, 2, '语言知识运用', 3, NOW(), NOW()),
(@en_pg_root, @en_pg, 2, '写作', 4, NOW(), NOW());

SET @en_pg_ch1 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_pg_root AND name = '听力理解' LIMIT 1);
SET @en_pg_ch2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_pg_root AND name = '阅读理解' LIMIT 1);
SET @en_pg_ch3 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_pg_root AND name = '语言知识运用' LIMIT 1);
SET @en_pg_ch4 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_pg_root AND name = '写作' LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@en_pg_ch1, @en_pg, 3, '短对话理解', 1, NOW(), NOW()),
(@en_pg_ch1, @en_pg, 3, '长对话与独白', 2, NOW(), NOW()),
(@en_pg_ch2, @en_pg, 3, '细节理解', 1, NOW(), NOW()),
(@en_pg_ch2, @en_pg, 3, '推理判断', 2, NOW(), NOW()),
(@en_pg_ch2, @en_pg, 3, '主旨大意', 3, NOW(), NOW()),
(@en_pg_ch3, @en_pg, 3, '完形填空', 1, NOW(), NOW()),
(@en_pg_ch3, @en_pg, 3, '语法填空', 2, NOW(), NOW()),
(@en_pg_ch4, @en_pg, 3, '短文改错', 1, NOW(), NOW()),
(@en_pg_ch4, @en_pg, 3, '书面表达', 2, NOW(), NOW());

-- ============================================================================
-- 职高语文[四川省对口升学] (subject_id=@yy_zg, root=@yy_zg_root)
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@yy_zg_root, @yy_zg, 2, '基础知识与运用', 1, NOW(), NOW()),
(@yy_zg_root, @yy_zg, 2, '现代文阅读', 2, NOW(), NOW()),
(@yy_zg_root, @yy_zg, 2, '文言文阅读', 3, NOW(), NOW()),
(@yy_zg_root, @yy_zg, 2, '写作', 4, NOW(), NOW());

SET @yy_zg_ch1 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '基础知识与运用' LIMIT 1);
SET @yy_zg_ch2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '现代文阅读' LIMIT 1);
SET @yy_zg_ch3 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '文言文阅读' LIMIT 1);
SET @yy_zg_ch4 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '写作' LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@yy_zg_ch1, @yy_zg, 3, '字音字形', 1, NOW(), NOW()),
(@yy_zg_ch1, @yy_zg, 3, '词语运用', 2, NOW(), NOW()),
(@yy_zg_ch1, @yy_zg, 3, '病句辨析与修辞', 3, NOW(), NOW()),
(@yy_zg_ch2, @yy_zg, 3, '社科类文本阅读', 1, NOW(), NOW()),
(@yy_zg_ch2, @yy_zg, 3, '文学作品阅读', 2, NOW(), NOW()),
(@yy_zg_ch3, @yy_zg, 3, '常见文言实词虚词', 1, NOW(), NOW()),
(@yy_zg_ch3, @yy_zg, 3, '文言文翻译与理解', 2, NOW(), NOW()),
(@yy_zg_ch4, @yy_zg, 3, '应用文写作', 1, NOW(), NOW()),
(@yy_zg_ch4, @yy_zg, 3, '话题作文', 2, NOW(), NOW());

-- ============================================================================
-- 职高英语[四川省对口升学] (subject_id=@en_zg, root=@en_zg_root)
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@en_zg_root, @en_zg, 2, '基础知识', 1, NOW(), NOW()),
(@en_zg_root, @en_zg, 2, '阅读理解', 2, NOW(), NOW()),
(@en_zg_root, @en_zg, 2, '翻译', 3, NOW(), NOW()),
(@en_zg_root, @en_zg, 2, '写作', 4, NOW(), NOW());

SET @en_zg_ch1 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_zg_root AND name = '基础知识' LIMIT 1);
SET @en_zg_ch2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_zg_root AND name = '阅读理解' LIMIT 1);
SET @en_zg_ch3 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_zg_root AND name = '翻译' LIMIT 1);
SET @en_zg_ch4 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_zg_root AND name = '写作' LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@en_zg_ch1, @en_zg, 3, '词汇与语法', 1, NOW(), NOW()),
(@en_zg_ch1, @en_zg, 3, '情景交际', 2, NOW(), NOW()),
(@en_zg_ch2, @en_zg, 3, '短文阅读', 1, NOW(), NOW()),
(@en_zg_ch2, @en_zg, 3, '任务型阅读', 2, NOW(), NOW()),
(@en_zg_ch3, @en_zg, 3, '英译汉', 1, NOW(), NOW()),
(@en_zg_ch3, @en_zg, 3, '汉译英', 2, NOW(), NOW()),
(@en_zg_ch4, @en_zg, 3, '应用文写作(书信/通知)', 1, NOW(), NOW()),
(@en_zg_ch4, @en_zg, 3, '话题写作', 2, NOW(), NOW());

SELECT 'v75: 文化课知识节点框架创建完成！' AS result;
SELECT CONCAT('普高语文: ', COUNT(*), ' 个节点') FROM knowledge_nodes WHERE subject_id = @yy_pg;
SELECT CONCAT('普高英语: ', COUNT(*), ' 个节点') FROM knowledge_nodes WHERE subject_id = @en_pg;
SELECT CONCAT('职高语文: ', COUNT(*), ' 个节点') FROM knowledge_nodes WHERE subject_id = @yy_zg;
SELECT CONCAT('职高英语: ', COUNT(*), ' 个节点') FROM knowledge_nodes WHERE subject_id = @en_zg;

-- ---------- v76_culture_exam_syllabus.sql ----------
-- ============================================================================
-- v76: 文化课考纲种子数据
-- 普高→全国考纲(GAOKAO) / 职高→四川省对口升学(DUIKOU)
-- 幂等：INSERT IGNORE
-- ============================================================================

SET @yy_pg_sid = (SELECT id FROM dict_subject WHERE subject_name = '语文[普高]' AND status = 1 LIMIT 1);
SET @en_pg_sid = (SELECT id FROM dict_subject WHERE subject_name = '英语[普高]' AND status = 1 LIMIT 1);
SET @yy_zg_sid = (SELECT id FROM dict_subject WHERE subject_name = '语文[职高]' AND status = 1 LIMIT 1);
SET @en_zg_sid = (SELECT id FROM dict_subject WHERE subject_name = '英语[职高]' AND status = 1 LIMIT 1);

-- ============================================================================
-- 普高语文 — 全国高考语文大纲
-- ============================================================================
INSERT IGNORE INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at)
VALUES (
    @yy_pg_sid, 'GAOKAO', 'BOTH',
    '普通高等学校招生全国统一考试语文大纲',
    '一、现代文阅读（论述类文本、实用类文本、文学类文本）：考查信息筛选、分析综合、鉴赏评价能力。'
    '二、古代诗文阅读（文言文阅读、古代诗歌鉴赏、名篇名句默写）：考查常见文言实词虚词、句式理解、诗歌形象/语言/表达技巧、名篇背诵默写。'
    '三、语言文字运用：正确使用词语(成语)、辨析修改病句、语言表达简明连贯得体、修辞手法运用。'
    '四、写作：审题立意、议论文写作（观点明确、论据充分、论证严密）、记叙文写作（内容具体、感情真实、结构完整）。'
    '基础等级(内容+表达)与发展等级(深刻+丰富+有文采+有创新)双维度评分。',
    '1.0', 1, NOW(), NOW()
);

-- ============================================================================
-- 普高英语 — 全国高考英语大纲
-- ============================================================================
INSERT IGNORE INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at)
VALUES (
    @en_pg_sid, 'GAOKAO', 'BOTH',
    '普通高等学校招生全国统一考试英语大纲',
    '一、听力理解：短对话理解(1-5题)、长对话与独白(6-20题)，考查获取事实信息、推断隐含意义、理解主旨要义能力。'
    '二、阅读理解：细节理解题、推理判断题、主旨大意题、词义猜测题，4篇短文+1篇七选五，考查不同文体的阅读策略。'
    '三、语言知识运用：完形填空(20题，考查语境词汇和语篇衔接)、语法填空(10题，考查基础语法和词形变化)。'
    '四、写作：短文改错(10处错误，考查语法准确性)、书面表达(100词左右，应用文/记叙文/议论文，考查内容完整性、语言准确性、篇章连贯性)。',
    '1.0', 1, NOW(), NOW()
);

-- ============================================================================
-- 职高语文 — 四川省对口升学语文大纲
-- ============================================================================
INSERT IGNORE INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at)
VALUES (
    @yy_zg_sid, 'DUIKOU', 'BOTH',
    '四川省对口升学考试语文大纲',
    '一、基础知识与运用：字音字形辨析、词语运用(近义词/成语)、病句辨析与修辞手法识别，以客观题为主。'
    '二、现代文阅读：社科类文本阅读(考查信息提取、分析推理)、文学作品阅读(考查形象分析、表达技巧、语言品味、主题理解)。'
    '三、文言文阅读：常见文言实词(120个)虚词(15个)的理解、文言文翻译(直译为主意译为辅)、文意理解与分析。'
    '四、写作：应用文写作(通知/启事/书信/条据等，考查格式规范和语言得体)、话题作文(600字左右，考查内容具体、语言通顺、结构完整)。',
    '1.0', 1, NOW(), NOW()
);

-- ============================================================================
-- 职高英语 — 四川省对口升学英语大纲
-- ============================================================================
INSERT IGNORE INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at)
VALUES (
    @en_zg_sid, 'DUIKOU', 'BOTH',
    '四川省对口升学考试英语大纲',
    '一、基础知识：词汇与语法(基础词汇2000个左右，考查词义辨析、时态语态、非谓语动词、复合句等基础语法)、情景交际(考查日常交际用语和场景对话)。'
    '二、阅读理解：短文阅读(3-4篇，考查细节理解、推理判断、主旨概括)、任务型阅读(信息匹配/表格填写)。'
    '三、翻译：英译汉(句子翻译，考查关键词语和句式理解)、汉译英(简单句子翻译，考查基础词汇和基本句式)。'
    '四、写作：应用文写作(书信/通知/便条等，80词左右，考查格式正确、内容完整、语言基本通顺)、话题写作(简单话题短文，考查基础表达能力)。',
    '1.0', 1, NOW(), NOW()
);

SELECT 'v76: 文化课考纲创建完成！' AS result;
SELECT exam_type AS '类型', title AS '考纲名称'
FROM exam_syllabus
WHERE subject_id IN (@yy_pg_sid, @en_pg_sid, @yy_zg_sid, @en_zg_sid)
ORDER BY exam_type, subject_id;

-- ---------- v77_rubric_templates.sql ----------
-- ============================================================================
-- v77: 语文/英语作文写作评分量规模板
-- 普高+职高，共4套Rubric
-- 幂等：INSERT IGNORE
-- ============================================================================

-- 语文普高作文
INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (100, '高考语文作文评分标准', 1, 3, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(100, '内容', 0.33, '切合题意、中心突出、内容充实、思想健康、感情真挚',
 '{"A": "切合题意，中心突出，内容充实，感情真挚", "B": "符合题意，中心明确，内容较充实，感情真实", "C": "基本符合题意，中心基本明确，内容单薄", "D": "偏离题意，中心不明确，内容不当"}'),
(100, '表达', 0.33, '符合文体要求、结构严谨、语言流畅、字迹工整',
 '{"A": "结构严谨，语言流畅，符合文体要求，字迹工整", "B": "结构完整，语言通顺，基本符合文体要求，字迹清楚", "C": "结构基本完整，语言基本通顺，字迹基本清楚", "D": "结构混乱，语言不通顺，语病多，字迹潦草"}'),
(100, '发展', 0.34, '深刻、丰富、有文采、有创新',
 '{"A": "观点深刻，材料丰富，语言有文采，见解新颖", "B": "观点较深刻，材料较丰富，语言较有文采", "C": "略有深刻性或丰富性", "D": "无明显特色"}');

-- 语文职高作文
INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (101, '对口升学语文作文评分标准', 1, 4, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(101, '内容', 0.38, '符合题意、内容具体、感情真实、思想健康',
 '{"A": "切合题意，内容具体充实，感情真实", "B": "符合题意，内容较具体，感情较真实", "C": "基本符合题意，内容不够具体", "D": "偏离题意，内容空泛"}'),
(101, '语言', 0.37, '语句通顺、表达清晰、格式规范、标点正确',
 '{"A": "语句通顺流畅，表达清晰准确", "B": "语句较通顺，表达较清晰", "C": "语句基本通顺，偶有语病", "D": "语句不通顺，语病较多"}'),
(101, '结构', 0.25, '条理清楚、分段合理、结构完整',
 '{"A": "条理清晰，层次分明，结构严谨", "B": "条理较清晰，结构完整", "C": "条理基本清晰，结构基本完整", "D": "条理不清，结构混乱"}');

-- 英语普高写作
INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (102, '高考英语书面表达评分标准', 1, 3, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(102, '内容要点', 0.33, '覆盖所有内容要点，表达清楚',
 '{"A": "覆盖所有要点，内容充实", "B": "覆盖大部分要点，内容较充实", "C": "遗漏部分要点，内容基本清楚", "D": "遗漏大部分要点，内容不清"}'),
(102, '语言质量', 0.34, '词汇丰富，句式多样，衔接自然',
 '{"A": "词汇丰富准确，句式灵活多样，衔接紧凑", "B": "词汇较丰富，句式有一定变化，衔接较自然", "C": "词汇基本够用，句式单一，衔接不够自然", "D": "词汇贫乏，表达困难"}'),
(102, '语法准确性', 0.33, '语法错误少，拼写和标点规范',
 '{"A": "语法结构准确，拼写标点正确", "B": "少量语法错误，不影响理解", "C": "一些语法错误，个别影响理解", "D": "语法错误较多，影响理解"}');

-- 英语职高写作
INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (103, '对口升学英语写作评分标准', 1, 4, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(103, '内容完整', 0.33, '覆盖主要内容点，表达基本意思',
 '{"A": "全面覆盖要点，内容完整", "B": "覆盖大部分要点", "C": "覆盖部分要点", "D": "内容不完整"}'),
(103, '语言表达', 0.34, '语句基本通顺，用词基本准确',
 '{"A": "语句通顺，用词准确", "B": "语句较通顺，用词较准确", "C": "语句基本通顺，有少量错误", "D": "错误较多，影响理解"}'),
(103, '格式规范', 0.33, '格式正确，书写规范，拼写基本正确',
 '{"A": "格式正确规范，书写整洁", "B": "格式较规范", "C": "格式基本规范", "D": "格式不规范"}');

SELECT 'v77: 作文写作评分量规模板创建完成！' AS result;
SELECT CONCAT(r.name, ' (', COUNT(d.id), '个维度)') AS detail
FROM rubric r LEFT JOIN rubric_dimension d ON r.id = d.rubric_id
WHERE r.id BETWEEN 100 AND 103
GROUP BY r.id, r.name;

-- ---------- v78_practical_writing_knowledge_points.sql ----------
-- ============================================================================
-- v78: 职高语文应用文写作知识点补充 (level-4)
-- 节点 862(应用文写作) → 6个常见文体知识点
-- 幂等：INSERT IGNORE
-- ============================================================================

SET @app_writing_id = 862;
SET @subject_id = 20;  -- 语文[职高]

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, created_at, updated_at)
VALUES
-- 1. 通知
(@app_writing_id, @subject_id, 4, '通知',
 '## 通知写作要点\n\n'
 '**格式规范**：\n'
 '1. 标题：首行居中写"通知"或"关于××的通知"\n'
 '2. 称呼：顶格写被通知对象，后加冒号\n'
 '3. 正文：另起一行空两格，写明通知事项（时间、地点、人物、事项、要求）\n'
 '4. 落款：右下角写发通知单位（可省略）\n'
 '5. 日期：落款下方写发通知日期\n\n'
 '**语言要求**：简明扼要、准确清楚、通俗易懂\n\n'
 '**考查重点**：格式要素完整、事项交代清楚、语言简洁规范',
 1, NOW(), NOW()),

-- 2. 启事
(@app_writing_id, @subject_id, 4, '启事',
 '## 启事写作要点\n\n'
 '**常见类型**：寻人启事、寻物启事、招领启事、征文启事、招聘启事\n\n'
 '**格式规范**：\n'
 '1. 标题：首行居中写"××启事"\n'
 '2. 正文：写明事由、具体内容、联系方式\n'
 '3. 落款：右下角写发启事人或单位\n'
 '4. 日期：落款下方写日期\n\n'
 '**注意**：招领启事不可详述物品特征（防冒领），寻物启事则需详述\n\n'
 '**语言要求**：诚恳、具体、有礼、简洁',
 2, NOW(), NOW()),

-- 3. 书信
(@app_writing_id, @subject_id, 4, '书信',
 '## 书信写作要点\n\n'
 '**格式规范**：\n'
 '1. 称呼：顶格写，后加冒号\n'
 '2. 问候语：另起一行空两格，如"你好"\n'
 '3. 正文：另起一行空两格，分段叙述\n'
 '4. 祝颂语：分两行，先写"此致"空两格，换行顶格写"敬礼"\n'
 '5. 署名：右下角写写信人\n'
 '6. 日期：署名下方写日期\n\n'
 '**语言要求**：口语化但得体，视对象调整语气（长辈→敬重、平辈→亲切）\n\n'
 '**考查重点**：格式完整、语言得体、内容清楚',
 3, NOW(), NOW()),

-- 4. 条据
(@app_writing_id, @subject_id, 4, '条据',
 '## 条据写作要点\n\n'
 '**常见类型**：借条、收条、请假条、留言条\n\n'
 '**借条/收条格式**：\n'
 '1. 标题：首行居中写"借条"或"收条"\n'
 '2. 正文：写明"今借到"或"今收到"+物品/金额(大写)+事由+归还日期\n'
 '3. 落款：借款人/收款人签名\n'
 '4. 日期：写明年月日\n\n'
 '**请假条格式**：\n'
 '1. 称呼：顶格写给谁\n'
 '2. 正文：请假原因+起止时间\n'
 '3. 敬语："此致敬礼"\n'
 '4. 落款：请假人+日期\n\n'
 '**语言要求**：准确无歧义（数字大写）、简洁明了',
 4, NOW(), NOW()),

-- 5. 总结
(@app_writing_id, @subject_id, 4, '总结',
 '## 总结写作要点\n\n'
 '**常见类型**：学习总结、工作总结、活动总结\n\n'
 '**结构规范**：\n'
 '1. 标题：可直书"××总结"或正副标题结合\n'
 '2. 前言：概述背景、时间段、总体情况\n'
 '3. 主体：分点叙述——成绩与做法、经验与体会、问题与不足\n'
 '4. 结尾：今后打算或努力方向\n'
 '5. 落款：总结人+日期\n\n'
 '**语言要求**：客观真实、条理清晰、语言平实\n\n'
 '**考查重点**：结构完整、内容具体、有反思意识',
 5, NOW(), NOW()),

-- 6. 会议记录
(@app_writing_id, @subject_id, 4, '会议记录',
 '## 会议记录写作要点\n\n'
 '**格式规范**：\n'
 '1. 会议名称、时间、地点\n'
 '2. 出席人、主持人、记录人\n'
 '3. 会议议题\n'
 '4. 发言记录（摘要式或详细式）：按发言顺序记录要点\n'
 '5. 决议事项：会议达成的决定或安排\n'
 '6. 主持人签名+记录人签名+日期\n\n'
 '**语言要求**：准确客观、条理清晰、不掺杂个人观点\n\n'
 '**考查重点**：要素完整、重点突出、记录准确',
 6, NOW(), NOW());

SELECT 'v78: 应用文写作知识点补充完成！' AS result;
SELECT CONCAT('应用文写作下共 ', COUNT(*), ' 个知识点') FROM knowledge_nodes WHERE parent_id = @app_writing_id;

-- ---------- v79_practical_writing_rubric.sql ----------
-- ============================================================================
-- v79: 职高语文应用文写作专用评分量规
-- 区别于话题作文（Rubric 101），应用文重在格式规范+语言得体+内容要素
-- 幂等：INSERT IGNORE
-- ============================================================================

INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (104, '对口升学应用文写作评分标准', 1, 4, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(104, '格式规范', 0.40, '文体格式正确、要素齐全、书写位置规范',
 '{"A": "格式完全正确，所有要素齐全且位置规范（标题居中、称呼顶格、落款右下角等）", '
  '"B": "格式基本正确，1-2处小瑕疵但不影响整体规范", '
  '"C": "格式有明显错误（如称呼未顶格、落款位置不当、缺少标题）", '
  '"D": "格式错误严重，要素缺失较多或完全不符合文体要求"}'),

(104, '语言得体', 0.35, '语言符合应用文文体要求：简明、准确、恰当',
 '{"A": "语言简明准确，完全符合文体特点（通知的简洁、书信的得体、启事的诚恳），无语病", '
  '"B": "语言较通顺得体，个别词句不够精炼但不影响表达", '
  '"C": "语言基本清楚，但不够简明准确（啰嗦或过于口语化）", '
  '"D": "语言不通顺，表达含混不清或严重不符合文体要求"}'),

(104, '内容要素', 0.25, '时间/地点/人物/事项/联系方式等关键信息完整',
 '{"A": "所有必要信息完整准确（如通知的时间地点事项、借条的金额大写、书信的祝愿语等）", '
  '"B": "主要信息齐全，个别次要信息遗漏", '
  '"C": "关键信息有遗漏，影响理解", '
  '"D": "内容严重不全，无法实现应用文书的基本功能"}');

SELECT 'v79: 应用文写作评分量规创建完成！' AS result;
SELECT CONCAT(r.name, ' (', COUNT(d.id), '个维度, 格式', ROUND(MAX(CASE WHEN d.name='格式规范' THEN d.weight END)*100), '%)') AS detail
FROM rubric r JOIN rubric_dimension d ON r.id = d.rubric_id
WHERE r.id = 104 GROUP BY r.id, r.name;

-- ---------- v80_enhance_exam_syllabus_practical_writing.sql ----------
-- ============================================================================
-- v80: 增强职高语文考纲中应用文写作的描述
-- 幂等：UPDATE 可重复执行
-- ============================================================================

UPDATE exam_syllabus
SET content = '一、基础知识与运用：字音字形辨析、词语运用(近义词/成语)、病句辨析与修辞手法识别，以客观题为主。'
    '二、现代文阅读：社科类文本阅读(考查信息提取、分析推理)、文学作品阅读(考查形象分析、表达技巧、语言品味、主题理解)。'
    '三、文言文阅读：常见文言实词(120个)虚词(15个)的理解、文言文翻译(直译为主意译为辅)、文意理解与分析。'
    '四、写作（两大题型）：'
    '（一）应用文写作（约15分）：考查范围包括通知、启事、书信、条据（借条/收条/请假条）、总结、会议记录等常见应用文体。'
    '考查重点——格式规范（标题、称呼、正文、落款、日期等要素完整正确）、语言得体（简明准确、符合文体特点）、内容要素（时间/地点/人物/事项/联系方式等关键信息齐全）。'
    '（二）话题作文（约45分）：600字左右，考查内容具体充实、语言通顺流畅、结构完整清晰。基础等级（内容+表达）与发展等级（深刻+丰富+有文采+有创新）双维度评分。',
    updated_at = NOW()
WHERE subject_id = (SELECT id FROM dict_subject WHERE subject_name = '语文[职高]' AND status = 1 LIMIT 1)
  AND exam_type = 'DUIKOU'
  AND title LIKE '%对口升学%语文%';

SELECT 'v80: 应用文写作考纲增强完成！' AS result;

-- ---------- v81_data_repair.sql ----------
-- ============================================================================
-- v81: 数据修补 — 批量创建学生/教师档案 + 班级 + 教学关系
-- 解决"1个班1个教师1个学生"的数据空壳问题
-- 幂等：INSERT IGNORE + 条件判断
-- ============================================================================

-- ============================================================
-- 1. 创建额外班级
-- ============================================================
INSERT IGNORE INTO classes (id, class_name, class_code, grade, major, academic_year, semester, head_teacher_id, status, student_count, school_id, stage_id, class_type, create_time, update_time)
VALUES
(2, '职高语文1班', 'ywzg01', '2024级', '语文', '2025-2026', '下', NULL, 1, 0, 1, 4, 'vocational', NOW(), NOW()),
(3, '职高英语1班', 'enzg01', '2024级', '英语', '2025-2026', '下', NULL, 1, 0, 1, 4, 'vocational', NOW(), NOW()),
(4, '普高文科班', 'pgwk01', '2024级', '文科', '2025-2026', '下', NULL, 1, 0, 1, 3, 'academic', NOW(), NOW());

-- ============================================================
-- 2. 批量创建教师档案（users表已有TEACHER角色，补teachers表记录）
-- ============================================================
INSERT IGNORE INTO teachers (id, user_id, teacher_number, gender, subject, school_id, create_time, update_time)
SELECT
  CASE u.id
    WHEN 14 THEN 2 WHEN 15 THEN 3 WHEN 16 THEN 4
    WHEN 259 THEN 5 WHEN 1008 THEN 6
  END AS id,
  u.id AS user_id,
  CASE u.id
    WHEN 14 THEN 'yw001' WHEN 15 THEN 'sx001' WHEN 16 THEN 'en001'
    WHEN 259 THEN 'yw002' WHEN 1008 THEN 'en002'
  END AS teacher_number,
  1 AS gender,
  CASE u.id
    WHEN 14 THEN '语文[职高]' WHEN 15 THEN '数学[职高]' WHEN 16 THEN '英语[职高]'
    WHEN 259 THEN '语文[普高]' WHEN 1008 THEN '英语[普高]'
  END AS subject,
  1 AS school_id,
  NOW(), NOW()
FROM users u
WHERE u.role_name = 'TEACHER'
  AND u.id NOT IN (SELECT user_id FROM teachers WHERE user_id IS NOT NULL);

-- ============================================================
-- 3. 教师-班级任教分配
-- ============================================================
INSERT IGNORE INTO teacher_classes (teacher_id, class_id, subject, school_id)
SELECT t.id, 2, '语文[职高]', 1 FROM teachers t WHERE t.user_id = 14
UNION ALL SELECT t.id, 3, '英语[职高]', 1 FROM teachers t WHERE t.user_id = 16
UNION ALL SELECT t.id, 4, '语文[普高]', 1 FROM teachers t WHERE t.user_id = 259
UNION ALL SELECT t.id, 4, '英语[普高]', 1 FROM teachers t WHERE t.user_id = 1008
UNION ALL SELECT t.id, 2, '数学[职高]', 1 FROM teachers t WHERE t.user_id = 15;

-- ============================================================
-- 4. 批量创建学生档案（users表已有STUDENT角色，补students表记录）
-- ============================================================
INSERT IGNORE INTO students (user_id, student_number, gender, birthday, enrollment_year, class_id, school_id, total_credits, current_streak, status, create_time, update_time)
SELECT
  u.id AS user_id,
  CONCAT('2024', LPAD(ROW_NUMBER() OVER (ORDER BY u.id), 4, '0')) AS student_number,
  1 AS gender,
  '2006-09-01' AS birthday,
  2024 AS enrollment_year,
  CASE
    WHEN u.id <= 230 THEN 1    -- 前30人 → 计算机17班
    WHEN u.id <= 250 THEN 2    -- 20人 → 职高语文1班
    WHEN u.id <= 260 THEN 3    -- 10人 → 职高英语1班
    ELSE 4                      -- 其余 → 普高文科班
  END AS class_id,
  1 AS school_id,
  0 AS total_credits,
  0 AS current_streak,
  'active' AS status,
  NOW(), NOW()
FROM users u
WHERE u.role_name = 'STUDENT'
  AND u.status = 1
  AND u.id NOT IN (SELECT user_id FROM students WHERE user_id IS NOT NULL);

-- ============================================================
-- 5. 将学生分配到班级（class_students表）
-- ============================================================
INSERT IGNORE INTO class_students (class_id, student_id, school_id, join_time)
SELECT s.class_id, s.id, 1, NOW()
FROM students s
WHERE s.class_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM class_students cs WHERE cs.class_id = s.class_id AND cs.student_id = s.id
  );

-- ============================================================
-- 6. 更新班级学生计数
-- ============================================================
UPDATE classes c SET student_count = (
  SELECT COUNT(*) FROM students s WHERE s.class_id = c.id
), update_time = NOW()
WHERE c.id IN (1, 2, 3, 4);

-- ============================================================
-- 7. 设置班主任
-- ============================================================
UPDATE classes SET head_teacher_id = (SELECT t.id FROM teachers t WHERE t.user_id = 14) WHERE id = 2;
UPDATE classes SET head_teacher_id = (SELECT t.id FROM teachers t WHERE t.user_id = 16) WHERE id = 3;

SELECT 'v81: 数据修补完成！' AS result;
SELECT '班级' AS category, COUNT(*) AS cnt FROM classes
UNION ALL SELECT '教师档案', COUNT(*) FROM teachers
UNION ALL SELECT '学生档案', COUNT(*) FROM students
UNION ALL SELECT '任教分配', COUNT(*) FROM teacher_classes
UNION ALL SELECT '班级学生', COUNT(*) FROM class_students;

-- ---------- v82_chinese_vocational_knowledge_content.sql ----------
-- ============================================================================
-- v82: 语文[职高]前2章节知识点内容补充
-- 为"基础知识与运用"和"现代文阅读"的level-2/3节点添加教学参考内容
-- 幂等：UPDATE可重复执行
-- ============================================================================

-- ============================================================
-- Level-2 章节内容
-- ============================================================
UPDATE knowledge_nodes SET content = '## 基础知识与运用\n\n本部分为四川省对口升学考试语文科目的基础板块，以客观题（选择题）为主，考查学生的汉语基础素养。\n\n**考查范围**：\n- 字音字形辨析\n- 词语运用（近义词、成语）\n- 病句辨析与修辞手法识别\n\n**分值占比**：约占总分的15%-20%。\n\n**教学重点**：注重积累，强化辨析能力，通过大量练习巩固基础。'
WHERE id = 851;

UPDATE knowledge_nodes SET content = '## 现代文阅读\n\n本部分考查学生对现代文（社科类文本和文学作品）的理解、分析和鉴赏能力。\n\n**考查范围**：\n- 社科类文本：信息提取、分析推理\n- 文学作品：形象分析、表达技巧、语言品味、主题理解\n\n**分值占比**：约占总分的20%-25%。\n\n**教学重点**：培养学生的文本细读能力，注重阅读方法和答题技巧的指导。'
WHERE id = 852;

-- ============================================================
-- Level-3 任务节点内容（基础知识与运用）
-- ============================================================
UPDATE knowledge_nodes SET content = '## 字音字形\n\n**考查要点**：\n1. 常见多音字辨析（如"差"有chā/chà/chāi/cī四音）\n2. 形近字区分（如"己/已/巳"、"戊/戌/戍"）\n3. 易读错字（如"莘莘学子"读shēn不读xīn）\n4. 易写错字（如"再接再厉"非"再接再励"）\n\n**教学方法**：\n- 归类记忆法：按偏旁部首或读音归类\n- 对比辨析法：形近字对比学习\n- 语境记忆法：在词语和句子中记忆\n\n**常见题型**：选择读音或书写完全正确的一项。'
WHERE id = 855;

UPDATE knowledge_nodes SET content = '## 词语运用\n\n**考查要点**：\n1. 近义词辨析（如"必须/必需"、"反应/反映"）\n   - 词义轻重、范围大小、搭配对象、感情色彩\n2. 成语使用（常见易误用成语）\n   - 望文生义类：如"首当其冲"非"首先"\n   - 对象误用类：如"相敬如宾"用于夫妻\n   - 褒贬颠倒类：如"无所不为"是贬义\n3. 关联词语搭配（"虽然…但是…"、"因为…所以…"等）\n\n**教学方法**：\n- 义素分析法：从语义成分角度区分近义词\n- 语境造句法：在不同语境中理解词语用法\n- 成语故事法：通过成语典故理解其含义'
WHERE id = 856;

UPDATE knowledge_nodes SET content = '## 病句辨析与修辞\n\n**病句常见类型**：\n1. 语序不当（如"我们认真研究听取了大家的意见"应为"听取并研究"）\n2. 搭配不当（如"提高水平"正确，"提高能力"正确，"提高意识"错误）\n3. 成分残缺或赘余（缺主语、缺宾语、重复啰嗦）\n4. 结构混乱（句式杂糅，两句混杂）\n5. 表意不明（歧义句）\n6. 不合逻辑（自相矛盾、否定不当）\n\n**常见修辞手法**：\n1. 比喻（明喻/暗喻/借喻）\n2. 拟人\n3. 夸张\n4. 排比\n5. 对偶\n6. 反复\n7. 设问/反问\n8. 借代\n\n**教学方法**：\n- 病句"诊断法"：主干提取→枝叶检查→逻辑分析\n- 修辞"赏析法"：识别手法→分析效果→体会情感'
WHERE id = 857;

-- ============================================================
-- Level-3 任务节点内容（现代文阅读）
-- ============================================================
UPDATE knowledge_nodes SET content = '## 社科类文本阅读\n\n**文本类型**：\n- 论述文（议论文）：论点、论据、论证方法\n- 说明文：说明对象、说明方法、说明顺序\n- 新闻通讯：新闻要素、报道角度\n\n**考查能力**：\n1. 信息筛选与整合：定位关键信息，归纳要点\n2. 分析推理：根据文本信息进行合理推断\n3. 把握思路：理清文章结构层次和论证逻辑\n4. 理解重要概念和句子含义\n\n**答题技巧**：\n- 先读题干，带着问题读文本\n- 关注首尾段、过渡句、中心句\n- 选项比对原文，注意偷换概念、以偏概全\n- 分析推理类题目不可脱离文本主观臆断\n\n**常见题型**：\n- "下列关于原文内容的理解和分析，正确/不正确的一项是"\n- "根据原文内容，下列说法正确/不正确的一项是"'
WHERE id = 858;

UPDATE knowledge_nodes SET content = '## 文学作品阅读\n\n**文本类型**：\n- 散文：叙事散文、抒情散文、哲理散文\n- 小说（微型/短篇）：人物、情节、环境\n- 现代诗歌：意象、意境、情感\n\n**考查能力**：\n1. 形象分析：人物性格特点、塑造手法\n2. 表达技巧：修辞手法、表现手法、表达方式\n   - 表现手法：象征、对比、衬托（正衬/反衬）、欲扬先抑、借景抒情\n   - 表达方式：记叙、描写、抒情、议论、说明\n3. 语言品味：关键词句的含义和表达效果\n4. 主题理解：作品的思想感情和价值倾向\n\n**答题规范**：\n- 概括类："通过…表现…表达…"\n- 赏析类："运用…手法，描绘…，表达…情感"\n- 含义类："表层含义是…，深层含义是…"'
WHERE id = 859;

SELECT 'v82: 语文[职高]知识点内容补充完成！' AS result;
SELECT CONCAT(name, ' (', CHAR_LENGTH(content), '字)') AS detail
FROM knowledge_nodes
WHERE id IN (851, 852, 855, 856, 857, 858, 859)
ORDER BY id;

-- ============================================================
-- v154: 知识库自测结果持久化
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_quiz_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '→ users.id',
    article_id BIGINT NOT NULL COMMENT '→ knowledge_articles.id',
    total_questions INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    score DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '正确率百分比',
    wrong_question_ids JSON DEFAULT NULL COMMENT '答错题目的索引数组',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_article (student_id, article_id),
    INDEX idx_student_created (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库自测结果';

SELECT 'v154: knowledge_quiz_results 表已创建' AS result;
-- ============================================================================
-- v179: 语文[职高] 考纲对齐修复 — 四川省对口升学2014版
-- 基于审查报告 docs/语文职高-考纲对齐审查报告-2026-06-30.md
-- 修复项:
--   1. 新增 L3「古诗词鉴赏」+ 7篇指定篇目L4
--   2. 新增 L3「文学常识与名句默写」+ 14篇默写篇目L4
--   3. 新增 L3「标点符号」+ 9种常用标点L4
--   4. 补齐应用文缺失3种(单据/说明书/会议记录) + 拆分条据
--   5. 新增 L3「修辞手法辨析」+ 8种修辞+易混辨析L4
--   6. 修正 exam_syllabus 考纲文本: 虚词15→18个 + 补充能力层级
-- 幂等：所有 INSERT 使用 IGNORE，UPDATE 使用条件判断
-- ============================================================================

SET @yy_zg_sid = (SELECT id FROM dict_subject WHERE subject_name = '语文[职高]' AND status = 1 LIMIT 1);
SET @yy_zg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 1 LIMIT 1);

-- 获取现有 L2/L3 节点ID（用于挂载新节点）
SET @l2_jczs = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '基础知识与运用' LIMIT 1);
SET @l2_xdyd = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '现代文阅读' LIMIT 1);
SET @l2_wyyd = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '文言文阅读' LIMIT 1);
SET @l2_xz   = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '写作' LIMIT 1);

SET @l3_yysc = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_wyyd AND name = '常见文言实词虚词' LIMIT 1);
SET @l3_wyfy = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_wyyd AND name = '文言文翻译与理解' LIMIT 1);
SET @l3_yysc_l2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_jczs AND name = '词语运用' LIMIT 1);
SET @l3_bjcf = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_jczs AND name = '病句辨析与修辞' LIMIT 1);
SET @l3_yyw = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_xz AND name = '应用文写作' LIMIT 1);
SET @l3_htzw = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_xz AND name = '话题作文' LIMIT 1);

-- ============================================================================
-- Part 1: 新增 L3 节点
-- ============================================================================

-- 1a. L3「古诗词鉴赏」→ 挂在 L2「文言文阅读」下
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l2_wyyd, @yy_zg_sid, 3, '古诗词鉴赏', 3, NOW(), NOW());

-- 1b. L3「文学常识与名句默写」→ 挂在 L2「文言文阅读」下
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l2_wyyd, @yy_zg_sid, 3, '文学常识与名句默写', 4, NOW(), NOW());

-- 1c. L3「标点符号」→ 挂在 L2「基础知识与运用」下
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l2_jczs, @yy_zg_sid, 3, '标点符号', 4, NOW(), NOW());

-- 1d. 拆分「病句辨析与修辞」→ 新增独立 L3「修辞手法辨析」
--     原 L3「病句辨析与修辞」改名为「病句辨析」
UPDATE knowledge_nodes SET name = '病句辨析' WHERE id = @l3_bjcf AND name = '病句辨析与修辞';

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l2_jczs, @yy_zg_sid, 3, '修辞手法辨析', 5, NOW(), NOW());

-- 重新获取可能变化的节点ID
SET @l3_gscjs = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_wyyd AND name = '古诗词鉴赏' LIMIT 1);
SET @l3_wxcs = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_wyyd AND name = '文学常识与名句默写' LIMIT 1);
SET @l3_bd = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_jczs AND name = '标点符号' LIMIT 1);
SET @l3_xc = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_jczs AND name = '修辞手法辨析' LIMIT 1);

-- ============================================================================
-- Part 2: 古诗词鉴赏 L4 — 7篇指定阅读篇目
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_gscjs, @yy_zg_sid, 4, '《诗经》选篇：关雎、蒹葭', 1, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '唐诗鉴赏：将进酒、茅屋为秋风所破歌', 2, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '宋词鉴赏：念奴娇·赤壁怀古、雨霖铃', 3, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '诗歌意象与意境分析', 4, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '诗歌表达技巧（抒情方式/描写手法/修辞）', 5, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '诗歌思想情感与观点态度', 6, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '诗歌语言赏析（炼字/诗眼/风格）', 7, NOW(), NOW());

-- ============================================================================
-- Part 3: 文学常识与名句默写 L4 — 14篇默写篇目 + 文学常识
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_wxcs, @yy_zg_sid, 4, '先秦诗文默写：静女、采薇、侍坐、寡人之于国、劝学', 1, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '唐宋诗文默写：师说、将进酒、琵琶行、念奴娇、雨霖铃', 2, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '宋文默写：六国论', 3, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '现代诗歌默写：我爱这土地、雨巷、致橡树', 4, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '文学体裁常识（诗歌/散文/小说/戏剧）', 5, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '重要作家作品（古今中外）', 6, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '文化常识（称谓/历法/官职/科举/地理）', 7, NOW(), NOW());

-- ============================================================================
-- Part 4: 标点符号 L4 — 9种常用标点
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_bd, @yy_zg_sid, 4, '顿号与逗号', 1, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '分号', 2, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '问号', 3, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '引号', 4, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '省略号', 5, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '破折号', 6, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '书名号', 7, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '连接号与间隔号', 8, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '标点符号综合辨析', 9, NOW(), NOW());

-- ============================================================================
-- Part 5: 修辞手法辨析 L4 — 8种修辞 + 易混辨析
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_xc, @yy_zg_sid, 4, '比喻', 1, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '比拟（拟人+拟物）', 2, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '借代', 3, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '夸张', 4, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '对偶', 5, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '排比', 6, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '反问', 7, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '设问', 8, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '易混修辞辨析（借代vs借喻/比喻vs比拟/对偶vs排比/设问vs反问）', 9, NOW(), NOW());

-- ============================================================================
-- Part 6: 应用文写作 — 补齐缺失3种 + 2种细化 + 拆分条据
-- ============================================================================

-- 6a. 补充缺失的应用文类型
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_yyw, @yy_zg_sid, 4, '单据（借条/收条/领条/欠条）', 7, NOW(), NOW()),
(@l3_yyw, @yy_zg_sid, 4, '说明书', 8, NOW(), NOW()),
(@l3_yyw, @yy_zg_sid, 4, '会议记录', 9, NOW(), NOW());

-- 6b. 将「条据」拆分为「便条」和「单据」（如果存在）
--     单据内容已在上方新建，这里更新旧「条据」节点名称为「便条」
UPDATE knowledge_nodes
SET name = '便条（请假条/留言条/托事条）'
WHERE parent_id = @l3_yyw AND name = '条据';

-- 6c. 补充话题作文写作技巧L4
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_htzw, @yy_zg_sid, 4, '审题与立意', 1, NOW(), NOW()),
(@l3_htzw, @yy_zg_sid, 4, '材料作文的阅读与分析', 2, NOW(), NOW()),
(@l3_htzw, @yy_zg_sid, 4, '议论文写作结构（引论-本论-结论）', 3, NOW(), NOW()),
(@l3_htzw, @yy_zg_sid, 4, '记叙文写作要素与方法', 4, NOW(), NOW()),
(@l3_htzw, @yy_zg_sid, 4, '作文语言提升（句式变化/修辞润色）', 5, NOW(), NOW());

-- ============================================================================
-- Part 7: 文言文阅读 L4 补充 — 指定篇目 + 虚词
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_wyfy, @yy_zg_sid, 4, '《廉颇蔺相如列传》精读', 3, NOW(), NOW()),
(@l3_wyfy, @yy_zg_sid, 4, '《子路、曾皙、冉有、公西华侍坐》精读', 4, NOW(), NOW()),
(@l3_wyfy, @yy_zg_sid, 4, '《劝学》精读', 5, NOW(), NOW()),
(@l3_wyfy, @yy_zg_sid, 4, '《师说》精读', 6, NOW(), NOW()),
(@l3_yysc, @yy_zg_sid, 4, '常见文言虚词（18个）：而何乎乃其且若所为焉也以因于与则者之', 3, NOW(), NOW());

-- ============================================================================
-- Part 8: 修正 exam_syllabus 考纲文本
-- ============================================================================
UPDATE exam_syllabus
SET content = CONCAT(
    '一、基础知识与运用：字音字形辨析（正确读音与拼写规则/多音字/形近字辨识）、词语运用（近义词辨析/成语理解与运用/感情色彩辨析）、病句辨析（语序不当/搭配不当/成分残缺或赘余/结构混乱/表意不明/不合逻辑六大类型）、标点符号（顿号/逗号/分号/问号/引号/省略号/破折号/书名号/连接号）、修辞手法辨析（比喻/比拟/借代/夸张/对偶/排比/反问/设问8种）。',
    '二、现代文阅读：社科类文本阅读（考查信息提取、分析推理）、文学作品阅读（散文与小说，考查形象分析、表达技巧、语言品味、主题理解）。',
    '三、文言文阅读：常见文言实词(120个)虚词(18个)的理解、文言文翻译(直译为主意译为辅)、文意理解与分析；指定篇目(劝学/师说/廉颇蔺相如列传/侍坐/诗经/唐诗宋词)。',
    '四、古诗词鉴赏：诗歌意象与意境分析、表达技巧鉴赏、思想情感评价、语言赏析。',
    '五、文学常识与名句默写：文学体裁常识、重要作家作品、文化常识、14篇必背名句名篇默写。',
    '六、写作：应用文写作(便条/单据/启事/通知/计划/总结/说明书/会议记录/求职信/应聘书10种，考查格式规范与语言得体)、话题作文(600字左右，考查审题立意/内容具体/语言通顺/结构完整)。',
    '能力层级分布：识记(A)10% + 理解(B)25% + 综合分析(C)15% + 表达应用(D)45% + 鉴赏评价(E)5%。试卷150分/150分钟。'
),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND exam_type = 'DUIKOU';

-- ============================================================================
-- 验证
-- ============================================================================
SELECT CONCAT('v179: 语文[职高] 考纲对齐修复完成！') AS result;
SELECT CONCAT('节点总数: ', COUNT(*)) AS stat FROM knowledge_nodes WHERE subject_id = @yy_zg_sid;
SELECT CONCAT('L3任务: ', COUNT(*)) FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 3;
SELECT CONCAT('L4知识点: ', COUNT(*)) FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 4;

-- 列出所有L3节点（确认结构）
SELECT CONCAT('  L3: ', name) FROM knowledge_nodes
WHERE subject_id = @yy_zg_sid AND level = 3
ORDER BY sort_order, id;

-- 确认考纲文本已更新
SELECT CONCAT('考纲文本长度: ', CHAR_LENGTH(content), '字符（原~280字符，现应>600字符）')
FROM exam_syllabus WHERE subject_id = @yy_zg_sid AND exam_type = 'DUIKOU';
-- ============================================================================
-- v170: 网络应用基础 — 知识库文章种子（单元1 初识计算机网络）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖5个任务15个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务1 走进计算机网络（知识点1075~1078）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10101: 计算机网络的定义与功能（node=1075）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10101, '计算机网络的定义与功能',
'## 什么是计算机网络？

计算机网络是利用**通信线路**和**通信设备**，将地理上分散的、具有独立功能的多个计算机系统互连起来，在网络软件的支持下实现**数据通信**和**资源共享**的系统。

## 计算机网络的四大功能

| 功能 | 说明 | 生活例子 |
|------|------|----------|
| **数据通信** 👈 最基本功能 | 计算机之间传送数据 | 微信发文件、视频通话 |
| **资源共享** 👈 最重要功能 | 共享硬件/软件/数据资源 | 办公室共享打印机、云盘 |
| **分布式处理** | 大任务拆解到多台计算机并行处理 | 淘宝双11的云计算 |
| **提高可靠性** | 单点故障由其他设备替代 | 服务器集群、异地备份 |

### 数据通信
计算机网络最核心的用途就是让计算机之间能互相"说话"——传输文件、发送消息、浏览网页，本质上都是数据通信。

### 资源共享
- **硬件共享**：多台电脑共享一台打印机
- **软件共享**：网络版软件无需每台安装
- **数据共享**：数据库供全公司访问

### 分布式处理
把一个大任务拆成多个小任务，分配给网络上的多台计算机同时处理，最后汇总结果。云计算就是典型的分布式处理。

### 提高可靠性
网络中某台计算机故障，任务可由其他计算机接替，避免系统完全瘫痪。

> **对口升学高频考点**：计算机网络的主要目的是**资源共享和数据通信**。考试中常见"计算机网络最主要的功能是什么"的选择题。',
'计算机网络是利用通信线路和设备将多台计算机互连的系统，核心功能包括数据通信、资源共享、分布式处理和提高可靠性。数据通信是最基本的功能，资源共享是最重要的功能。',
5, '单元1 初识计算机网络', '任务1 走进计算机网络', 1075,
'四大功能记法："通资分可"——通(数据通信)资(资源共享)分(分布式处理)可(提高可靠性)。另记：最基本→数据通信，最重要→资源共享。',
'【必考】①网络的主要目的＝资源共享+数据通信 ②四大功能的区分：哪些是"通信"行为，哪些是"共享"行为 ③分布式处理的概念判断',
2,
'["计算机网络","网络功能","数据通信","资源共享","分布式处理"]',
'["计算机网络基本概念"]',
'[]',
'PUBLISHED');

-- 卡片10101~10104: 计算机网络的定义与功能
INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10101, 10101, '计算机网络的定义是什么？', '计算机网络是利用通信线路和设备，将地理上分散、具有独立功能的计算机互连，在网络软件支持下实现数据通信和资源共享的系统。', 1, 'DEFINITION'),
(10102, 10101, '计算机网络的四大功能是什么？', '①数据通信（最基本）②资源共享（最重要）③分布式处理④提高可靠性。记忆口诀："通资分可"。', 2, 'DEFINITION'),
(10103, 10101, '判断：网络中一台服务器故障导致整个网络瘫痪——这违反了哪个功能的设计目标？', '提高可靠性。网络设计应保证单点故障不影响整体，由其他设备接替工作。', 3, 'SCENARIO'),
(10104, 10101, '教室里有10台电脑共享一台打印机，这属于计算机网络的哪项功能？', '资源共享（硬件共享）。共享打印机、扫描仪等外设是局域网最常见的资源共享场景。', 4, 'APPLICATION');

-- ============================================================
-- 文章10102: 计算机网络的组成（node=1076）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10102, '计算机网络的组成——通信子网与资源子网',
'## 计算机网络的逻辑组成

计算机网络从逻辑功能上分为**两大子网**：

### 通信子网（内层）——"搬运工"
负责**数据传输和通信控制**，相当于网络的"骨架"。

| 组成 | 举例 |
|------|------|
| **传输介质** | 双绞线、同轴电缆、光纤、无线信道 |
| **通信设备** | 网卡、交换机、路由器、调制解调器 |

### 资源子网（外层）——"使用者"
负责**数据处理**和**提供共享资源**，相当于网络的"血肉"。

| 组成 | 举例 |
|------|------|
| **计算机** | 服务器、工作站、PC |
| **终端设备** | 打印机、扫描仪、摄像头 |
| **软件资源** | 操作系统、应用软件、数据库 |

### 两者的关系

```
用户 ←→ 资源子网（产生/使用数据）
                ↕  （通过网络接口连接）
        通信子网（传输数据）
```

- 通信子网是网络的内层，负责数据传输
- 资源子网是网络的外层，负责数据加工
- 两者通过**网络接口**（如网卡）连接

### 物理组成视角

从物理设备角度看，网络由以下组成：
1. **计算机**：服务器（提供服务）和客户机（使用服务）
2. **网络设备**：交换机、路由器、集线器
3. **传输介质**：网线、光纤、无线信号
4. **网络软件**：操作系统、协议软件、应用软件

> **考试易混淆点**：交换机属于**通信子网**（通信设备），打印机属于**资源子网**（终端设备）。做题时注意判断设备归属。',
'计算机网络从逻辑上分为通信子网（负责数据传输）和资源子网（负责数据处理）。通信子网包括传输介质和通信设备，资源子网包括计算机和终端设备。两者通过网络接口连接。',
5, '单元1 初识计算机网络', '任务1 走进计算机网络', 1076,
'"通信子网是搬运工，资源子网是使用者"\n\n通信子网≈通信设备+传输介质（只管搬）\n资源子网≈计算机+终端设备（只管用）',
'【必考】①子网归属判断——路由器属于通信子网，打印机属于资源子网 ②两种划分视角——逻辑组成vs物理组成',
2,
'["计算机网络组成","通信子网","资源子网","网络设备"]',
'["计算机网络基本概念","网络的组成"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10105, 10102, '通信子网由哪些部分组成？', '通信子网由传输介质（双绞线/光纤/同轴电缆）和通信设备（路由器/交换机/集线器/调制解调器）组成。记忆：通信子网是"搬运工"。', 1, 'DEFINITION'),
(10106, 10102, '资源子网由哪些部分组成？', '资源子网由计算机（服务器/工作站/PC）和终端设备（打印机/扫描仪）以及软件资源组成。记忆：资源子网是"使用者"。', 2, 'DEFINITION'),
(10107, 10102, '判断：路由器、交换机和打印机中，哪些属于通信子网？', '路由器和交换机属于通信子网（通信设备）。打印机属于资源子网（终端设备）。', 3, 'COMPARISON'),
(10108, 10102, '通信子网和资源子网通过什么连接？', '通过网络接口（如网卡NIC）连接。网卡既属于通信子网也连接着资源子网中的计算机。', 4, 'DEFINITION');

-- ============================================================
-- 文章10103: 计算机网络的分类（node=1077）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10103, '计算机网络的分类',
'## 按覆盖范围分类（最重要）

| 类型 | 英文 | 覆盖范围 | 典型场景 |
|:---:|:---:|:---------:|:---------:|
| **局域网** | LAN | 几十米~几千米 | 学校机房、办公室、家庭 |
| **城域网** | MAN | 几千米~几十千米 | 城市教育网、政务网 |
| **广域网** | WAN | 几百千米以上 | Internet、银行全国联网 |

**区分要点**：
- LAN范围最小，速度最快，属于私有网络
- WAN范围最大，速度相对较慢，通常使用租用线路
- Internet是全球最大的广域网，但不是唯一的广域网

## 按传输介质分类

| 类型 | 介质 | 特点 |
|:----:|:----:|:------:|
| **有线网** | 双绞线/光纤/同轴电缆 | 速度快、稳定、安全性高 |
| **无线网** | 无线电波/红外线/蓝牙 | 灵活方便、移动性好 |

## 按拓扑结构分类

| 拓扑 | 特点 | 优缺点 |
|:----:|:----:|:--------:|
| **总线型** | 共享一条总线 | 结构简单，故障难隔离 |
| **星型** | 中心节点连接各节点 | 单点故障不影响其余，**最常用** |
| **环型** | 首尾相连形成环路 | 无冲突，单点故障全网瘫痪 |
| **树型** | 星型的层次化扩展 | 易于扩展，上级故障影响下级 |
| **网状型** | 多点互连 | 可靠性最高，成本也最高 |

## 其他分类方式

| 分类标准 | 类型 |
|:---------:|:-----:|
| 按使用范围 | 公用网、专用网 |
| 按交换方式 | 电路交换、报文交换、分组交换 |
| 按传输速率 | 低速网、中速网、高速网 |

> **考试重点**：LAN/MAN/WAN的覆盖范围区分是必考题，星型拓扑是目前最常用的局域网拓扑。',
'计算机网络有三种主要分类方式：按覆盖范围（LAN/MAN/WAN）、按传输介质（有线/无线）、按拓扑结构（总线型/星型/环型/树型/网状型）。其中按覆盖范围的分类是考试重点。',
5, '单元1 初识计算机网络', '任务1 走进计算机网络', 1077,
'LAN小MAN中WAN大——局域网几十米到几千米，城域网几千米到几十千米，广域网几百千米以上。\n\n星型拓扑记法：星星在中间，大家都在旁边。',
'【必考】①LAN/MAN/WAN的覆盖范围和典型示例 ②星型拓扑的特点（最常用）③按拓扑结构的五种类型能根据描述判断类型',
2,
'["计算机网络分类","LAN","MAN","WAN","网络拓扑"]',
'["计算机网络基本概念","网络的分类","网络拓扑结构"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10109, 10103, 'LAN、MAN、WAN分别代表什么？覆盖范围如何？', 'LAN=局域网（几十米~几千米）；MAN=城域网（几千米~几十千米）；WAN=广域网（几百千米以上）。口诀：LAN小MAN中WAN大。', 1, 'DEFINITION'),
(10110, 10103, '五种网络拓扑结构有哪些？哪种最常用？', '总线型、星型（最常用）、环型、树型、网状型。星型拓扑是目前局域网最常用的结构。', 2, 'DEFINITION'),
(10111, 10103, '星型拓扑的优缺点是什么？', '优点：单节点故障不影响其他节点，易于维护扩展。缺点：中心节点故障全网瘫痪，线缆用量大。', 3, 'COMPARISON'),
(10112, 10103, '学校的计算机机房通常属于哪种类型的网络？', '局域网（LAN）。机房覆盖范围在一栋楼内，属于典型的小范围私有网络。', 4, 'APPLICATION');

-- ============================================================
-- 文章10104: 网络拓扑结构（node=1078）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10104, '网络拓扑结构——五种基本形态详解',
'## 什么是网络拓扑结构？

网络拓扑结构是指网络中计算机和通信设备的**物理连接方式**（布局形态）。不同拓扑结构影响网络的性能、可靠性和成本。

## 五种基本拓扑结构详解

### 1. 总线型拓扑

```
计算机1 ──── 计算机2 ──── 计算机3 ──── 计算机4
                         │
                       终端器
```

- **结构**：所有节点连接到一条共享总线上
- **通信方式**：任一时刻只允许一台设备发送数据
- **优点**：结构简单、布线容易、成本低
- **缺点**：总线故障导致全网瘫痪；故障隔离困难
- **应用**：早期的10Base-2以太网（已基本淘汰）

### 2. 星型拓扑 ⭐（最常用）

```
           计算机1
             │
   计算机2 ──交换机── 计算机3
             │
           计算机4
```

- **结构**：所有节点通过独立链路连接到中心设备（交换机/集线器）
- **优点**：单节点故障不影响其他节点；易于维护和扩展
- **缺点**：中心节点故障全网瘫痪；线缆用量大
- **应用**：**当前最常用的局域网拓扑**——学校机房、办公室网络

### 3. 环型拓扑

```
计算机1 ───→ 计算机2
   ↑              ↓
   计算机4 ←─── 计算机3
```

- **结构**：各节点首尾相连形成闭合环路，数据单向传输
- **优点**：无数据冲突，适合实时控制
- **缺点**：单节点故障全网瘫痪；扩展困难
- **应用**：令牌环网（Token Ring，已较少使用）

### 4. 树型拓扑

```
        根交换机
        │    │
   交换机1  交换机2
   │   │    │   │
  PC1 PC2  PC3 PC4
```

- **结构**：星型的层次化扩展，存在层级关系
- **优点**：易于扩展、分级管理
- **缺点**：上级节点故障影响下级
- **应用**：校园网、企业分支机构网络

### 5. 网状型拓扑

```
   计算机1 ═══ 计算机2
    ║  ║      ║  ║
   计算机3 ═══ 计算机4
```

- **结构**：节点之间多点连接，形成网状
- **优点**：可靠性极高、负载均衡
- **缺点**：成本高、布线复杂
- **应用**：Internet骨干网、军事通信

## 各拓扑优缺点对比表

| 拓扑 | 可靠性 | 成本 | 扩展性 | 维护难易 |
|:----:|:-----:|:----:|:------:|:--------:|
| 总线型 | 低 | 低 | 中 | 难 |
| 星型 ⭐ | 中 | 中 | 易 | 易 |
| 环型 | 低 | 低 | 难 | 中 |
| 树型 | 中 | 中高 | 易 | 中 |
| 网状型 | 高 | 高 | 中 | 难 |

> **考试重点**：能根据描述判断拓扑类型（如"中心节点故障全网瘫痪"→星型），掌握各拓扑优缺点对比。',
'五种基本网络拓扑结构：总线型（共享总线）、星型（中心节点）、环型（闭合环路）、树型（层次化星型）、网状型（多点互连）。星型最常用，网状可靠性最高。',
5, '单元1 初识计算机网络', '任务1 走进计算机网络', 1078,
'星型像星星——中心发光，四周亮\n总线像一条线——大家都挂着\n环型像手拉手——围成一圈\n树型像家谱——有层级\n网状像蜘蛛网——谁都能连谁',
'【必考】①能根据特征描述判断拓扑类型 ②各拓扑的优缺点对比 ③星型拓扑是当前局域网最常用、最实用的拓扑结构',
2,
'["网络拓扑","总线型","星型","环型","树型","网状型"]',
'["网络拓扑结构"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10113, 10104, '星型拓扑的优缺点有哪些？', '优点：①单节点故障不影响其他节点 ②易于维护和扩展。缺点：①中心节点故障全网瘫痪 ②线缆用量大。口诀：中心出事全网倒。', 1, 'COMPARISON'),
(10114, 10104, '网状型拓扑的最大优点和最大缺点分别是什么？', '最大优点：可靠性极高（有多条冗余路径）。最大缺点：成本高、布线复杂。应用场景：Internet骨干网、军事通信。', 2, 'COMPARISON'),
(10115, 10104, '学校的校园网通常采用哪种拓扑结构？为什么？', '树型拓扑或星型拓扑。校园网分层设计：核心层→汇聚层→接入层，树型拓扑支持这种分级管理架构，便于扩展和维护。', 3, 'APPLICATION'),
(10116, 10104, '判断拓扑类型："所有节点共享一条通信线路，任一时刻只允许一台设备发送数据。"', '总线型拓扑。总线型拓扑中所有节点都连接到同一根总线上，采用共享信道方式通信。', 4, 'SCENARIO');

-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务2 认识数据通信（知识点1079）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10105: 数据通信基本概念（node=1079）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10105, '数据通信基本概念——信号类型与通信方式',
'## 什么是数据通信？

数据通信是指通过传输介质在计算机之间传送**数字信号**或**模拟信号**的过程。

## 信号类型

| 类型 | 特点 | 波形 | 示例 |
|:----:|:----:|:----:|:----:|
| **模拟信号** | 连续变化的电信号 | 正弦波 | 电话语音、广播 |
| **数字信号** | 离散的脉冲（0和1） | 方波 | 计算机内部数据 |

**区别要点**：模拟信号是连续变化的（像流水），数字信号是离散跳变的（像台阶）。

## 通信方式（按传输方向）

| 方式 | 方向 | 类比 | 示例 |
|:----:|:----:|:----:|:----:|
| **单工** 📺 | 只能单向 | 广播电台→听众 | 电视广播、寻呼系统 |
| **半双工** 📻 | 双向但不可同时 | 对讲机 | 对讲机、集线器 |
| **全双工** ☎️ | 双向可同时 | 电话 | 电话、交换机连接 |

### 区分技巧
- 单工 = 一路单向（只听不说）
- 半双工 = 双向轮流（你说我听，我说你听）
- 全双工 = 双向同时（边说边听）

## 带宽

| 概念 | 说明 |
|:----:|:------|
| **定义** | 通信线路传输数据的能力 |
| **单位** | bps（比特/秒） |
| **换算** | 1Kbps=1000bps, 1Mbps=1000Kbps, 1Gbps=1000Mbps |
| **宽带** | 速率≥2Mbps的数据传输 |

> **考试要点**：三种通信方向的定义及生活类比是高频考点。注意"全双工"不等于"半双工"，区别在于是否可同时传输。',
'数据通信涉及信号类型（模拟/数字）、通信方向（单工/半双工/全双工）和带宽等基本概念。三种通信方式的区分是考试重点，常以生活设备进行类比考查。',
5, '单元1 初识计算机网络', '任务2 认识数据通信', 1079,
'单工→广播（只听不说的单向）\n半双工→对讲机（轮流你说我/我说你）\n全双工→电话（双方同时说）',
'【必考】①模拟信号vs数字信号的区分 ②三种通信方式（单工/半双工/全双工）的判别——常给生活设备问属于哪种',
1,
'["数据通信","信号类型","模拟信号","数字信号","单工","半双工","全双工"]',
'["计算机网络基本概念"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10117, 10105, '模拟信号和数字信号有什么区别？', '模拟信号是连续变化的（如正弦波），适合传输语音；数字信号是离散跳变的（如方波的0和1），适合计算机处理。记忆：模拟像流水，数字像台阶。', 1, 'COMPARISON'),
(10118, 10105, '单工、半双工、全双工分别对应哪些生活设备？', '单工→广播/电视（单向）；半双工→对讲机（轮流）；全双工→电话（同时双向）。记忆口诀："广半电"——广播单工，对讲机半双工，电话全双工。', 2, 'APPLICATION'),
(10119, 10105, '带宽的单位是什么？如何换算？', '带宽的单位是bps（比特/秒）。换算：1Kbps=1000bps，1Mbps=1000Kbps，1Gbps=1000Mbps。注意计算机存储用Byte（字节），1Byte=8bit。', 3, 'DEFINITION');

-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务3 剖析计算机网络体系结构（知识点1080~1082）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10106: OSI七层模型及各层功能（node=1080）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10106, 'OSI七层模型——从物理层到应用层',
'## OSI参考模型概述

OSI（Open System Interconnection，开放系统互连）是**国际标准化组织（ISO）** 制定的网络通信标准模型。它将网络通信划分为**七层**，从下到上依次为：

## 七层结构详解（从下到上）

| 层号 | 名称 | 英文 | 核心功能 | 数据单元 | 典型协议/设备 |
|:---:|:----:|:----:|:---------:|:--------:|:-------------:|
| 7 | **应用层** | Application | 为用户提供网络服务接口 | 报文 | HTTP、FTP、SMTP、DNS |
| 6 | **表示层** | Presentation | 数据格式转换、加密解密、压缩 | 报文 | SSL/TLS、JPEG、MPEG |
| 5 | **会话层** | Session | 建立/管理/终止会话 | 报文 | NetBIOS、RPC |
| 4 | **传输层** | Transport | 端到端可靠传输、流量控制、差错控制 | **段** | TCP、UDP |
| 3 | **网络层** | Network | 路由选择、逻辑寻址（IP）、拥塞控制 | **包** | IP、ICMP、ARP、路由器 |
| 2 | **数据链路层** | Data Link | 帧封装、物理寻址（MAC）、差错检测 | **帧** | Ethernet、PPP、交换机、网卡 |
| 1 | **物理层** | Physical | 比特流传输、定义电气/机械特性 | **比特** | 双绞线、光纤、Hub、Modem |

### 各层职责速记

| 层 | 一句话理解 |
|:--:|:----------:|
| 物理层 | "传比特"——在介质上传输0和1 |
| 数据链路层 | "传帧"——在相邻节点间传输数据帧 |
| 网络层 | "选路"——为数据包选择最佳路径 |
| 传输层 | "保可靠"——确保数据端到端可靠到达 |
| 会话层 | "管对话"——建立和管理通信会话 |
| 表示层 | "做翻译"——数据格式转换和加密 |
| 应用层 | "用网络"——为用户提供网络应用服务 |

## 核心原则

- **对等通信**：每一层只与对等端同一层"对话"，使用相同的协议
- **逐层封装**：发送端从上层到下层层层加头部，接收端从下层到上层层层解封
- **下层为上层服务**：下层为上层提供透明的数据传输服务

> **高频考点**：能按顺序默写七层名称，知道每层对应的典型协议和设备。传输层的数据单元是"段"，网络层是"包"，数据链路层是"帧"，物理层是"比特"。',
'OSI参考模型是ISO制定的七层网络通信标准，从下到上依次为：物理层、数据链路层、网络层、传输层、会话层、表示层、应用层。每层有特定的功能、数据单元和协议。',
5, '单元1 初识计算机网络', '任务3 剖析计算机网络体系结构', 1080,
'口诀："物数网传会表应"（从下到上）\n"物联网络传话表演"——物理层/数据链路层/网络层/传输层/会话层/表示层/应用层\n\n数据单元记法：比(特)→帧→包→段→报文',
'【必考】①七层名称按顺序默写 ②每层对应的协议（如HTTP→应用层，TCP→传输层，IP→网络层） ③数据单元（段/包/帧/比特）的层级归属',
3,
'["OSI参考模型","OSI七层","网络体系结构","ISO"]',
'["网络体系结构","OSI参考模型"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10120, 10106, 'OSI七层模型从下到上的顺序是什么？', '物理层→数据链路层→网络层→传输层→会话层→表示层→应用层。口诀："物数网传会表应"。', 1, 'DEFINITION'),
(10121, 10106, 'HTTP、TCP、IP分别属于OSI的哪一层？', 'HTTP→应用层（第7层）；TCP→传输层（第4层）；IP→网络层（第3层）。', 2, 'APPLICATION'),
(10122, 10106, 'OSI各层的数据单元分别叫什么？', '物理层→比特（bit）；数据链路层→帧（frame）；网络层→包（packet）；传输层→段（segment）；上三层→报文（message）。', 3, 'DEFINITION'),
(10123, 10106, '路由器、交换机、集线器各工作在OSI哪一层？', '路由器→网络层（第3层，基于IP地址转发）；交换机→数据链路层（第2层，基于MAC地址转发）；集线器→物理层（第1层，广播转发）。', 4, 'COMPARISON');

-- ============================================================
-- 文章10107: TCP/IP四层模型（node=1081）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10107, 'TCP/IP四层模型——Internet的实际标准',
'## TCP/IP模型概述

TCP/IP（Transmission Control Protocol/Internet Protocol）是**Internet的实际运行标准**，由IETF（互联网工程任务组）制定。TCP/IP模型采用**四层结构**，比OSI更简洁。

## 四层结构详解（从下到上）

| 层号 | 名称 | 核心功能 | 对应协议 |
|:---:|:----:|:---------:|:---------:|
| 4 | **应用层** | 为应用程序提供网络服务 | HTTP、FTP、SMTP、DNS、DHCP、Telnet |
| 3 | **传输层** | 端到端数据传输，提供可靠/不可靠服务 | **TCP**、**UDP** |
| 2 | **网络层** | IP寻址和路由选择 | **IP**、ICMP、ARP、RARP |
| 1 | **网络接口层** | 物理介质访问和数据帧传输 | Ethernet、Wi-Fi、PPP、ATM |

## TCP/IP核心协议

### TCP（传输控制协议）
- **面向连接**：通信前先建立连接（三次握手）
- **可靠传输**：有确认重传机制
- **应用**：网页浏览（HTTP）、文件传输（FTP）、电子邮件（SMTP）

### UDP（用户数据报协议）
- **无连接**：直接发送数据，不需建立连接
- **不可靠但高效**：无确认重传机制，速度快
- **应用**：视频直播、在线游戏、DNS查询

### IP（网际协议）
- 负责数据包的**路由选择**和**转发**
- 提供**无连接**的数据报传输服务
- 使用**IP地址**进行逻辑寻址

### ICMP（互联网控制报文协议）
- 用于传递**错误报告**和**网络诊断**信息
- 典型应用：ping命令基于ICMP协议

### ARP（地址解析协议）
- 将**IP地址**解析为**MAC地址**
- 广播发送ARP请求，获取目标MAC地址

## 各层典型设备与协议汇总

| 层次 | 设备 | 协议 |
|:----:|:----:|:----:|
| 应用层 | 计算机、服务器 | HTTP、FTP、SMTP、DNS |
| 传输层 | —（软件实现） | TCP、UDP |
| 网络层 | 路由器 | IP、ICMP、ARP |
| 网络接口层 | 交换机、网卡、Hub | Ethernet、Wi-Fi |

> **考试重点**：TCP/IP四层的名称和顺序、各层典型协议、TCP与UDP的区别。',
'TCP/IP是Internet实际运行的四层网络模型：网络接口层→网络层→传输层→应用层。核心协议包括TCP（可靠）、UDP（高效）、IP（路由）。与OSI七层相比更简洁实用。',
5, '单元1 初识计算机网络', '任务3 剖析计算机网络体系结构', 1081,
'TCP/IP四层记法："接网传应"——接(网络接口层)网(网络层)传(传输层)应(应用层)\n\nTCP可靠记法：TCP=Three-way handshake + Confirm + Protocol（三次握手+确认=可靠）\nUDP快速记法：U=Unreliable（不可靠）但快',
'【必考】①TCP/IP四层名称和顺序 ②TCP与UDP的核心区别（面向连接vs无连接，可靠vs高效） ③典型协议归属',
3,
'["TCP/IP","TCP","UDP","IP","网络协议","传输层","网络层"]',
'["网络体系结构","TCP/IP模型"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10124, 10107, 'TCP/IP四层模型从下到上的顺序是什么？', '网络接口层→网络层→传输层→应用层。口诀："接网传应"。对应OSI：下两层合并为网络接口层，上三层合并为应用层。', 1, 'DEFINITION'),
(10125, 10107, 'TCP和UDP的区别是什么？', 'TCP：面向连接（三次握手）、可靠（确认重传）、速度较慢。UDP：无连接、不可靠、速度快。场景对比：TCP→HTTP/FTP，UDP→视频直播/DNS。', 2, 'COMPARISON'),
(10126, 10107, 'ARP协议的作用是什么？', 'ARP（地址解析协议）将IP地址解析为MAC地址。当计算机知道目标IP但不知道其MAC地址时，通过ARP广播获取。', 3, 'DEFINITION'),
(10127, 10107, 'ping命令基于哪个协议？', 'ping命令基于ICMP（互联网控制报文协议）。ICMP位于网络层，用于传递网络诊断信息和错误报告。', 4, 'APPLICATION');

-- ============================================================
-- 文章10108: OSI与TCP/IP对应关系（node=1082）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10108, 'OSI与TCP/IP模型的对应关系',
'## 两个模型的层次对应

```
OSI七层                     TCP/IP四层
─────────────────            ─────────────────
应用层  ┐
表示层  ├──→  应用层 (HTTP/FTP/SMTP/DNS)
会话层  ┘
─────────────────            ─────────────────
传输层  ────→  传输层 (TCP/UDP)
─────────────────            ─────────────────
网络层  ────→  网络层 (IP/ICMP/ARP)
─────────────────            ─────────────────
数据链路层 ┐
          ├──→  网络接口层 (Ethernet/Wi-Fi/PPP)
物理层    ┘
```

### 对应关系要点
| OSI七层 | TCP/IP四层 | 说明 |
|:-------:|:----------:|:-----:|
| 应用层+表示层+会话层 | → **应用层** | OSI上层三层合并为TCP/IP的应用层 |
| 传输层 | → **传输层** | 一一对应，TCP/UDP相同 |
| 网络层 | → **网络层** | 一一对应，IP协议相同 |
| 数据链路层+物理层 | → **网络接口层** | OSI下两层合并为TCP/IP的网络接口层 |

## 两个模型详细对比

| 对比维度 | OSI参考模型 | TCP/IP模型 |
|:---------:|:-----------:|:-----------:|
| **层数** | 7层 | 4层 |
| **制定者** | ISO（国际标准化组织） | IETF（互联网工程任务组） |
| **制定时间** | 1984年（先于实现） | 1970年代（先有协议后有模型） |
| **性质** | **理论标准**（教学用） | **实际工业标准**（Internet用） |
| **上层划分** | 应用层、表示层、会话层独立 | 三层合并为应用层 |
| **下层划分** | 数据链路层、物理层独立 | 两层合并为网络接口层 |
| **实际应用** | 主要用于教学和理论分析 | Internet实际运行 |

## 记忆方法

| 提示 | 对应关系 |
|:----:|:---------:|
| OSI＝"理论范本"——标准但复杂 |
| TCP/IP＝"实战派"——实用但简洁 |
| OSI上三层→TCP/IP应用层 | "上合三为一" |
| OSI下两层→TCP/IP网络接口层 | "下合二为一" |
| OSI传输层/网络层→对应TCP/IP | "中间两对两" |

> **高频考点**：OSI 7层对应TCP/IP 4层的映射关系（哪几层合并了）、两个模型各自的层数。',
'OSI七层与TCP/IP四层的对应关系：OSI的上三层（应用/表示/会话）合并为TCP/IP的应用层，OSI的下两层（数据链路/物理）合并为网络接口层，中间两层一一对应。OSI是理论标准，TCP/IP是实际标准。',
5, '单元1 初识计算机网络', '任务3 剖析计算机网络体系结构', 1082,
'"上三合一下二合一"——OSI的上三层合并为TCP/IP的应用层，下两层合并为网络接口层。\n\nOSI=7层=理论标准，TCP/IP=4层=实际标准',
'【必考】①两个模型的层数差异 ②对应关系——哪几层合并了 ③OSI是理论标准、TCP/IP是实际标准',
2,
'["OSI","TCP/IP","网络体系结构","模型对比"]',
'["网络体系结构","OSI参考模型","TCP/IP模型"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10128, 10108, 'OSI七层和TCP/IP四层是如何对应的？', '上三合一下二合一：OSI应用/表示/会话→TCP/IP应用层；OSI传输层→TCP/IP传输层；OSI网络层→TCP/IP网络层；OSI数据链路/物理层→TCP/IP网络接口层。', 1, 'COMPARISON'),
(10129, 10108, 'OSI模型和TCP/IP模型分别有多少层？', 'OSI有7层（物理/数据链路/网络/传输/会话/表示/应用），TCP/IP有4层（网络接口/网络/传输/应用）。', 2, 'COMPARISON'),
(10130, 10108, '为什么说OSI是理论标准而TCP/IP是实际标准？', 'OSI由ISO制定，先有模型后有协议实现，主要用于教学和理论分析。TCP/IP由IETF制定，先有协议（TCP/IP协议族）后有模型，是Internet实际运行的工业标准。', 3, 'COMPARISON');

-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务4 认识传输介质（知识点1083~1084）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10109: 有线传输介质（node=1083）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10109, '有线传输介质——双绞线、同轴电缆与光纤',
'## 常见有线传输介质

### 1. 双绞线（Twisted Pair）⭐最常用

**结构**：两根绝缘铜线按一定密度绞合在一起，目的是减少电磁干扰。

**分类**：

| 类型 | 特点 | 应用 |
|:----:|:----:|:----:|
| **UTP**（非屏蔽双绞线） | 无金属屏蔽层，成本低 | **最常用**，办公室/家庭布线 |
| **STP**（屏蔽双绞线） | 有金属屏蔽层，抗干扰强 | 工业环境、强干扰区域 |

**关键参数**：
- **接口**：RJ-45（8芯水晶头），俗称"网口"
- **最大传输距离**：**100米**（超长需加中继器）
- **常用类别**：Cat5e（百兆）、Cat6（千兆）、Cat6a（万兆）

**T568A/T568B线序**（应会技能）：
- T568B（最常用）：白橙/橙/白绿/蓝/白蓝/绿/白棕/棕
- T568A：白绿/绿/白橙/蓝/白蓝/橙/白棕/棕
> 直通线：两端线序相同（PC↔交换机）
> 交叉线：两端线序不同（PC↔PC，已基本被MDI/MDIX自动翻转替代）

### 2. 同轴电缆（Coaxial Cable）

**结构**：内导体→绝缘层→金属屏蔽层→外层护套（四层结构）

**特点**：
- 抗干扰能力优于双绞线（有金属屏蔽层）
- 成本高于双绞线

**应用**：
- 有线电视（CATV）
- 早期以太网标准：10Base-2（细缆）、10Base-5（粗缆）——已淘汰

### 3. 光纤（Optical Fiber）⭐传输性能最佳

**原理**：利用**光的全反射**原理在纤芯中传输光信号。

**分类对比**：

| 类型 | 光源 | 纤芯直径 | 传输距离 | 成本 |
|:----:|:----:|:---------:|:---------:|:----:|
| **单模光纤** | 激光 | 小（约9μm） | 几十千米 | 高 |
| **多模光纤** | LED | 大（约50/62.5μm） | 几百米~2千米 | 较低 |

**光纤的优缺点**：
- ✅ 传输速度快、带宽大
- ✅ 传输距离远
- ✅ 抗电磁干扰（光信号不受电磁影响）
- ✅ 保密性好（不易被窃听）
- ❌ 成本较高
- ❌ 连接和安装技术要求高

**应用**：骨干网、跨洋通信、FTTH（光纤到户）

> **高频考点**：双绞线最大距离100m、光纤的最大优点（抗干扰/远距离/高带宽）、单模与多模的区别——单模用激光、距离远、成本高。',
'三种有线传输介质：双绞线（UTP/STP，100m，RJ-45接口）、同轴电缆（抗干扰强，有线电视用）、光纤（单模/多模，光全反射原理，速度最快距离最远）。双绞线最常用。',
5, '单元1 初识计算机网络', '任务4 认识传输介质', 1083,
'双绞线：100米，RJ-45，UTP最常用\n光纤：全反射传输，单模激光远，多模LED近\n同轴：四层结构，有线电视\n\n双绞线距离记法："百米双绞线"',
'【必考】①双绞线最大传输距离100米 ②光纤的最大特点（抗干扰/远距离）③单模vs多模的区别 ④直通线vs交叉线的使用场景',
2,
'["传输介质","双绞线","UTP","光纤","单模光纤","多模光纤","同轴电缆","RJ-45"]',
'["网络传输介质与设备","有线传输介质"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10131, 10109, '双绞线的最大传输距离是多少？接口是什么？', '最大传输距离100米。接口为RJ-45（8芯水晶头）。当需要超过100米时，必须使用中继器（如交换机）延长。', 1, 'DEFINITION'),
(10132, 10109, 'UTP和STP有什么区别？', 'UTP（非屏蔽双绞线）：无金属屏蔽层，成本低，最常用。STP（屏蔽双绞线）：有金属屏蔽层，抗干扰能力强，成本高，用于工业等强干扰环境。', 2, 'COMPARISON'),
(10133, 10109, '光纤的优点有哪些？', '①传输速度快/带宽大 ②传输距离远 ③抗电磁干扰（光信号不受电磁影响）④保密性好（不易被窃听）。口诀："快远抗密"——快、远、抗干扰、保密。', 3, 'DEFINITION'),
(10134, 10109, '直通线和交叉线分别用于什么场景？', '直通线（两端线序相同）：用于连接不同类型的设备（PC↔交换机，路由器↔交换机）。交叉线（两端线序不同）：用于连接同类型设备（PC↔PC），现代设备已可自动翻转。', 4, 'APPLICATION');

-- ============================================================
-- 文章10110: 无线传输介质（node=1084）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10110, '无线传输介质——无线电波、Wi-Fi与蓝牙',
'## 常见的无线传输介质

| 介质类型 | 频段 | 传输距离 | 典型应用 |
|:---------:|:----:|:---------:|:---------:|
| **无线电波** | 3kHz~300GHz | 视功率而定 | 广播、电视、移动通信 |
| **微波** | 1GHz~30GHz | 几十千米（需视距） | 卫星通信、微波中继 |
| **红外线** | 300GHz~400THz | 几米 | 遥控器、短距离通信 |
| **蓝牙** | 2.4GHz ISM频段 | 约**10米** | 无线耳机、手机互联 |
| **Wi-Fi** | 2.4GHz / 5GHz | 约100米（室内） | 无线局域网（WLAN） |

## Wi-Fi（无线保真）

Wi-Fi基于 **IEEE 802.11** 标准系列，是目前最广泛的无线联网技术。

| 标准 | 频段 | 理论速率 | 说明 |
|:----:|:----:|:---------:|:----:|
| 802.11b | 2.4GHz | 11Mbps | 早期标准 |
| 802.11g | 2.4GHz | 54Mbps | 兼容b |
| 802.11n（Wi-Fi 4） | 2.4/5GHz | 可达600Mbps | 使用MIMO技术 |
| 802.11ac（Wi-Fi 5） | 5GHz | 可达1Gbps+ | 当前主流 |
| 802.11ax（Wi-Fi 6） | 2.4/5GHz | 可达9.6Gbps | 最新一代 |

**热点（Hotspot）**：提供无线网络接入的地点，如咖啡馆、机场、学校的Wi-Fi接入点。

## 蓝牙（Bluetooth）

- **工作频段**：2.4GHz ISM频段
- **传输距离**：约**10米**（Class 2标准）
- **特点**：低功耗、短距离、设备互联
- **应用**：无线耳机、键盘鼠标、手机互联

## 无线 vs 有线对比

| 维度 | 无线 | 有线 |
|:----:|:----:|:----:|
| **灵活性** | ✅ 移动方便，不受线缆束缚 | ❌ 受线缆长度和位置限制 |
| **传输速度** | ❌ 受环境干扰，速度波动 | ✅ 稳定高速（千兆/万兆） |
| **安全性** | ❌ 信号可被截获窃听 | ✅ 物理传输，相对安全 |
| **安装成本** | ✅ 无需布线，安装方便 | ❌ 布线成本高，维护复杂 |

> **考试重点**：蓝牙最大距离约10m、Wi-Fi频段（2.4GHz/5GHz）、无线介质的分类。',
'常见无线传输介质包括无线电波、微波、红外线、蓝牙（约10m）和Wi-Fi（2.4/5GHz，约100m）。Wi-Fi基于IEEE 802.11标准，是目前最主流的无线联网技术。',
5, '单元1 初识计算机网络', '任务4 认识传输介质', 1084,
'蓝牙≈10米记法："蓝牙一室"——一个房间内（约10米）\nWi-Fi频段记法："24和5"——2.4GHz和5GHz双频\n无线介质分类：无线电→微波→红外→蓝牙→Wi-Fi（距离从远到近排列）',
'【必考】①蓝牙传输距离≈10米 ②Wi-Fi的频段（2.4GHz/5GHz） ③无线vs有线的主要优缺点对比',
1,
'["无线传输介质","Wi-Fi","蓝牙","无线电波","微波","红外线","802.11"]',
'["网络传输介质与设备","无线传输介质"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10135, 10110, '蓝牙的传输距离大约是多少？', '蓝牙（Class 2）的典型传输距离约为10米。常用于无线耳机、键盘鼠标等短距离设备互联。', 1, 'DEFINITION'),
(10136, 10110, 'Wi-Fi使用哪些频段？各有什么特点？', '①2.4GHz频段：覆盖范围广、穿墙能力强，但干扰多（蓝牙/微波炉同频）②5GHz频段：速度快、干扰少，但穿墙能力弱。现代路由器多支持双频切换。', 2, 'COMPARISON'),
(10137, 10110, '无线网络相比有线网络有哪些优缺点？', '优点：移动灵活、免布线、安装方便。缺点：速度受环境影响大、安全性较低（信号可被截获）、稳定性不如有线。', 3, 'COMPARISON'),
(10138, 10110, '教室部署无线网络，学生反映网速慢而稳定，最可能的原因是什么？', '同频干扰（如相邻教室Wi-Fi信道重叠）或接入设备过多。可通过切换5GHz频段或调整信道改善。', 4, 'SCENARIO');

-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务5 认识网络接口及网络设备（知识点1085~1089）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10111: 网卡与MAC地址（node=1085）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10111, '网卡（NIC）与MAC地址——网络的身份标识',
'## 什么是网卡？

网卡（Network Interface Card，NIC）是计算机与网络之间的**接口设备**，也称为**网络适配器**。

### 网卡的主要功能
1. **数据封装**：将计算机数据封装成帧，加上源MAC和目标MAC地址
2. **介质访问控制**：遵循CSMA/CD等协议规则控制数据发送时机
3. **信号转换**：并行数据↔串行数据的转换
4. **数据缓冲**：暂存数据，匹配计算机与网络之间的速度差异

### 网卡的类型
| 分类 | 类型 |
|:----:|:----:|
| 按接口类型 | PCIe网卡、USB网卡、板载网卡（集成在主板上）|
| 按传输速率 | 百兆网卡、千兆网卡、万兆网卡 |
| 按传输介质 | 有线网卡、无线网卡 |

## MAC地址（物理地址）

### 基本概念
- **全称**：Media Access Control Address（介质访问控制地址）
- **长度**：**48位**二进制 = 6字节
- **表示**：12位**十六进制**数，每2位一组用"-"或":"分隔
- **示例**：`00-1A-2B-3C-4D-5E`

### MAC地址结构
```
│────────── 48位 ──────────│
├──前24位（厂商ID/OUI）───┼──后24位（设备序列号）──┤
│     00-1A-2B            │        3C-4D-5E        │
│    IEEE分配给厂商        │     厂商自编，唯⼀     │
```

- **前24位**（3字节）：组织唯一标识符（OUI），由IEEE统一分配给设备厂商
- **后24位**（3字节）：由厂商自编，确保同一厂商的不同设备不重复

### MAC地址的特点
- **全球唯一**：理论上每个网卡的MAC地址在全球范围内不重复
- **出厂烧录**：在网卡制造时写入ROM中，不可修改（软件层面可"伪装"）
- **物理地址**：工作在数据链路层，用于**局域网内部**的设备标识

### MAC vs IP 对比

| 对比维度 | MAC地址 | IP地址 |
|:--------:|:--------:|:--------:|
| **长度** | 48位 | 32位（IPv4）|
| **表示** | 十六进制（如00-1A-2B-3C-4D-5E） | 点分十进制（如192.168.1.1）|
| **层次** | 数据链路层（第2层） | 网络层（第3层）|
| **作用域** | 局域网内部 | 全球网络（Internet）|
| **是否可变** | 出厂固定（理论上全球唯一） | 可手动配置或DHCP分配 |
| **类比** | 身份证号（终身不变） | 家庭住址（可以搬家）|

> **高频考点**：MAC地址48位、十六进制表示、前24位是厂商ID。MAC地址是"物理地址/硬件地址"，IP地址是"逻辑地址"。',
'网卡（NIC）是计算机连接网络的接口设备。MAC地址是48位物理地址，用12位十六进制表示，前24位是厂商ID（OUI），后24位是设备序列号。MAC地址全球唯一、出厂烧录，工作在数据链路层。',
5, '单元1 初识计算机网络', '任务5 认识网络接口及网络设备', 1085,
'MAC地址记法：\n48位=6字节=12个十六进制数\n"48=6×8"——48位共6字节，每字节8位\n\nMAC vs IP：MAC是身份证号（终身不变），IP是家庭住址（可以搬家）',
'【必考】①MAC地址长度48位 ②MAC地址用十六进制表示（12位）③MAC地址的结构（前24位厂商ID+后24位序列号）④MAC是物理地址/硬件地址',
2,
'["网卡","NIC","MAC地址","物理地址","网络适配器"]',
'["网络传输介质与设备","网卡"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10139, 10111, 'MAC地址有多少位？用什么进制表示？', '48位二进制，用12位十六进制表示。格式如：00-1A-2B-3C-4D-5E（每2位一组用"-"或":"分隔）。', 1, 'DEFINITION'),
(10140, 10111, 'MAC地址的前24位和后24位分别代表什么？', '前24位：厂商ID（OUI），由IEEE统一分配给厂商。后24位：设备序列号，由厂商自编保证唯一。', 2, 'DEFINITION'),
(10141, 10111, 'MAC地址和IP地址有什么区别？', 'MAC=48位、十六进制、物理地址（数据链路层）、局域网使用、出厂固定。IP=32位、十进制、逻辑地址（网络层）、Internet使用、可配置。类比：MAC是身份证号，IP是家庭住址。', 3, 'COMPARISON'),
(10142, 10111, '为什么说MAC地址是"物理地址"？', '因为MAC地址被烧录在网卡的ROM中，出厂时已固化，与硬件绑定。虽然可软件伪装，但理论上全球唯一且不可修改。', 4, 'DEFINITION');

-- ============================================================
-- 文章10112: 集线器（node=1086）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10112, '集线器（Hub）——物理层广播设备',
'## 什么是集线器？

集线器（Hub）是计算机网络中一种**物理层设备**（OSI第1层），用于将多台计算机连接起来形成网络。

### 工作特点
- **工作层级**：物理层（仅处理比特流，不解析数据内容）
- **工作原理**：从一个端口收到数据后，**向所有其他端口广播转发**
- **带宽分配**：**共享带宽**——所有端口共享总带宽

### Hub的工作方式图解
```
计算机A ↝ 数据(发往D)
    │
    ├──→ Hub ──→ 计算机B（收到，查看不是自己的，丢弃）
    │        │──→ 计算机C（收到，查看不是自己的，丢弃）
    │        │──→ 计算机D（收到，是自己的，处理！）
    │
所有端口都收到了数据！
```

### Hub的优缺点

| 优点 | 缺点 |
|:----:|:----:|
| ✅ 价格低廉 | ❌ 任意时刻只能一台设备发送数据 |
| ✅ 即插即用（无需配置） | ❌ 共享带宽，效率低 |
| ✅ 结构简单 | ❌ 广播式转发，存在安全隐患 |
| | ❌ 半双工通信（不能同时收发） |

### Hub vs Switch 对比

| 对比维度 | 集线器（Hub） | 交换机（Switch） |
|:--------:|:------------:|:----------------:|
| **工作层** | 物理层（L1） | 数据链路层（L2） |
| **转发方式** | **广播转发**（所有端口） | **定向转发**（查MAC地址表） |
| **带宽** | 共享（8口百兆=全共用100M） | 独占（8口百兆=每口100M） |
| **通信模式** | 半双工 | 全双工 |
| **安全性** | 低（数据所有端口可见） | 高（仅目标端口收到） |
| **当前状态** | 基本被淘汰 | 全面普及 |

> **考试重点**：Hub是物理层设备、广播转发、共享带宽。对比交换机是数据链路层、定向转发、独享带宽。',
'集线器（Hub）是物理层设备，通过广播方式转发数据（所有端口都能收到）。主要缺点是共享带宽和安全低。已基本被交换机取代，但考试中仍是重要对比对象。',
5, '单元1 初识计算机网络', '任务5 认识网络接口及网络设备', 1086,
'Hub=物理层+广播+共享带宽\n三个"共"：共享带宽、广播公共、半双工共同等待',
'【必考】①Hub工作在哪一层（物理层）②转发方式（广播）③带宽分配方式（共享）④与交换机的对比',
1,
'["集线器","Hub","物理层","广播"]',
'["网络传输介质与设备","集线器"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10143, 10112, '集线器（Hub）工作在哪一层？转发方式是什么？', 'Hub工作在物理层（第1层）。转发方式是广播转发——收到的数据向所有其他端口发送，所有连接设备都能收到。', 1, 'DEFINITION'),
(10144, 10112, '集线器的带宽分配方式是什么？', '共享带宽。例如一个8口100Mbps Hub，所有8台计算机加起来共享100Mbps，同时只能一台发送数据。', 2, 'DEFINITION'),
(10145, 10112, '集线器和交换机在转发方式上有什么区别？', 'Hub：广播转发（向所有端口发），工作在物理层。Switch：定向转发（查MAC地址表找到目标端口），工作在数据链路层。Hub是"喇叭"（喊给所有人听），Switch是"快递员"（准确送达）。', 3, 'COMPARISON');

-- ============================================================
-- 文章10113: 交换机（node=1087）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10113, '交换机（Switch）——局域网的核心设备',
'## 什么是交换机？

交换机（Switch）是**数据链路层设备**（OSI第2层），是目前局域网中**最核心的网络设备**。它根据**MAC地址表**进行定向数据转发。

## 交换机的工作原理

交换机的工作分为三个步骤，称为"**学习—转发—泛洪**"机制：

### 步骤1：学习（Learning）
```
PC-A（MAC: AA-AA-AA）←→ 端口1┐
                              ├ Switch ─ 端口2 → PC-B（MAC: BB-BB-BB）
PC-D（MAC: DD-DD-DD）←→ 端口4┘
```
当PC-A（端口1）发送数据给PC-B时，交换机：
1. 记录：**端口1 → AA-AA-AA**（源MAC地址）
2. 此时MAC地址表：`{AA-AA-AA → 端口1}`

### 步骤2：转发（Forwarding）
3. 查找目标MAC地址 BB-BB-BB → 不在表中
4. 向所有端口（除端口1外）**泛洪**转发

当PC-B回送数据时：
5. 记录：**端口2 → BB-BB-BB**（源MAC地址）
6. 此时MAC地址表：`{AA-AA-AA → 端口1, BB-BB-BB → 端口2}`

### 步骤3：定向转发
此后，PC-A再次发送数据给PC-B时会：
7. 查找目标MAC地址 BB-BB-BB → 端口2
8. **定向转发** → 只从端口2发出，其他端口不受影响

### 关键特性

| 特性 | 说明 |
|:----:|:------|
| **每端口独享带宽** | 8口千兆交换机，每端口独享1000Mbps |
| **全双工通信** | 可同时发送和接收，效率翻倍 |
| **MAC地址表** | 存储在交换机内存中，自动学习建立 |
| **即插即用** | 非管理型交换机无需配置即可工作 |

## 交换机 vs 集线器（对比总结）

| 对比维度 | 集线器 Hub | 交换机 Switch |
|:--------:|:----------:|:-------------:|
| OSI层 | 物理层（L1） | 数据链路层（L2） |
| 转发依据 | 无（直接广播） | MAC地址表 |
| 转发方式 | 广播 | 定向（精确到目标端口） |
| 带宽 | 共享（总带宽÷端口数） | 每端口独占（各享全速） |
| 通信模式 | 半双工 | 全双工 |
| 安全性 | 低 | 高 |
| 价格 | 低（基本淘汰） | 低至中（全面普及） |

> **考试口诀**："Switch是数据链路层的查表专家——学习MAC地址，定向精准送达。"',
'交换机（Switch）是数据链路层设备，通过MAC地址表实现定向数据转发。核心机制：学习源MAC→泛洪未知目标→定向转发已知目标。每端口独享带宽、全双工通信是其关键优势。',
5, '单元1 初识计算机网络', '任务5 认识网络接口及网络设备', 1087,
'交换机三步曲：\n①学习——记住谁在哪个端口\n②泛洪——不知目标就广播问\n③定向——知道后直接送达\n\n记法："学泛定"——学地址、泛洪找、定向送',
'【必考】①交换机工作在哪一层（数据链路层）②转发依据（MAC地址表）③交换机vs集线器对比（工作层/带宽/转发方式）④MAC地址表的学习过程',
2,
'["交换机","Switch","数据链路层","MAC地址表","网络设备"]',
'["网络传输介质与设备","交换机"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10146, 10113, '交换机工作在哪一层？根据什么进行转发？', '交换机工作在数据链路层（第2层）。根据MAC地址表（MAC地址与端口的映射关系）进行定向转发。', 1, 'DEFINITION'),
(10147, 10113, '交换机的工作原理三步曲是什么？', '①学习：记录源MAC地址对应的端口 ②泛洪：目标MAC未知时向所有端口广播 ③定向转发：学习到目标MAC后，只向对应端口精确转发。口诀："学泛定"。', 2, 'PROCEDURE'),
(10148, 10113, '一个8口千兆交换机，每个端口的带宽是多少？', '每个端口都是千兆（1000Mbps），相互独立。这是交换机"每端口独享带宽"的特性——8个口可以同时以千兆速度传输数据。', 3, 'APPLICATION'),
(10149, 10113, '交换机和集线器的转发方式有何本质不同？', 'Hub：广播转发——数据来了向所有端口发（不管对方在哪里）。Switch：定向转发——查MAC地址表，只向目标端口发（精准送达）。', 4, 'COMPARISON');

-- ============================================================
-- 文章10114: 路由器（node=1088）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10114, '路由器（Router）——网际互联的交通警察',
'## 什么是路由器？

路由器（Router）是**网络层设备**（OSI第3层），用于连接不同的网络，实现**网际互联**。它的核心功能是根据**IP地址**进行**路由选择**和**数据包转发**。

### 一句话理解
> 交换机管"局域网内部通信"，路由器管"网与网的连接和上网"。

## 路由器的工作原理

```
             ┌─────────────────────────────────────┐
PC-A → 交换机1 → 路由器A ═══ Internet ═══ 路由器B → 交换机2 → PC-B
(192.168.1.2)  (192.168.1.1)                    (10.0.0.1)   (10.0.0.2)
              └─────────────────────────────────────┘
```

### 工作流程
1. PC-A发送数据给PC-B（目标IP: 10.0.0.2）
2. PC-A发现目标IP不在本局域网→将数据发给**默认网关**（路由器A）
3. 路由器A**解封数据包**，查看目标IP地址
4. 路由器A**查询路由表**，找到到达10.0.0.0网络的最佳路径
5. 路由器A从对应接口转发数据包给路由器B
6. 路由器B收到后，查看目标IP→发现是局域网内设备→转发给PC-B

## 路由表

路由器维护一张**路由表**，记录到达不同网络的路径信息：

| 目标网络 | 下一跳地址 | 接口 | 优先级 |
|:--------:|:----------:|:----:|:------:|
| 192.168.1.0/24 | 直接连接 | eth0 | 0（直连）|
| 10.0.0.0/24 | 直接连接 | eth1 | 0（直连）|
| 172.16.0.0/16 | 10.0.0.254 | eth1 | 10（静态）|
| 0.0.0.0/0 | 202.96.128.1 | wan | 20（默认路由）|

- **直连路由**：直接连接的网络，自动学习
- **静态路由**：管理员手动配置
- **动态路由**：通过路由协议（如RIP、OSPF）自动学习
- **默认路由**（0.0.0.0/0）：所有未明确指定的目标走此路

## 路由器 vs 交换机

| 对比维度 | 交换机 Switch | 路由器 Router |
|:--------:|:------------:|:-------------:|
| **工作层** | 数据链路层（L2） | **网络层**（L3）|
| **转发依据** | MAC地址（48位） | **IP地址**（32位）|
| **功能范围** | 局域网内部（同一个广播域） | 连接不同网络（隔离广播域）|
| **广播域** | 不隔离 | **隔离广播域** |
| **典型场景** | 交换机连接机房内所有电脑 | 路由器连接局域网到Internet |
| **工作维度** | 二层交换（MAC） | 三层路由（IP）|

> **高频考点**：路由器是网络层设备、根据IP地址路由、隔离广播域。交换机不隔离广播域。',
'路由器是网络层设备，根据IP地址进行路由选择和转发，用于连接不同网络（如家庭局域网连接Internet）。核心组件是路由表，包含目标网络、下一跳地址等信息。与交换机不同，路由器隔离广播域。',
5, '单元1 初识计算机网络', '任务5 认识网络接口及网络设备', 1088,
'Router=网络层+IP路由+隔离广播域\n\n"交"换机管内部，"路"由器管互联\n\n路由表记法：要找谁→从哪走→从哪个口出',
'【必考】①路由器工作在哪一层（网络层）②路由器vs交换机对比（层次/转发依据/广播域）③路由表的作用',
2,
'["路由器","Router","网络层","路由","路由表"]',
'["网络传输介质与设备","路由器"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10150, 10114, '路由器工作在哪一层？转发依据是什么？', '路由器工作在网络层（第3层）。转发依据是IP地址（32位），通过查询路由表选择最佳路径转发数据包。', 1, 'DEFINITION'),
(10151, 10114, '路由表包含哪些关键信息？', '路由表主要包含：目标网络地址、下一跳地址（下一站走哪）、出接口（从哪个口走）、优先级/度量值。其中默认路由（0.0.0.0/0）匹配所有未明确指定的目标。', 2, 'DEFINITION'),
(10152, 10114, '交换机、路由器、集线器各工作在哪一层？', 'Hub→物理层（L1）；Switch→数据链路层（L2）；Router→网络层（L3）。口诀："物数网"——Hub物、Switch数、Router网。', 3, 'COMPARISON'),
(10153, 10114, '家里电脑上网时，数据包先经过交换机还是先经过路由器？', '通常电脑→交换机→路由器→Internet。交换机负责局域网内部通信，路由器负责连接外部网络（提供上网功能）。家用场景中路由器和交换机常集成在一台设备中。', 4, 'SCENARIO');

-- ============================================================
-- 文章10115: 调制解调器（node=1089）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10115, '调制解调器（Modem）——数字与模拟的翻译官',
'## 什么是调制解调器？

调制解调器（**Mo**dulator + **Dem**odulator）是实现**数字信号**与**模拟信号**互转的设备。

## 名称由来

| 部分 | 英文 | 功能 |
|:----:|:----:|:-----:|
| **调"制"** | Modulator | 数字信号→模拟信号（发送方向）|
| **解"调"** | Demodulator | 模拟信号→数字信号（接收方向）|
| 合称 | **Modem** = **Mo**dulator + **Dem**odulator |

### 为什么需要Modem？

```
计算机（数字信号0/1）
        ↓
    ┌──Modem──┐
    │ 调制器↘  │
    │ 解调器↗  │
    └──────────┘
        ↓
电话线/光纤（模拟信号/光信号）
```

计算机内部是数字信号（0和1），而传统的电话线传输的是模拟信号（连续电波），光纤传输的是光信号。Modem就是这两个世界的"翻译官"。

## Modem的发展历程

| 类型 | 速率 | 时期 | 说明 |
|:----:|:----:|:----:|:-----:|
| **电话拨号Modem** | **56Kbps** | 1990年代 | 通过电话线上网，拨号时占用电话线 |
| **ADSL Modem** | 2~8Mbps（下行） | 2000年代 | 使用电话线但不影响通话 |
| **光猫（光纤Modem）** | 可达**1Gbps+** | 当前 | 将光信号转换为电信号 |

### 当前的光猫

现在家庭宽带使用的"光猫"本质上就是Modem的一种——它将入户光纤中的**光信号**转换为路由器/电脑能识别的**电信号**（数字信号）。

> 可以简单理解：Modem = 信号的"翻译器"。

## Modem vs 路由器 vs 交换机

| 设备 | 主要功能 |
|:----:|:---------:|
| **Modem** | 数字信号↔模拟/光信号的转换 |
| **路由器** | 连接不同网络，IP路由转发 |
| **交换机** | 局域网内部MAC帧转发 |

> **当前家庭网络连接方式**：光纤入户→光猫（Modem）→路由器→交换机→电脑

> **考试要点**：Modem的基本概念（数字↔模拟转换）、名称含义（Modulator+Demodulator）。通常不考深入细节。',
'调制解调器（Modem）实现数字信号与模拟/光信号的互转。名称由Modulator（调制器）和Demodulator（解调器）合并而成。当前家庭宽带使用的光猫就是一种Modem，将光信号转换为电信号。',
5, '单元1 初识计算机网络', '任务5 认识网络接口及网络设备', 1089,
'Modem=Mo(调制器)+dem(解调器)\n\n记得：Modem是数字和模拟之间的"翻译官"\n\n"调制"=数字变模拟（发出去）\n"解调"=模拟变数字（收进来）',
'【必考】①Modem的功能（数字↔模拟转换）②Modem名称的含义（Modulator+Demodulator）',
1,
'["调制解调器","Modem","光猫","数字信号","模拟信号"]',
'["网络传输介质与设备","调制解调器"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10154, 10115, '调制解调器（Modem）的主要功能是什么？', '实现数字信号与模拟信号之间的互相转换。调制=数字→模拟（发送），解调=模拟→数字（接收）。', 1, 'DEFINITION'),
(10155, 10115, 'Modem名称的由来是什么？', 'Modem = Modulator（调制器）+ Demodulator（解调器）的合称。调制器将数字信号转为模拟信号，解调器将模拟信号转为数字信号。', 2, 'DEFINITION'),
(10156, 10115, '家庭上网设备中，光猫、路由器、交换机分别负责什么？', '光猫（Modem）：光信号↔电信号转换。路由器：连接家庭局域网和Internet。交换机：连接家庭内的多台电脑。现代家庭路由器常内置交换机和Modem功能。', 3, 'COMPARISON'),
(10157, 10115, '电话拨号Modem的速率是多少？', '电话拨号Modem的最高速率是56Kbps（注意单位是Kbps，不是Mbps）。相比现在的光纤千兆网（1000Mbps），速度差约2万倍。', 4, 'APPLICATION');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v170] 单元1 知识库文章种子完成：共15篇文章，约57张记忆卡片。' AS result;
-- ============================================================================
-- v171: 网络应用基础 — 知识库文章种子（单元2 组建局域网）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖4个任务18个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元2：组建局域网
-- ═══════════════════════════════════════════════════════════════
-- 任务1 组建典型局域网（节点1090~1092）
-- 任务2 配置TCP/IP协议（节点1093~1104）
-- 任务3 组建虚拟局域网（节点1105~1106）
-- 任务4 组建无线局域网（节点1107）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10116: 局域网的概念与组成（node=1090）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10116, '局域网（LAN）的概念与组成',
'## 什么是局域网？

局域网（Local Area Network，LAN）是将**较小地理范围**内的计算机和通信设备互连起来的网络。

### 一句话理解
> 局域网就像一个单位内部的"小网络"——一个办公室、一栋楼、一个校园内。

## 局域网的基本组成

| 组成部分 | 具体内容 | 典型例子 |
|---------|---------|---------|
| **硬件设备** | 计算机、网卡、传输介质 | PC、NIC、双绞线 |
| **网络设备** | 连接和转发数据的中介设备 | 交换机、集线器 |
| **通信协议** | 控制数据收发和传输的规则 | TCP/IP、Ethernet |
| **网络操作系统** | 管理网络资源和服务的系统 | Windows Server、Linux |

## 局域网的主要特点

- **覆盖范围小**：通常在一栋楼或一个园区内（几千米以内）
- **传输速率高**：目前主流为千兆（1000Mbps），万兆以太网已普及
- **误码率低**：传输介质质量高，出错概率小
- **归属私有**：为一个单位或组织所有，自主管理
- **成本较低**：设备简单、维护方便

## 局域网 vs 广域网

| 对比维度 | 局域网（LAN） | 广域网（WAN） |
|---------|:----------:|:----------:|
| 覆盖范围 | 几千米以内 | 几百千米以上 |
| 传输速率 | 千兆~万兆 | 几十Mbps~几百Mbps |
| 误码率 | 低 | 较高 |
| 建设成本 | 低 | 高（租用线路） |
| 所有权 | 私有自建 | 运营商所有 |

### 典型场景
- **学校机房**：50台电脑通过交换机连接成小型LAN
- **家庭网络**：路由器+手机/电脑/电视组成家庭LAN
- **企业办公**：各部门通过交换机互连，共享打印机和文件服务器

> **考试重点**：局域网覆盖范围"小"（几千米内）、速率"高"（千兆为主）、归属"私有"。与广域网的对比是常考选择题。',
'局域网（LAN）是将较小地理范围内的计算机互连的网络。核心特点：覆盖范围小、传输速率高、误码率低、归属私有。组成包括计算机、网卡、传输介质、网络设备、通信协议和网络操作系统。',
5, '单元2 组建局域网', '任务1 组建典型局域网', 1090,
'LAN三大特点："小高速稳私"——小(范围小)高(速率高)速(不是速度)稳(误码率低)私(私有)。\n\nLAN vs WAN记法：LAN=私房小院，WAN=公共马路',
'【必考】①局域网的特点（范围小/速率高/误码率低/私有）②局域网的组成要素③LAN与WAN的对比',
1,
'["局域网","LAN","本地网络","网络类型"]',
'["局域网技术"]',
'[
  {"type":"choice","question":"局域网（LAN）的覆盖范围通常是多少？","options":["几米以内","几千米以内","几百千米以内","全球范围"],"answer":"B","explanation":"局域网覆盖范围通常是几千米以内（如一栋楼、一个园区）。几百千米以上属于广域网。"},
  {"type":"choice","question":"以下哪项不是局域网的组成要素？","options":["计算机和网卡","传输介质","通信协议","长途光缆"],"answer":"D","explanation":"长途光缆属于广域网的传输线路。局域网使用双绞线、光纤等自建传输介质。"},
  {"type":"judge","question":"局域网属于私有网络，为一个单位或组织所拥有和管理。","answer":"T","explanation":"局域网通常是一个单位自建的私有网络，由该单位自主管理。广域网则是运营商提供的公共网络。"},
  {"type":"multi","question":"以下哪些是局域网的特点？（多选）","options":["覆盖范围小","传输速率高","误码率低","租用运营商线路","归属私有"],"answer":"A,B,C,E","explanation":"局域网的特点：范围小、速率高、误码率低、归属私有。租用运营商线路是广域网的特点。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10158, 10116, '什么是局域网（LAN）？', '局域网是将较小地理范围内的计算机互连的网络。特点：覆盖范围小（几千米内）、传输速率高（千兆为主）、误码率低、归属私有。', 1, 'DEFINITION'),
(10159, 10116, '局域网由哪些部分组成？', '①硬件设备（计算机/网卡/传输介质）②网络设备（交换机等）③通信协议（TCP/IP/Ethernet）④网络操作系统。', 2, 'DEFINITION'),
(10160, 10116, '局域网和广域网在覆盖范围和速率上有何区别？', 'LAN：范围小（几千米内），速率高（千兆~万兆）。WAN：范围大（几百千米以上），速率较低（受限于租用线路）。', 3, 'COMPARISON');

-- ============================================================
-- 文章10117: 以太网标准IEEE 802.3（node=1091）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10117, '以太网标准 IEEE 802.3',
'## 什么是以太网？

以太网（Ethernet）是**当前应用最广泛的局域网技术**，由 **IEEE 802.3** 标准定义。

### 一句话理解
> 我们日常用的"插网线上网"，99%用的就是以太网技术。

## CSMA/CD——以太网的核心机制

在早期的以太网（使用集线器/Hub）中，所有设备共享同一条通信信道，因此需要一个"交通规则"来避免数据碰撞，这就是 **CSMA/CD**。

### 全称
**C**arrier **S**ense **M**ultiple **A**ccess with **C**ollision **D**etection
（载波监听多路访问/冲突检测）

### 工作四步曲

| 步骤 | 名称 | 一句话理解 |
|:----:|------|-----------|
| ①先听后发 | 载波监听 | 发送前先"听听"线上有没有人在发数据 |
| ②边发边听 | 冲突检测 | 发送过程中持续"监听"，看是否发生碰撞 |
| ③冲突停发 | 检测到冲突 | 发现数据碰撞了，立即停止发送 |
| ④随机重发 | 退避重传 | 等待一个随机时间后重新发送 |

### 口诀
> **"先听后发，边发边听，冲突停发，随机重发"**

> **注意**：现在的交换机网络下，每端口独享带宽，CSMA/CD已基本不使用，但考试仍然考查其原理。

## 以太网标准演进

| 标准 | 速率 | 传输介质 | 最大距离 | 简称记忆 |
|:----:|:----:|---------|:--------:|---------|
| 10Base-T | 10Mbps | 双绞线 | 100m | "十兆双绞" |
| 100Base-TX | 100Mbps | 双绞线 | 100m | "百兆双绞" |
| 1000Base-T | 1Gbps | 双绞线 | 100m | "千兆双绞" |
| 10GBase-T | 10Gbps | 双绞线 | 100m | "万兆双绞" |

### 命名规则解读

以 **100Base-TX** 为例：
- **100** = 速率 100Mbps
- **Base** = 基带传输（Baseband）
- **TX** = 传输介质类型（T=双绞线，F=光纤）

### 速率单位注意
> 以太网速率的单位是 **bps**（bit per second，比特每秒），注意大小写：
> - 100Mbps = 100兆比特每秒
> - 实际下载速度需 ÷8（1字节=8比特），所以100Mbps ≈ 12.5MB/s

> **对口升学考点**：CSMA/CD的四步过程、以太网标准编号含义（100Base-TX）、以太网是最广泛应用的局域网技术。',
'以太网（Ethernet）是当前最广泛应用的局域网技术，由IEEE 802.3标准定义。核心机制CSMA/CD（先听后发→边发边听→冲突停发→随机重发）。标准命名：速率+Base+介质类型。',
5, '单元2 组建局域网', '任务1 组建典型局域网', 1091,
'CSMA/CD四步曲："先听边冲重"——先听后发、边发边听、冲突停发、随机重发。\n\n标准记法：数字是速率，Base=基带，T=双绞线。如100Base-TX=百兆双绞基带传输。',
'【必考】①CSMA/CD的四个步骤②以太网标准命名规则（速率+Base+传输介质）③当前最广泛使用的局域网技术是Ethernet',
2,
'["以太网","Ethernet","IEEE802.3","CSMA/CD","局域网标准"]',
'["局域网技术"]',
'[
  {"type":"choice","question":"CSMA/CD中的」CD「代表什么？","options":["载波监听","冲突检测","载波检测","冲突避免"],"answer":"B","explanation":"CD=Collision Detection（冲突检测）。CS=Carrier Sense（载波监听），MA=Multiple Access（多路访问）。"},
  {"type":"choice","question":"100Base-TX中的」100「表示什么？","options":["传输距离100米","速率100Mbps","100个连接设备","电缆长度100英尺"],"answer":"B","explanation":"数字代表速率，100=100Mbps。100米是双绞线的最大传输距离，不是此处含义。"},
  {"type":"judge","question":"以太网标准由IEEE 802.3定义，是目前应用最广泛的局域网技术。","answer":"T","explanation":"IEEE 802.3标准定义了以太网技术规范，从10Mbps到400Gbps均有覆盖，是目前局域网的主流技术。"},
  {"type":"multi","question":"以下哪些是以太网标准？（多选）","options":["10Base-T","100Base-TX","1000Base-T","802.11ac","10GBase-T"],"answer":"A,B,C,E","explanation":"10Base-T/100Base-TX/1000Base-T/10GBase-T均是以太网标准。802.11ac是无线Wi-Fi标准。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10161, 10117, '什么是以太网？由什么标准定义？', '以太网是当前最广泛应用的局域网技术，由IEEE 802.3标准定义。应用范围从10Mbps到400Gbps，覆盖双绞线和光纤介质。', 1, 'DEFINITION'),
(10162, 10117, 'CSMA/CD的工作过程是什么？', '①先听后发—发送前检测信道②边发边听—发送中检测冲突③冲突停发—检测到碰撞停止④随机重发—等待随机时间重试。口诀："先听边冲重"。', 2, 'PROCEDURE'),
(10163, 10117, '100Base-TX标准中各部分的含义是什么？', '100=速率100Mbps，Base=基带传输，TX=双绞线介质。完整含义：在双绞线上以100Mbps速率传输的基带以太网标准。', 3, 'DEFINITION');

-- ============================================================
-- 文章10118: MAC地址格式详解（node=1092）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10118, 'MAC地址格式——网络硬件的身份证',
'## MAC地址的基本属性

MAC地址（Media Access Control Address）是网卡的**物理地址/硬件地址**。

| 属性 | 数值 |
|:----:|:----:|
| 长度 | **48位**（6字节） |
| 表示方式 | **12位十六进制数** |
| 常见分隔符 | `-` 或 `:` |
| 工作层级 | 数据链路层（第2层） |
| 分配方式 | IEEE统一分配前24位 |

### 格式示例
```
00-1A-2B-3C-4D-5E
或
00:1A:2B:3C:4D:5E
```

## MAC地址的结构

```
  前24位（3字节）         后24位（3字节）
 ┌────────────────┐ ┌────────────────┐
 │ 00-1A-2B       │ │ 3C-4D-5E       │
 │ OUI（厂商代码）   │ │ 设备序列号       │
 └────────────────┘ └────────────────┘
```

- **OUI**（Organizationally Unique Identifier）：由IEEE分配给网卡制造商
- **设备序列号**：由厂商自行分配，保证每块网卡唯一

### 常见厂商OUI示例

| 厂商 | OUI示例 |
|:----:|:--------:|
| Cisco | 00-1A-6C |
| Intel | 00-1B-21 |
| TP-Link | 00-27-19 |
| Huawei | 00-25-9E |

## MAC地址的核心特性

- **全球唯一**：理论上每块网卡MAC地址不重复
- **出厂烧录**：生产时写入网卡ROM，一般不可更改
- **工作在数据链路层**：用于局域网内设备识别

## MAC地址 vs IP地址

| 对比维度 | MAC地址 | IP地址 |
|:--------:|:--------:|:-------:|
| 长度 | 48位 | 32位（IPv4） |
| 进制表示 | 十六进制（12位） | 点分十进制（4段） |
| 形象类比 | **身份证号**（终身不变） | **家庭住址**（可以搬家） |
| 工作层级 | 数据链路层 | 网络层 |
| 能否改变 | 出厂固定（不可变） | 可手动或DHCP更改 |

> **考试口诀**："MAC=48位12个十六进制，前24厂商后24序列，物理地址不变，IP逻辑地址可变。"',
'MAC地址是网卡的物理地址，48位（6字节），用12位十六进制表示。前24位是厂商ID（OUI），后24位是设备序列号。全球唯一、出厂烧录，工作在数据链路层。类比：MAC=身份证号（不变），IP=住址（可变）。',
5, '单元2 组建局域网', '任务1 组建典型局域网', 1092,
'MAC记法："48位=6字节=12个十六进制数"\n前24位=OUI（厂商代码），后24位=序列号\n\nMAC vs IP：MAC是身份证号（终身不变），IP是家庭住址（可以搬家）',
'【必考】①MAC地址长度48位②12位十六进制表示③前24位厂商ID+后24位序列号④MAC是物理地址，IP是逻辑地址',
1,
'["MAC地址","MAC","物理地址","OUI","IEEE分配"]',
'["局域网技术"]',
'[
  {"type":"choice","question":"MAC地址的二进制长度是多少？","options":["32位","48位","64位","128位"],"answer":"B","explanation":"MAC地址48位（6字节），IPv4地址32位，IPv6地址128位。注意不要混淆。"},
  {"type":"choice","question":"MAC地址中由IEEE统一分配的OUI占多少位？","options":["12位","24位","48位","8位"],"answer":"B","explanation":"OUI（组织唯一标识符）占前24位（3字节），由IEEE分配给各网卡制造商。"},
  {"type":"judge","question":"MAC地址是逻辑地址，可以通过DHCP更改。","answer":"F","explanation":"MAC地址是物理地址，出厂时烧录在网卡ROM中，一般不可更改。DHCP分配的是IP地址。"},
  {"type":"multi","question":"以下关于MAC地址的描述，正确的有哪些？（多选）","options":["48位二进制","12位十六进制表示","工作在网络层","前24位是OUI厂商代码","全球唯一出厂烧录"],"answer":"A,B,D,E","explanation":"MAC地址48位、12位十六进制、前24位OUI、全球唯一出厂烧录均正确。MAC地址工作在数据链路层，不是网络层。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10164, 10118, 'MAC地址的格式和结构是怎样的？', 'MAC地址48位（6字节），用12位十六进制表示，前24位=OUI厂商代码，后24位=设备序列号。如00-1A-2B-3C-4D-5E。', 1, 'DEFINITION'),
(10165, 10118, 'MAC地址和IP地址有什么区别？', 'MAC=物理地址/硬件地址（48位十六进制），出厂固定不变→类比"身份证号"。IP=逻辑地址（32位点分十进制），可以改变→类比"家庭住址"。', 2, 'COMPARISON'),
(10166, 10118, 'MAC地址的OUI是什么？占多少位？', 'OUI=Organizationally Unique Identifier（组织唯一标识符），占MAC地址的前24位（3字节），由IEEE统一分配给网卡制造商。', 3, 'DEFINITION');

-- ============================================================
-- 文章10119: IP地址的概念与点分十进制（node=1093）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10119, 'IP地址与点分十进制表示法',
'## 什么是IP地址？

IP地址是为Internet上每台主机分配的**唯一逻辑地址**。

### IPv4地址

| 属性 | 数值 |
|:----:|:----:|
| 长度 | **32位**（4字节） |
| 总数 | 约43亿（2^32） |
| 表示方法 | **点分十进制** |
| 是否可变 | 可手动或DHCP更改 |

### 点分十进制表示法

将32位二进制分成4组，每组8位（1字节），分别转换为十进制，用`.`连接：

```
二进制: 11000000 10101000 00000001 00001010
               ↓        ↓        ↓        ↓
十进制:    192   .  168   .   1    .   10
               → 192.168.1.10
```

### 每段范围

每段8位二进制 → 十进制范围 **0~255**

| 二进制 | 十进制 |
|:------:|:------:|
| 00000000 | 0 |
| 00000001 | 1 |
| ... | ... |
| 11111110 | 254 |
| 11111111 | 255 |

> 结论：**IP地址中任何一段都不会超过255**，这是判断题常见陷阱。

## 特殊IP地址

| IP地址 | 名称 | 作用 |
|:------:|:----:|:----:|
| **127.0.0.1** | 本地回环地址 | 指向本机，测试网卡是否正常 |
| **255.255.255.255** | 受限广播地址 | 向当前网络所有设备广播 |
| **0.0.0.0** | 任意地址 | 表示"所有网络"或"本网络" |

### ping 127.0.0.1 的含义
> 在命令提示符中输入 `ping 127.0.0.1`，是测试本机TCP/IP协议栈是否正常工作的常用方法。如果通，说明本机网络协议安装正确。

## 二进制与十进制快速转换技巧

| 位权 | 2^7 | 2^6 | 2^5 | 2^4 | 2^3 | 2^2 | 2^1 | 2^0 |
|:----:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 值 | 128 | 64 | 32 | 16 | 8 | 4 | 2 | 1 |

**方法**：将二进制位对应的权值相加。
- `11000000` = 128 + 64 = **192**
- `10101000` = 128 + 32 + 8 = **168**

> **考试重点**：IP地址32位/点分十进制/每段0~255/127.0.0.1是回环地址。',
'IP地址是为网络设备分配的唯一逻辑地址。IPv4为32位，用点分十进制表示（4段0~255）。特殊地址：127.0.0.1（本机回环）、255.255.255.255（广播）、0.0.0.0（任意）。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1093,
'点分十进制记法："32位分4段，每段8位0~255"\n\n127.0.0.1回环地址："12(7)回环像7层宝塔，让数据回到自己"',
'【必考】①IP地址32位②点分十进制表示③每段范围0~255④127.0.0.1是回环地址',
1,
'["IP地址","IPv4","点分十进制","回环地址"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"IPv4地址由多少位二进制组成？","options":["16位","32位","48位","128位"],"answer":"B","explanation":"IPv4地址32位，用点分十进制表示。IPv6才是128位，MAC地址是48位。"},
  {"type":"choice","question":"以下哪个IP地址表示本机回环地址？","options":["0.0.0.0","127.0.0.1","192.168.1.1","255.255.255.255"],"answer":"B","explanation":"127.0.0.1是本地回环地址，ping它测试本机网络协议是否正常。0.0.0.0表示任意地址。"},
  {"type":"judge","question":"点分十进制中每段取值范围是0~256。","answer":"F","explanation":"每段是8位二进制，范围0~255（00000000~11111111），256超出了最大值。"},
  {"type":"multi","question":"以下哪些属于特殊IP地址？（多选）","options":["127.0.0.1","255.255.255.255","192.168.1.1","0.0.0.0","10.0.0.1"],"answer":"A,B,D","explanation":"127.0.0.1（回环）、255.255.255.255（广播）、0.0.0.0（任意）是特殊地址。192.168.1.1和10.0.0.1是私有地址。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10167, 10119, '什么是点分十进制表示法？', '将32位IP地址分成4组8位二进制，分别转为十进制后用"点"连接。如 11000000 10101000 00000001 00001010 → 192.168.1.10。每段范围0~255。', 1, 'DEFINITION'),
(10168, 10119, 'IP地址中的特殊地址有哪些？', '①127.0.0.1：本机回环地址（测试网卡）②255.255.255.255：受限广播地址 ③0.0.0.0：表示任意网络。', 2, 'DEFINITION'),
(10169, 10119, 'ping 127.0.0.1可以测试什么？', '测试本机TCP/IP协议栈是否正常。如果能ping通，说明本机的网络协议安装正确，网卡基本正常。', 3, 'APPLICATION');

-- ============================================================
-- 文章10120: IP地址分类与默认子网掩码（node=1094）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10120, 'IP地址分类——A/B/C类与默认子网掩码',
'## IP地址分类（IPv4）

IP地址按网络规模分为A、B、C三类（实际使用），以及D类（组播）和E类（保留）。

### A/B/C类核心对比

| 类别 | 首位二进制 | 第一段范围 | 默认子网掩码 | 网络数 | 每网络主机数 |
|:----:|:---------:|:----------:|:-----------:|:-----:|:----------:|
| **A类** | 0xxx | **1~126** | 255.0.0.0 | 126 | 约1677万 |
| **B类** | 10xx | **128~191** | 255.255.0.0 | 16384 | 65534 |
| **C类** | 110x | **192~223** | 255.255.255.0 | 约209万 | 254 |

### 快速判断方法

**只需看IP地址的第一段数字**：

| 第一段范围 | 类别 | 示例 |
|:----------:|:----:|:----:|
| **1~126** | A类 | 10.0.0.1、114.114.114.114 |
| **128~191** | B类 | 172.16.0.1、180.101.49.11 |
| **192~223** | C类 | 192.168.1.1、200.100.50.1 |

> **注意**：127.x.x.x是回环地址段，不属于A类范围。

### 口诀
> **"12(1~126)得A，18(128~191)得B，92(192~223)得C"**

## 默认子网掩码

子网掩码用于区分IP地址中的**网络位**和**主机位**：

```
A类: 255 .  0  .  0  .  0   网络位=前8位，主机位=后24位
B类: 255 . 255 .  0  .  0   网络位=前16位，主机位=后16位
C类: 255 . 255 . 255 .  0   网络位=前24位，主机位=后8位
```

### CIDR斜线表示法

| 类别 | 子网掩码 | CIDR表示 |
|:----:|:--------:|:---------:|
| A类 | 255.0.0.0 | /8 |
| B类 | 255.255.0.0 | /16 |
| C类 | 255.255.255.0 | /24 |

### 考试记忆技巧
> 子网掩码中"255"的个数就是类别字母正数（A=1个255，B=2个255，C=3个255）。

> **必考高频题**：给一个IP地址判断类别 → 只看第一段数字。给一个类别问默认子网掩码 → ABC分别对应255/255.255/255.255.255后面加.0。',
'IP地址分A/B/C/D/E五类，常用A/B/C三类。A类1~126/掩码255.0.0.0，B类128~191/掩码255.255.0.0，C类192~223/掩码255.255.255.0。判断只看第一段数字。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1094,
'A/B/C首段记忆："1~126→A，128~191→B，192~223→C"\n\n默认掩码记忆：A=255+3个0，B=255.255+2个0，C=255.255.255+1个0\n（255的个数恰好等于ABC的字母序数）',
'【必考】①根据首段数字判断IP类别 ②各类默认子网掩码 ③127不属于A类（是回环地址）',
2,
'["IP地址分类","A类","B类","C类","子网掩码","默认子网掩码","CIDR"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"IP地址192.168.1.1属于哪一类？","options":["A类","B类","C类","D类"],"answer":"C","explanation":"192在第一段范围192~223内，属于C类地址。C类默认子网掩码为255.255.255.0。"},
  {"type":"choice","question":"B类IP地址的默认子网掩码是什么？","options":["255.0.0.0","255.255.0.0","255.255.255.0","255.255.255.255"],"answer":"B","explanation":"B类默认子网掩码255.255.0.0（前16位网络位），CIDR表示为/16。"},
  {"type":"judge","question":"A类地址的第一段范围是1~127。","answer":"F","explanation":"A类范围是1~126，127.x.x.x是回环地址段，不属于A类。这是一个常见陷阱。"},
  {"type":"multi","question":"以下哪些IP地址属于C类地址？（多选）","options":["10.0.0.1","192.168.1.1","172.16.0.1","200.100.50.1","223.10.20.30"],"answer":"B,D,E","explanation":"C类范围为192~223，192.168.1.1、200.100.50.1、223.10.20.30都在此范围。10.x.x.x是A类私有，172.16.x.x是B类私有。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10170, 10120, '如何快速判断IP地址的类别？', '只看第一段数字：1~126→A类，128~191→B类，192~223→C类。注意127是回环地址，不属于A类。', 1, 'PROCEDURE'),
(10171, 10120, 'A/B/C三类各自的默认子网掩码是什么？', 'A类：255.0.0.0（/8），B类：255.255.0.0（/16），C类：255.255.255.0（/24）。255的个数=类别字母序数（A=1个，B=2个，C=3个）。', 2, 'DEFINITION'),
(10172, 10120, 'CIDR斜线表示法的含义？', '斜线后的数字表示子网掩码中连续1的位数。如/24表示前24位是网络位，即255.255.255.0。A/B/C三类对应/8、/16、/24。', 3, 'DEFINITION');

-- ============================================================
-- 文章10121: 私有IP地址范围（node=1095）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10121, '私有IP地址——内网通信的秘密武器',
'## 什么是私有IP地址？

私有IP地址是**保留给内部网络使用**的IP地址段，不能直接在Internet上路由。

### 一句话理解
> 私有地址就像"公司内部短号"——内部互打免费，但不能直接打外线。要上网得通过总机（NAT）转接。

## 三个私有地址范围

| 类别 | 私有范围 | 地址数 | CIDR |
|:----:|:--------:|:------:|:----:|
| **A类私网** | **10.0.0.0 ~ 10.255.255.255** | 约1677万 | 10.0.0.0/8 |
| **B类私网** | **172.16.0.0 ~ 172.31.255.255** | 约104万 | 172.16.0.0/12 |
| **C类私网** | **192.168.0.0 ~ 192.168.255.255** | 65536 | 192.168.0.0/16 |

### 记忆口诀
> **"10一个大A，172小B一个段，192.168家家有"**

### 详细解释
- **10.x.x.x**：A类私有，大企业内网使用（如华为、阿里内网）
- **172.16.x.x ~ 172.31.x.x**：B类私有，中型企业
- **192.168.x.x**：C类私有，**最常见**——家庭、学校、小企业

## 常见私有IP使用场景

| 场景 | 典型IP | 说明 |
|:----:|:------:|:----:|
| 家庭路由器 | 192.168.1.1 | 最常用的路由器管理地址 |
| 学校机房 | 192.168.x.x | 学生机自动获取 |
| 大型企业 | 10.x.x.x | 内部分配大量IP |

## 私有 vs 公有IP

| 对比维度 | 私有IP | 公有IP |
|:--------:|:------:|:------:|
| 范围 | 上述三个范围 | 除私有和保留外的所有IP |
| 路由 | 不可在Internet路由 | 可在Internet路由 |
| 唯一性 | 不同内网可重复使用 | 全球唯一 |
| 是否需要付费 | 免费，任意使用 | 需向运营商租用 |

## NAT（网络地址转换）

私有IP访问Internet必须通过**NAT**（Network Address Translation）转换为公有IP。
> 家用路由器默认开启NAT功能，所以多台设备共用一个公网IP上网。

> **对口升学必考**：三个私有地址范围（10.x.x.x/172.16~31.x.x/192.168.x.x），以及它们属于哪一类。最常见的私有IP是192.168.x.x。',
'私有IP地址是保留给内部网络使用的地址段，不能直接在Internet上路由。三个范围：10.0.0.0/8、172.16.0.0/12、192.168.0.0/16。通过NAT转换访问公网。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1095,
'三大私有范围："10大A，172小B中间的16~31，192.168家家用"',
'【必考】①三个私有地址范围（10/172.16~31/192.168）②私有IP不能在Internet直接路由③NAT的作用——私转公',
1,
'["私有IP","私有地址","NAT","10.0.0.0","192.168","172.16"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"以下哪个是C类私有IP地址段？","options":["10.0.0.0/8","172.16.0.0/12","192.168.0.0/16","172.32.0.0/16"],"answer":"C","explanation":"C类私有地址是192.168.0.0/16。10.0.0.0/8是A类私有，172.16.0.0/12是B类私有。172.32.x.x不在私有范围内。"},
  {"type":"choice","question":"私有IP地址能否直接在Internet上路由？","options":["可以，但速度慢","不可以，需通过NAT转换","可以，通用于全球","取决于运营商"],"answer":"B","explanation":"私有IP地址不能在Internet上直接路由。必须通过NAT（网络地址转换）转换为公有IP后才能访问Internet。"},
  {"type":"judge","question":"不同单位的局域网可以使用相同的私有IP地址段。","answer":"T","explanation":"私有IP的一个特点就是在不同内网中可以重复使用。A公司和B公司都可以使用192.168.1.x段。"},
  {"type":"multi","question":"以下哪些属于私有IP地址范围？（多选）","options":["10.0.0.0~10.255.255.255","172.16.0.0~172.31.255.255","192.168.0.0~192.168.255.255","172.32.0.0~172.32.255.255","1.0.0.0~1.255.255.255"],"answer":"A,B,C","explanation":"私有IP三个范围：10.x.x.x（A类）、172.16~31.x.x（B类）、192.168.x.x（C类）。172.32.x.x和1.x.x.x不在私有范围内。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10173, 10121, '私有IP地址的三个范围是什么？', '①10.0.0.0/8（A类私网，大企业用）②172.16.0.0/12（B类私网，中型企业）③192.168.0.0/16（C类私网最常用，家庭和小企业）。', 1, 'DEFINITION'),
(10174, 10121, '私有IP和公有IP有什么区别？', '私有IP：内网使用、免费、不同内网可重复、不能直接上Internet。公有IP：全球唯一、需付费、可在Internet路由。私有IP通过NAT转公有IP上网。', 2, 'COMPARISON'),
(10175, 10121, '为什么家庭多台设备可以共用一个公网IP上网？', '家用路由器启用NAT（网络地址转换）功能，将多台设备的私有IP映射到同一个公网IP上。这是私有IP访问互联网的核心机制。', 3, 'SCENARIO');

-- ============================================================
-- 文章10122: 子网掩码的作用（node=1096）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10122, '子网掩码——IP地址的"分割线"',
'## 子网掩码的作用

子网掩码（Subnet Mask）用于**区分IP地址中的网络位和主机位**。

### 一句话理解
> IP地址 = 网络号（你住在哪个小区） + 主机号（你是小区里的哪栋楼）。子网掩码划出这两部分的分界线。

## 工作原理

子网掩码中为 **"1"的位对应IP地址的网络位**，为 **"0"的位对应主机位**。

```
IP地址:     192 . 168 .  1  . 10
二进制:   11000000 10101000 00000001 00001010
子网掩码:   255 . 255 . 255 . 0
二进制:   11111111 11111111 11111111 00000000
          └───── 网络位（24位）─────┘└主机位（8位）┘
```

### 由掩码计算出的关键地址

| 含义 | 计算方式 | 本例结果 |
|:----:|:--------:|:---------:|
| **网络地址** | IP AND 子网掩码（主机位全0） | 192.168.1.0 |
| **广播地址** | 网络地址的主机位全变1 | 192.168.1.255 |
| **可用IP范围** | 网络地址+1 ~ 广播地址-1 | 192.168.1.1 ~ 192.168.1.254 |
| **可用主机数** | 2^n - 2（n=主机位数） | 254台 |

> **核心公式**：可用主机数 = 2^(主机位数) - 2（减去网络地址和广播地址）

## 如何判断两台设备是否在同一网段？

将两个IP分别与子网掩码做**按位与（AND）运算**，结果相同则在同一网段。

### 示例

```
设备A: 192.168.1.10  AND 255.255.255.0 = 192.168.1.0
设备B: 192.168.1.20  AND 255.255.255.0 = 192.168.1.0  → 同一网段 ✓
设备C: 192.168.2.10  AND 255.255.255.0 = 192.168.2.0  → 不同网段 ✗
```

### 考点延伸
> 同一网段的设备可以直接通信（通过交换机）。不同网段的设备通信需要路由器，这也是路由器存在的核心意义。

### 记忆表格

| 子网掩码 | CIDR | 主机位数 | 可用主机数 |
|:--------:|:----:|:-------:|:----------:|
| 255.0.0.0 | /8 | 24 | 约1677万 |
| 255.255.0.0 | /16 | 16 | 65534 |
| 255.255.255.0 | /24 | 8 | 254 |
| 255.255.255.128 | /25 | 7 | 126 |
| 255.255.255.192 | /26 | 6 | 62 |

> **必考**：子网掩码的作用（区分网络位/主机位）；根据IP和掩码计算网络地址；判断两台主机是否在同一子网。',
'子网掩码区分IP地址中哪些位是网络号、哪些是主机号。"1"对应网络位，"0"对应主机位。网络地址=IP AND 子网掩码。可用主机数=2^(主机位数)-2。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1096,
'子网掩码记法："1是网络位，0是主机位"\n\n网络地址=IP与掩码做AND运算\n可用主机数=2^(主机位)-2\n\n"减2"是因为网络地址和广播地址不能用',
'【必考】①子网掩码的作用（区分网络位/主机位）②根据IP和掩码计算网络地址③判断两台主机是否在同一子网④可用主机数公式',
2,
'["子网掩码","网络地址","广播地址","CIDR","AND运算","网段"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"子网掩码的主要作用是什么？","options":["加密IP地址","区分网络位和主机位","提高网络速度","分配IP地址"],"answer":"B","explanation":"子网掩码用于区分IP地址中的网络位（掩码中1的位）和主机位（掩码中0的位）。"},
  {"type":"choice","question":"已知子网掩码255.255.255.0，一个网段可用的主机数最多是多少？","options":["254","255","256","65534"],"answer":"A","explanation":"255.255.255.0有8位主机位，2^8-2=256-2=254。减2是因为要去掉网络地址和广播地址。"},
  {"type":"judge","question":"两台计算机的子网掩码必须相同才能在同一网段通信。","answer":"F","explanation":"两台计算机通信时，它们使用各自的子网掩码计算网络地址。网络地址相同即可通信，掩码不一定相同。但通常同一网段的掩码是一致的。"},
  {"type":"multi","question":"以下关于子网掩码255.255.255.0的推导，正确的有哪些？（多选）","options":["CIDR表示为/24","主机位8位","可用主机255台","网络位24位","可用范围1~254"],"answer":"A,B,D,E","explanation":"/24表示24位网络位、8位主机位，可用2^8-2=254台（1~254），不是255台。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10176, 10122, '子网掩码的作用是什么？', '子网掩码用于区分IP地址中的网络位和主机位。掩码为1的位=网络位，掩码为0的位=主机位。网络地址=IP地址 AND 子网掩码。', 1, 'DEFINITION'),
(10177, 10122, '如何计算一个网段的可用主机数？', '可用主机数=2^(主机位数)-2。减2是因为网络地址（主机位全0）和广播地址（主机位全1）不能分配给主机使用。如/24有8位主机位，256-2=254台。', 2, 'APPLICATION'),
(10178, 10122, '如何判断两台设备是否在同一网段？', '将各设备的IP地址分别与子网掩码做按位与（AND）运算，得到网络地址。若两个网络地址相同，则在同网段，可直接通信；不同则需路由器。', 3, 'PROCEDURE');

-- ============================================================
-- 文章10123: IPv6概念（node=1097）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10123, 'IPv6——下一代互联网协议',
'## 为什么需要IPv6？

IPv4地址仅有32位（约43亿个），而全球上网设备远超43亿。IPv4地址在**2019年已基本耗尽**。

> **一句话**：IPv4不够用了，所以IPv6来了。

## IPv6地址

| 属性 | IPv4 | IPv6 |
|:----:|:----:|:----:|
| 地址长度 | **32位** | **128位** |
| 地址数量 | 约43亿 | 约3.4×10^38 |
| 表示方式 | 点分十进制 | **冒号十六进制** |
| 安全性 | 无内置安全 | 内置IPSec |
| 配置方式 | DHCP或手动 | 支持无状态自动配置 |

### IPv6地址数量有多大？
> 3.4×10^38 ≈ 地球上每粒沙子都能分配一个IPv6地址。

## IPv6地址表示法

IPv6采用**冒号十六进制**表示：8组，每组4位十六进制数，组间用冒号分隔。

**完整格式示例**：
```
2001:0db8:85a3:0000:0000:8a2e:0370:7334
```

### 简化书写规则

**规则1：每组的前导零可以省略**
```
2001:0db8:85a3:0000:0000:8a2e:0370:7334
→ 2001:db8:85a3:0:0:8a2e:370:7334
```

**规则2：连续的零组可以用"::"替代（只能使用一次）**
```
2001:0db8:0000:0000:0000:0000:0370:7334
→ 2001:db8::370:7334
```

> **注意**："::"只能出现一次，否则无法确定省略了多少组。

### 简化示例
| 完整地址 | 简化后 |
|:--------:|:-------:|
| 2001:0db8:0000:0000:0000:0000:0000:0001 | 2001:db8::1 |
| FF02:0000:0000:0000:0000:0000:0000:0001 | FF02::1 |
| 0000:0000:0000:0000:0000:0000:0000:0001 | ::1（IPv6回环地址）|

## IPv4 vs IPv6 对比总结

| 对比维度 | IPv4 | IPv6 |
|:--------:|:----:|:----:|
| 地址长度 | 32位 | **128位** |
| 表示方式 | 点分十进制 | **冒号十六进制** |
| 地址数量 | 约43亿 | 约3.4×10^38 |
| 安全性 | 需额外配置IPSec | 内置IPSec安全机制 |
| QoS | 无 | 内置流标签 |
| 地址配置 | DHCP/手动 | 自动配置/DHCPv6 |

> **考试重点**：IPv6=128位、冒号十六进制、简化规则（前导零省略和::替代）、IPv6解决了地址不足问题。',
'IPv6将地址长度从32位扩展到128位，用冒号十六进制表示（8组4位十六进制）。简化规则：前导零可省略，连续零组可用::替代。内置IPSec安全机制。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1097,
'IPv6记法："128位、冒号十六进制、8组4位"\n简化规则：①前导零省略 ②连续零组用::（只能用1次）\n\nIPv6回环地址：::1（相当于IPv4的127.0.0.1）',
'【必考】①IPv6地址长度128位 ②冒号十六进制表示 ③简化书写规则（前导零省略和::替代）④IPv6解决的核⼼问题是IPv4地址不足',
1,
'["IPv6","IP地址","下一代互联网","128位","冒号十六进制"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"IPv6地址由多少位二进制组成？","options":["32位","64位","128位","256位"],"answer":"C","explanation":"IPv6地址128位，由8组4位十六进制数组成。IPv4是32位，注意区分。"},
  {"type":"choice","question":"IPv6地址用什么进制表示？","options":["点分十进制","冒号十六进制","二进制","冒号十进制"],"answer":"B","explanation":"IPv6使用冒号十六进制表示（8组4位十六进制数，冒号分隔）。IPv4使用点分十进制。"},
  {"type":"judge","question":"IPv6简化规则中，「::」可以在一个地址中多次使用。","answer":"F","explanation":"「::」只能使用一次，否则无法确定每组省略了多少个零。如2001::1::1是不合法的。"},
  {"type":"multi","question":"以下关于IPv6的描述，正确的有哪些？（多选）","options":["128位地址","冒号十六进制表示","解决了IPv4地址耗尽问题","比IPv4地址少","内置IPSec安全机制"],"answer":"A,B,C,E","explanation":"IPv6=128位、冒号十六进制、解决IPv4耗尽问题、内置IPSec。IPv6地址比IPv4多得多。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10179, 10123, 'IPv6地址的核心属性是什么？', '地址长度128位，采用冒号十六进制表示（8组4位十六进制），地址数量约3.4×10^38，是IPv4的2^96倍。内置IPSec安全机制。', 1, 'DEFINITION'),
(10180, 10123, 'IPv6简化书写规则有哪些？', '①每组前导零可以省略（如0db8→db8）②连续的零组可以用::替代（只能使用一次）。例：2001:0db8::1 简化自 2001:0db8:0…0:1。', 2, 'PROCEDURE'),
(10181, 10123, 'IPv4和IPv6的主要区别？', 'IPv4：32位，点分十进制，约43亿地址，无内置安全。IPv6：128位，冒号十六进制，无限地址，内置IPSec。', 3, 'COMPARISON');

-- ============================================================
-- 文章10124: TCP协议（node=1098）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10124, 'TCP协议——可靠传输的保障',
'## 什么是TCP？

TCP（Transmission Control Protocol，传输控制协议）是传输层的核心协议之一，提供**面向连接的、可靠的**数据传输服务。

### 一句话理解
> TCP就像"挂号信"——发信前先确认对方地址正确，发信后要求对方签收确认，没收到就重发，确保100%送达。

## TCP的五大核心特征

| 特征 | 说明 | 类比 |
|:----:|:----:|:----:|
| **面向连接** | 通信前先建立连接（三次握手） | 打电话前先拨号接通 |
| **可靠传输** | 确认+重传保证数据正确到达 | 挂号信要求签收 |
| **流量控制** | 防止发送方速度太快淹没接收方 | 水龙头调节水流大小 |
| **拥塞控制** | 网络拥堵时自动降低发送速度 | 堵车时减速慢行 |
| **有序交付** | 数据按发送顺序到达接收方 | 信件按编号排列 |

## 三次握手（建立连接）

```
客户端                           服务器
  │───SYN（发起连接请求）───────→  │
  │←──SYN+ACK（同意并确认）────── │
  │───ACK（确认收到）───────────→  │
  │      连接建立完成              │
  │←───── 开始传输数据 ──────────→ │
```

**三步详解**：
1. **SYN**：客户端说"我想和你建立连接"
2. **SYN+ACK**：服务器说"好的，我也准备好了"
3. **ACK**：客户端说"收到，开始传输"

## 四次挥手（断开连接）

```
客户端                           服务器
  │───FIN（发起断开请求）─────────→  │
  │←──ACK（确认收到，准备关闭）──────  │
  │←──FIN（服务器也准备关闭）───────  │
  │───ACK（确认关闭）─────────────→  │
  │      连接完全断开                │
```

## 使用TCP的典型应用

| 应用 | 原因 |
|:----:|:----:|
| **HTTP/HTTPS**（网页浏览） | 网页内容必须完整无误 |
| **FTP**（文件传输） | 文件损坏一个字节都不行 |
| **SMTP/POP3**（电子邮件） | 邮件内容不能丢失 |
| **SSH**（远程登录） | 命令执行必须准确 |

### 判断方法
> 可靠性要求高 → 使用TCP。如：网页浏览、文件下载、邮件收发、网银交易。

> **对口升学高频考点**：TCP=面向连接/可靠传输/三次握手。对应的应用层协议有HTTP、FTP、SMTP。',
'TCP是传输层协议，提供面向连接、可靠的数据传输服务。核心机制：三次握手建立连接、确认重传保证可靠、滑动窗口控制流量。典型应用：HTTP、FTP、SMTP。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1098,
'TCP记法："TCP=Three-way handshake + Confirm + Protocol"\n即：三次握手+确认=可靠\n\n应用记法：需要"准确"的应用都走TCP（网页、文件、邮件）',
'【必考】①TCP的特征（面向连接/可靠传输）②三次握手的过程 ③TCP vs UDP的区别 ④使用TCP的典型应用',
2,
'["TCP","传输控制协议","传输层","三次握手","可靠传输","面向连接"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"TCP协议位于OSI模型的哪一层？","options":["应用层","传输层","网络层","数据链路层"],"answer":"B","explanation":"TCP位于传输层（第4层），提供端到端的可靠传输服务。网络层是IP协议，应用层是HTTP/FTP等。"},
  {"type":"choice","question":"TCP三次握手的第二步是什么？","options":["客户端发送SYN","服务器回复SYN+ACK","客户端发送ACK","服务器发送FIN"],"answer":"B","explanation":"三次握手：①客户端→SYN ②服务器→SYN+ACK ③客户端→ACK。第二步是服务器回复SYN+ACK。"},
  {"type":"judge","question":"TCP协议是面向连接的不可靠传输协议。","answer":"F","explanation":"TCP是面向连接的、可靠的传输协议。UDP才是不可靠的。可靠体现在确认应答、超时重传等机制。"},
  {"type":"multi","question":"以下哪些应用通常使用TCP协议？（多选）","options":["网页浏览（HTTP）","文件传输（FTP）","视频直播","电子邮件（SMTP）","DNS查询"],"answer":"A,B,D","explanation":"HTTP、FTP、SMTP需要数据完整可靠，使用TCP。视频直播和DNS查询对实时性要求高，使用UDP。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10182, 10124, 'TCP协议的核心特征有哪些？', '①面向连接—通信前先建立连接②可靠传输—确认+重传③流量控制—防止发送过快④拥塞控制—网络拥堵时减速⑤有序交付—数据按序到达。', 1, 'DEFINITION'),
(10183, 10124, 'TCP三次握手的过程是什么？', '①客户端→发送SYN请求建立连接②服务器→回复SYN+ACK同意请求③客户端→发送ACK确认，连接建立完成。可以开始传输数据。', 2, 'PROCEDURE'),
(10184, 10124, '哪些应用使用TCP协议？为什么？', 'HTTP/HTTPS（网页）、FTP（文件）、SMTP/POP3（邮件）、SSH（远程登录）。这些应用要求数据100%完整准确，不能丢失。', 3, 'APPLICATION');

-- ============================================================
-- 文章10125: UDP协议（node=1099）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10125, 'UDP协议——高效传输的轻骑兵',
'## 什么是UDP？

UDP（User Datagram Protocol，用户数据报协议）是传输层的另一核心协议，提供**无连接的、不可靠但高效**的数据传输服务。

### 一句话理解
> UDP就像"明信片"——写地址就寄出去，不确认对方收到没，也不管送达顺序。速度很快，但可能丢件。

## UDP的四大核心特征

| 特征 | 说明 | 类比 |
|:----:|:----:|:----:|
| **无连接** | 发送前不需要建立连接 | 寄明信片，不用提前打招呼 |
| **不可靠** | 不保证送达，无确认重传 | 寄出去就不管了 |
| **高效** | 头部开销小（8字节），延迟低 | 明信片比挂号信更简洁 |
| **无拥塞控制** | 保持恒定速率发送 | 不管网络堵不堵都发 |

### TCP vs UDP 头部对比
| 协议 | 头部大小 | 额外开销 |
|:----:|:--------:|:--------:|
| TCP | 20~60字节 | 大（连接管理+确认机制）|
| UDP | **8字节** | **小（仅有源端口+目标端口+长度+校验）** |

## 适用场景

### 什么时候用UDP？
1. **对实时性要求高**——宁可丢一点，不能卡
2. **对可靠性要求不高**——少一帧画面不影响
3. **简单请求响应**——一次一问一答

### 典型应用

| 应用 | 使用UDP的原因 |
|:----:|:-------------:|
| **DNS查询**（端口53） | 一问一答，简单快速 |
| **视频直播/流媒体** | 允许少量丢帧，但不能卡顿 |
| **VoIP网络电话** | 实时对话，延迟比丢包更影响体验 |
| **在线游戏** | 实时对战，稍微丢包可接受 |
| **DHCP**（端口67/68） | 广播请求，简单高效 |

### 判断方法
> 实时性要求高、可容忍少量丢失 → 使用UDP。如：视频直播、语音通话、在线游戏。

## TCP vs UDP 核心对比

| 对比维度 | TCP | UDP |
|:--------:|:----:|:----:|
| 连接方式 | 面向连接 | **无连接** |
| 可靠性 | **可靠**（确认重传） | 不可靠（尽力而为） |
| 传输速度 | 慢 | **快** |
| 头部大小 | 20~60字节 | **8字节** |
| 流量控制 | 有 | 无 |
| 适用场景 | 网页/文件/邮件 | 直播/DNS/游戏 |

### 对比口诀
> **"TCP是挂号信——慢但准；UDP是明信片——快但可能丢。"**

> **高频考点**：TCP vs UDP的区别（面向连接vs无连接/可靠vs不可靠/慢vs快）。哪个应用用哪个协议。',
'UDP提供无连接、不可靠但高效的数据传输。头部仅8字节，延迟低。适用于对实时性要求高、可容忍少量丢失的场景：DNS、视频直播、在线游戏、VoIP。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1099,
'UDP记法："U=Unreliable（不可靠）、U=Ultra-fast（超快）、U=User-friendly for real-time（适合实时）"\n\nUDP=明信片（快但可能丢），TCP=挂号信（慢但准）',
'【必考】①UDP的特征（无连接/不可靠/高效）②TCP vs UDP的核心区别 ③使用UDP的典型应用（DNS/视频/游戏/语音）',
2,
'["UDP","用户数据报协议","传输层","无连接","不可靠传输","高效"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"UDP协议的核心特点是什么？","options":["面向连接、可靠传输","无连接、可靠传输","无连接、不可靠但高效","面向连接、高效传输"],"answer":"C","explanation":"UDP=无连接+不可靠+高效。它在传输前不需要建立连接，不保证数据到达，但速度快、开销小。"},
  {"type":"choice","question":"以下哪项最适合使用UDP协议？","options":["网页浏览","文件传输","视频直播","电子邮件"],"answer":"C","explanation":"视频直播对实时性要求高，允许少量丢帧，适合用UDP。网页/文件/邮件要求数据完整，使用TCP。"},
  {"type":"judge","question":"UDP协议提供可靠的数据传输服务，保证数据不丢失。","answer":"F","explanation":"UDP是」尽力而为」的传输，不提供可靠性保证。可靠传输是TCP的特性。"},
  {"type":"multi","question":"以下哪些应用通常使用UDP协议？（多选）","options":["DNS查询","网页浏览","视频直播","在线游戏","邮件发送"],"answer":"A,C,D","explanation":"DNS（53端口）、视频直播、在线游戏使用UDP。网页浏览（HTTP）和邮件发送（SMTP）使用TCP。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10185, 10125, 'UDP协议的核心特征有哪些？', '①无连接—不需要建立连接②不可靠—不确认不重传③高效—头部仅8字节④无拥塞控制—保持速率。概括：简单、快速、不保证。', 1, 'DEFINITION'),
(10186, 10125, '什么场景下应该使用UDP而不是TCP？', '对实时性要求高、可容忍少量数据丢失的场景。如：视频直播（少一帧没关系）、语音通话（不能卡顿）、在线游戏（延迟最重要）、DNS查询（一问一答）。', 2, 'APPLICATION'),
(10187, 10125, 'TCP和UDP的核心区别是什么？', 'TCP：面向连接、可靠、有确认重传、头部大、速度慢——用于网页/文件/邮件。UDP：无连接、不可靠、无确认、头部小、速度快——用于直播/DNS/游戏。', 3, 'COMPARISON');

-- ============================================================
-- 文章10126: HTTP/HTTPS协议与端口（node=1100）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10126, 'HTTP与HTTPS——网页浏览背后的协议',
'## HTTP（超文本传输协议）

- **全称**：HyperText Transfer Protocol
- **作用**：Web浏览器和Web服务器之间传输超文本（HTML页面、图片等）
- **默认端口**：**80**
- **特点**：明文传输，数据不加密

### 一句话理解
> 你在浏览器里输入网址、打开网页，背后就是HTTP协议在工作。

### HTTP请求方法

| 方法 | 含义 | 举例 |
|:----:|:----:|:----:|
| **GET** | 获取资源 | 请求网页、下载图片 |
| **POST** | 提交数据 | 登录表单、搜索内容 |
| PUT | 更新资源 | 修改个人信息 |
| DELETE | 删除资源 | 删除文件 |

> HTTP是**无状态**协议——每个请求相互独立，记不住你之前做过什么。需要Cookie/Session辅助记录状态。

## HTTPS（安全超文本传输协议）

- **全称**：HTTP over SSL/TLS
- **默认端口**：**443**
- **特点**：通过SSL/TLS加密传输，防止数据被窃听和篡改
- **标识**：浏览器地址栏显示 🔒 图标

### 一句话理解
> HTTPS = HTTP + 加密。你在网上购物、登录银行时看到网址前有个🔒小锁，就是在用HTTPS。

## HTTP vs HTTPS

| 对比维度 | HTTP | HTTPS |
|:--------:|:----:|:-----:|
| 默认端口 | **80** | **443** |
| 传输方式 | 明文（不加密） | SSL/TLS加密 |
| 安全性 | 低 | 高 |
| URL前缀 | http:// | https:// |
| 性能 | 快 | 略慢（加密消耗性能） |
| 证书 | 不需要 | 需要CA证书 |

### 为什么现在越来越多的网站用HTTPS？
> ①防止信息被窃听（如Wi-Fi钓鱼）②防止内容被篡改（如植入广告）③搜索引擎（百度、Google）对HTTPS网站有排名加分。

## 常见端口速记

| 应用层协议 | 端口 | 传输层协议 |
|:----------:|:----:|:----------:|
| HTTP | 80 | TCP |
| HTTPS | **443** | TCP |

> **对口升学高频考点**：HTTP端口80、HTTPS端口443、HTTPS比HTTP多一层SSL/TLS加密。一般不考更深细节。',
'HTTP（端口80）是Web传输协议，明文传输。HTTPS（端口443）是HTTP+SSL/TLS加密，更安全。HTTPS使用CA证书验证身份，地址栏显示🔒图标。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1100,
'端口记法："HTTP=8竖起来像锁没锁（不安全），HTTPS=4+4+3=10+3像🔒（安全）"\n更好记：HTTP=80（80像"发令"发网页），HTTPS=443（443像"事事安全"）',
'【必考】①HTTP端口80 ②HTTPS端口443 ③HTTPS比HTTP多一层SSL/TLS加密 ④HTTPS更安全',
2,
'["HTTP","HTTPS","超文本传输协议","SSL","TLS","80端口","443端口","Web"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"HTTP协议的默认端口号是多少？","options":["21","80","443","53"],"answer":"B","explanation":"HTTP默认端口80。443是HTTPS，21是FTP，53是DNS。"},
  {"type":"choice","question":"HTTPS比HTTP多了一层什么机制？","options":["数据压缩","SSL/TLS加密","负载均衡","缓存加速"],"answer":"B","explanation":"HTTPS=HTTP+SSL/TLS加密，通过CA证书验证网站身份并加密传输数据。"},
  {"type":"judge","question":"HTTPS的默认端口是8080。","answer":"F","explanation":"HTTPS默认端口是443。8080通常作为HTTP的替代端口使用，但不是HTTPS的标准端口。"},
  {"type":"multi","question":"以下关于HTTP和HTTPS的描述，正确的有哪些？（多选）","options":["HTTP默认端口80","HTTPS默认端口443","HTTPS传输加密更安全","HTTP比HTTPS更安全","HTTPS需要CA证书"],"answer":"A,B,C,E","explanation":"HTTP（80）明文传输不安全，HTTPS（443）加密传输更安全且需要CA证书。D明显错误。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10188, 10126, 'HTTP和HTTPS的默认端口是多少？', 'HTTP默认端口80（明文传输），HTTPS默认端口443（SSL/TLS加密传输）。HTTPS = HTTP + 加密 + 证书验证。', 1, 'DEFINITION'),
(10189, 10126, 'HTTPS是如何保证安全的？', 'HTTPS在HTTP基础上增加SSL/TLS加密层，对传输数据进行加密，防止窃听和篡改。同时通过CA数字证书验证网站身份，防止钓鱼网站。', 2, 'DEFINITION'),
(10190, 10126, '什么场景下应该使用HTTPS？', '涉及敏感信息的场景：网上银行（密码）、电商购物（支付）、登录页面（账号密码）、电子邮件（内容隐私）。原则：有隐私数据就用HTTPS。', 3, 'APPLICATION');

-- ============================================================
-- 文章10127: FTP协议与端口（node=1101）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10127, 'FTP——文件传输的标准协议',
'## 什么是FTP？

FTP（File Transfer Protocol，文件传输协议）用于在网络上的两台计算机之间**上传和下载文件**。

### 一句话理解
> 就像"网络上的文件管理器"——你可以把电脑上的文件传到服务器上（上传），也可以从服务器下载到本地（下载）。

## FTP的端口

FTP使用**两个连接**完成传输：

| 连接类型 | 作用 | 默认端口 |
|:--------:|:----:|:--------:|
| **控制连接** | 传输命令和用户名密码 | **21** |
| **数据连接** | 实际传输文件数据 | **20** |

> **考试重点**：FTP的控制端口是21，数据端口是20。但考试中一般只考21。

## FTP的访问方式

### 匿名FTP
- 用户名：`anonymous`
- 密码：任意邮箱或空
- 权限：只能下载，不能上传（通常）
- 用途：公共文件下载（如开源软件镜像站）

### 认证FTP
- 需要合法用户名和密码
- 权限：可上传、下载、管理文件
- 用途：网站管理、文件备份

## FTP传输模式

| 模式 | 适用于 | 说明 |
|:----:|:------:|:----:|
| **ASCII模式** | 文本文件（.txt/.html） | 自动转换不同操作系统的换行符 |
| **二进制模式** | 非文本文件（图片/程序/ZIP） | 原样传输，不做任何转换 |

## 安全的文件传输替代方案

| 协议 | 端口 | 特点 |
|:----:|:----:|:----:|
| **SFTP** | 22 | 基于SSH的安全文件传输，传输加密 |
| **FTPS** | 990 | FTP over SSL/TLS，传输加密 |

> **对口升学考点**：FTP的功能是文件传输（上传/下载），默认端口21。',
'FTP（端口21/20）用于网络文件上传和下载。使用两个连接：控制连接（21）传输命令、数据连接（20）传输文件。支持匿名和认证两种访问方式。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1101,
'FTP端口记法："FTP我要上传文件—21（2要1传）"\n数据端口20（2要0数据）\n\n"FTP=File Transfer Protocol=文件传输"',
'【必考】①FTP的作用（文件上传/下载）②默认端口21 ③匿名FTP使用anonymous用户名 ④控制连接21/数据连接20',
2,
'["FTP","文件传输协议","21端口","文件上传","文件下载","匿名FTP"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"FTP协议的主要功能是什么？","options":["网页浏览","文件上传和下载","邮件收发","域名解析"],"answer":"B","explanation":"FTP（文件传输协议）用于在网络上进行文件的上传和下载。HTTP用于网页浏览。"},
  {"type":"choice","question":"FTP控制连接的默认端口是多少？","options":["20","21","22","80"],"answer":"B","explanation":"FTP控制连接（传输命令）使用端口21，数据连接（传输文件）使用端口20。22是SSH端口。"},
  {"type":"judge","question":"FTP只能从服务器下载文件，不能上传文件到服务器。","answer":"F","explanation":"FTP既可以下载（从服务器到本地）也可以上传（从本地到服务器），是双向文件传输协议。"},
  {"type":"multi","question":"以下关于FTP的描述，正确的有哪些？（多选）","options":["默认端口21","能上传和下载文件","匿名FTP使用anonymous用户名","FTP传输是加密的","基于TCP协议"],"answer":"A,B,C,E","explanation":"FTP端口21、支持上传下载、匿名用anonymous、基于TCP均正确。FTP传输是明文的，不加密。SFTP/FTPS才是加密版本。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10191, 10127, 'FTP协议的作用和默认端口是什么？', 'FTP（文件传输协议）用于网络文件上传和下载。控制连接端口21（传输命令），数据连接端口20（传输文件）。考试常考端口21。', 1, 'DEFINITION'),
(10192, 10127, '什么是匿名FTP？', '匿名FTP使用用户名anonymous登录，无需密码，通常仅有下载权限。开源软件镜像站（如清华TUNA源）常使用匿名FTP提供服务。', 2, 'DEFINITION'),
(10193, 10127, 'FTP为什么要用两个端口？', 'FTP将控制命令和文件数据分离：端口21负责传输用户名密码和操作命令（控制连接），端口20负责传输文件内容（数据连接）。分工明确提高效率。', 3, 'SCENARIO');

-- ============================================================
-- 文章10128: SMTP/POP3邮件协议（node=1102）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10128, '电子邮件协议——SMTP与POP3',
'## 电子邮件的两个关键协议

| 协议 | 全称 | 作用 | 默认端口 |
|:----:|:----:|:----:|:--------:|
| **SMTP** | Simple Mail Transfer Protocol | **发送**邮件和服务器间转发 | **25** |
| **POP3** | Post Office Protocol Version 3 | **接收**邮件到本地客户端 | **110** |

### 一句话理解
> SMTP是"邮递员"（帮你把信送出去），POP3是"信箱钥匙"（帮你把信取回来）。

## SMTP（发信协议）

- **全称**：Simple Mail Transfer Protocol
- **作用**：①用户→发件服务器 ②发件服务器→收件服务器
- **默认端口**：**25**
- **特点**：只能"推"（push），不能"拉"（pull）

### SMTP工作过程
```
发件人 → SMTP(25) → 发件服务器 → SMTP(25) → 收件服务器 → POP3(110) → 收件人
         ↑发送邮件        ↑服务器间转发           ↑接收邮件到本地
```

## POP3（收信协议）

- **全称**：Post Office Protocol Version 3
- **作用**：从收件服务器将邮件下载到本地客户端
- **默认端口**：**110**
- **特点**：默认下载邮件后从服务器删除（可设置保留副本）

> **对比**：POP3将邮件下载到本地阅读（离线阅读），IMAP将邮件保留在服务器上（在线阅读）。

## 协议对比

| 协议 | 方向 | 端口 | 类比 |
|:----:|:----:|:----:|:----:|
| **SMTP** | 发（push） | **25** | 邮递员送信出去 |
| **POP3** | 收（pull） | **110** | 从邮箱取信回家 |

### 完整的邮件发送过程
```
例：张三用QQ邮箱发邮件给李四的163邮箱

张三（QQ邮箱） → SMTP(25) → QQ邮件服务器
                              ↓ SMTP(25) 互联网转发
                            163邮件服务器
                              ↓ POP3(110)
                             李四（163客户端）
```

### 现代加密版本
| 协议 | 加密版本 | 端口 |
|:----:|:--------:|:----:|
| SMTP | SMTPS | 465 |
| POP3 | POP3S | 995 |

> **对口升学必考**：SMTP=发邮件（25），POP3=收邮件（110）。两者不能混淆！',
'SMTP（端口25）用于发送邮件，POP3（端口110）用于接收邮件。SMTP将邮件从客户端发送到服务器并在服务器间转发，POP3从服务器下载邮件到本地客户端。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1102,
'SMTP记法："S=Send（发送），25=2要5我发"\nPOP3记法："P=Post/Pull（取），110=110来收邮件"\n\n"SMTP送出去，POP3收回来"',
'【必考】①SMTP作用=发邮件（端口25）②POP3作用=收邮件（端口110）③两者不能混淆 ④邮件传输的完整流程',
2,
'["SMTP","POP3","电子邮件","邮件协议","25端口","110端口","邮件发送","邮件接收"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"发送电子邮件时使用的协议是？","options":["POP3","SMTP","HTTP","FTP"],"answer":"B","explanation":"SMTP（简单邮件传输协议）用于发送邮件，端口25。POP3用于接收邮件，端口110。"},
  {"type":"choice","question":"POP3协议的默认端口是多少？","options":["25","53","110","443"],"answer":"C","explanation":"POP3默认端口110，用于从邮件服务器接收邮件到本地。SMTP端口25，DNS端口53，HTTPS端口443。"},
  {"type":"judge","question":"SMTP协议用于从邮件服务器接收邮件到本地客户端。","answer":"F","explanation":"SMTP是发送邮件协议（推），POP3才是从服务器接收邮件到本地的协议（拉）。两者功能相反。"},
  {"type":"multi","question":"以下关于电子邮件协议的描述，正确的有哪些？（多选）","options":["SMTP用于发送邮件","POP3用于接收邮件","SMTP默认端口25","POP3默认端口110","两者默认端口都是80"],"answer":"A,B,C,D","explanation":"SMTP发（25）、POP3收（110）均正确。E错误，80是HTTP的端口。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10194, 10128, 'SMTP和POP3分别用于什么？默认端口是多少？', 'SMTP（25端口）：发送邮件。POP3（110端口）：接收邮件到本地。SMTP=发信，POP3=收信。', 1, 'DEFINITION'),
(10195, 10128, '一封电子邮件从发件人到收件人的完整过程？', '发件人→SMTP(25)→发件服务器→SMTP(25)→收件服务器→POP3(110)→收件人。SMTP负责"送"，POP3负责"取"。', 2, 'PROCEDURE'),
(10196, 10128, 'SMTP和POP3在功能上有什么本质区别？', 'SMTP是"推"协议（push），将邮件推向服务器。POP3是"拉"协议（pull），将邮件拉到本地。一个只管发，一个只管收。', 3, 'COMPARISON');

-- ============================================================
-- 文章10129: DNS域名解析（node=1103）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10129, 'DNS——互联网的电话簿',
'## 什么是DNS？

DNS（Domain Name System，域名系统）的主要作用是将**域名转换为IP地址**（正向解析），也可以从IP反查域名（反向解析）。

### 一句话理解
> DNS就像"手机通讯录"——你记不住朋友的电话号码（IP地址），但记得住名字（域名），通讯录（DNS）帮你查到号码。

### 为什么需要DNS？
```
你记住的是：  www.baidu.com    （域名——好记）
电脑需要的是： 183.2.172.185   （IP地址——难记）
     DNS服务器帮你把域名翻译成IP地址
```

## 域名空间结构（树形层次）

```
根域（.）
├── 顶级域（Top-Level Domain, TLD）
│   ├── 通用顶级域：.com .org .net .edu
│   ├── 国家顶级域：.cn .jp .uk .us
│   └── 新顶级域：.top .xyz .club
│       │
│       └── 二级域（Second-Level Domain）
│           ├── baidu.com
│           ├── google.com
│           └── tsinghua.edu.cn
│               │
│               └── 三级域（子域名）
│                   ├── www.baidu.com
│                   ├── mail.baidu.com
│                   └── www.tsinghua.edu.cn
```

### 常见顶级域名

| 域名后缀 | 机构类型 |
|:--------:|:--------:|
| .com | 商业机构（最常见） |
| .cn | 中国 |
| .edu | 教育机构 |
| .gov | 政府机构 |
| .org | 非营利组织 |

## DNS的默认端口

| 协议 | 默认端口 |
|:----:|:--------:|
| **DNS** | **53** |

> DNS使用UDP（端口53）进行普通查询，TCP（端口53）进行区域传输。

## DNS解析过程

```
你在浏览器输入 www.baidu.com

①浏览器缓存 → 有记录？→ 直接用
②操作系统缓存 / hosts文件 → 有记录？→ 直接用
③本地DNS服务器（运营商/114.114.114.114）→ 有缓存？→ 直接返回
④根DNS服务器 → 告诉你去哪找.com的DNS
⑤.com顶级域DNS → 告诉你去哪找baidu.com的DNS
⑥baidu.com权威DNS → 返回www.baidu.com的IP地址 → 你的浏览器开始访问
```

> 实际过程中第④~⑥步通常在几毫秒内完成。

### 常用公共DNS

| DNS服务器 | 地址 | 特点 |
|:---------:|:----:|:----:|
| 114DNS | 114.114.114.114 | 国内快速、安全 |
| 阿里DNS | 223.5.5.5 | 国内稳定 |
| Google DNS | 8.8.8.8 | 全球通用 |
| 腾讯DNS | 119.29.29.29 | 国内快速 |

> **对口升学高频考点**：DNS的作用（域名→IP地址），端口号53。',
'DNS将域名转换为IP地址，默认端口53（UDP）。域名空间为树形层次（根→顶级域→二级域→三级域）。解析过程：浏览器缓存→操作系统缓存→本地DNS→根DNS→顶级域DNS→权威DNS。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1103,
'DNS记法："DNS=Domain Name System=域名=53"\n\n"DNS就像电话本，记名不记号（IP号）"\n\n常用DNS：114.114.114.114（记忆：114查号台）',
'【必考】①DNS的作用（域名↔IP地址）②DNS端口53 ③域名层次结构（.com/.cn等）④DNS解析过程',
2,
'["DNS","域名系统","域名解析","53端口","域名","顶级域","域名服务器"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"DNS的主要作用是什么？","options":["将域名转换为IP地址","发送电子邮件","浏览网页","传输文件"],"answer":"A","explanation":"DNS（域名系统）将人容易记忆的域名（如www.baidu.com）转换为计算机需要的IP地址。"},
  {"type":"choice","question":"DNS服务的默认端口号是多少？","options":["21","25","53","80"],"answer":"C","explanation":"DNS默认端口53（UDP）。21是FTP，25是SMTP，80是HTTP。"},
  {"type":"judge","question":".com属于顶级域名。","answer":"T","explanation":".com是通用顶级域（gTLD），由ICANN管理。是互联网上最常见的顶级域名，主要用于商业机构。"},
  {"type":"multi","question":"以下哪些属于顶级域名？（多选）","options":[".com",".cn",".www",".org",".baidu"],"answer":"A,B,D","explanation":"顶级域名包括.com（商业）、.cn（中国）、.org（非营利）等。www是三级域名（子域名），baidu是二级域名。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10197, 10129, 'DNS的作用和默认端口是什么？', 'DNS将域名解析为IP地址（正向解析），也可将IP解析为域名（反向解析）。默认端口53（UDP）。', 1, 'DEFINITION'),
(10198, 10129, '域名的层次结构是怎样的？', '树形结构：根域(.)→顶级域(.com/.cn)→二级域(baidu.com)→三级域(www.baidu.com)。越往右层级越低。', 2, 'DEFINITION'),
(10199, 10129, 'DNS解析的大致过程是什么？', '浏览器缓存→操作系统缓存/hosts→本地DNS服务器→根DNS→顶级域DNS→权威DNS→返回IP。实际在毫秒级完成。', 3, 'PROCEDURE');

-- ============================================================
-- 文章10130: DHCP动态主机配置（node=1104）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10130, 'DHCP——IP地址的自动分配器',
'## 什么是DHCP？

DHCP（Dynamic Host Configuration Protocol，动态主机配置协议）用于**自动为网络中的计算机分配IP地址**和其他网络参数。

### 一句话理解
> 没有DHCP时，你得手动给每台电脑设置IP地址；有DHCP后，电脑插上网线或连上Wi-Fi就自动获取IP。

## DHCP能分配哪些参数？

| 参数 | 说明 | 示例 |
|:----:|:----:|:----:|
| **IP地址** | 主机的逻辑地址 | 192.168.1.10 |
| **子网掩码** | 区分网络位/主机位 | 255.255.255.0 |
| **默认网关** | 访问外网的出口 | 192.168.1.1 |
| **DNS服务器** | 域名解析服务地址 | 114.114.114.114 |

## DHCP的三种分配方式

| 方式 | 说明 | 适用场景 |
|:----:|:----:|:---------:|
| **自动分配** | 永久分配一个IP给客户端 | 服务器、打印机等固定设备 |
| **动态分配**（最常用） | 分配有租期的IP，到期回收 | 电脑、手机等临时设备 |
| **手动分配** | 管理员指定IP→MAC绑定 | MAC地址白名单，安全控制 |

## DHCP工作原理

```
客户端                     DHCP服务器
  │── DHCP Discover ──────→  │  ①客户端广播找服务器
  │   （广播，我是谁？）      │
  │                          │
  │←── DHCP Offer ────────  │  ②服务器回应，提供IP
  │   （来，给你这个IP）      │
  │                          │
  │── DHCP Request ────────→  │  ③客户端请求使用该IP
  │   （好的，我就用这个）     │
  │                          │
  │←── DHCP ACK ──────────  │  ④服务器确认分配
  │   （成交，归你了）        │
```

### 四步简称：**DORA**
> **D**iscover（发现）→ **O**ffer（提供）→ **R**equest（请求）→ **A**ck（确认）

### DORA口诀
> **"发现→提供→请求→确认"（DORA）**

## 日常生活中的DHCP

- 手机连接Wi-Fi：自动获取IP地址（DHCP）
- 电脑插网线上网：自动获取IP地址
- 公司会议室临时接入设备：自动获取IP

> **考试重点**：DHCP的作用（自动分配IP），四步过程（DORA），分配方式中动态分配最常用。',
'DHCP自动为网络设备分配IP地址、子网掩码、默认网关和DNS等参数。四步过程：Discover→Offer→Request→ACK（DORA）。动态分配最常用。',
5, '单元2 组建局域网', '任务2 配置TCP/IP协议', 1104,
'DHCP记法："DORA四步曲"\nDiscover（发现）→Offer（提供）→Request（请求）→ACK（确认）\n\n"动态配置不用愁，插上网线马上有"',
'【必考】①DHCP的作用（自动分配IP）②DORA四步过程 ③三种分配方式（动态最常用）④DHCP可分配IP/掩码/网关/DNS',
1,
'["DHCP","动态主机配置","自动分配IP","DORA","IP地址分配"]',
'["TCP/IP协议与IP地址"]',
'[
  {"type":"choice","question":"DHCP的主要作用是什么？","options":["将域名转换为IP地址","自动分配IP地址","加密网络传输","文件传输"],"answer":"B","explanation":"DHCP（动态主机配置协议）自动为网络设备分配IP地址等参数。DNS才是域名解析。"},
  {"type":"choice","question":"DHCP工作中，客户端发送的第一个消息是什么？","options":["DHCP Offer","DHCP Request","DHCP Discover","DHCP ACK"],"answer":"C","explanation":"第一步是客户端广播DHCP Discover（发现）消息，寻找DHCP服务器。这是DORA的D。"},
  {"type":"judge","question":"DHCP只能分配IP地址，不能分配其他网络参数。","answer":"F","explanation":"DHCP不仅可以分配IP地址，还可以分配子网掩码、默认网关、DNS服务器等参数。"},
  {"type":"multi","question":"DHCP可以自动分配哪些网络参数？（多选）","options":["IP地址","子网掩码","默认网关","DNS服务器","MAC地址"],"answer":"A,B,C,D","explanation":"DHCP可分配IP地址、子网掩码、默认网关、DNS服务器。MAC地址是网卡硬件地址，不由DHCP分配。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10200, 10130, 'DHCP的作用是什么？', 'Dynamic Host Configuration Protocol，自动为网络中的设备分配IP地址、子网掩码、默认网关、DNS等参数。无需手动配置。', 1, 'DEFINITION'),
(10201, 10130, 'DHCP的DORA四步过程是什么？', '①D=Discover：客户端广播找DHCP服务器②O=Offer：服务器提供IP地址③R=Request：客户端请求使用该IP④A=ACK：服务器确认分配。', 2, 'PROCEDURE'),
(10202, 10130, 'DHCP有哪三种分配方式？', '①自动分配：永久固定IP（服务器用）②动态分配：有租期，到期回收（最常用，手机/电脑用）③手动分配：IP绑定MAC地址（安全控制用）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10131: VLAN的概念与作用（node=1105）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10131, 'VLAN——虚拟局域网',
'## 什么是VLAN？

VLAN（Virtual Local Area Network，虚拟局域网）将一个**物理局域网**逻辑上划分为多个相互隔离的**广播域**。

### 一句话理解
> 一台交换机本来是一个大办公室（所有人在一起广播很吵），VLAN就是给大办公室装上玻璃隔断——分成独立的小隔间，每个隔间的广播不会干扰隔壁。

## VLAN的三大作用

| 作用 | 说明 | 实例 |
|:----:|:----:|:------:|
| **隔离广播域** | 每个VLAN是一个独立广播域，广播帧不跨VLAN | 教务VLAN的广播不影响财务VLAN |
| **提高安全性** | 不同VLAN默认不能直接通信 | 学生的VLAN不能访问行政VLAN |
| **便于管理** | 同一VLAN成员不必在物理上同一位置 | 不同楼层的财务人员同在VLAN_Finance |

### 隔离广播（最核心作用）
在没有VLAN时，交换机的广播会扩散到所有端口（广播域=整个交换机）。VLAN将一个交换机划分为多个逻辑交换机，每个VLAN的广播只在本VLAN内传播。

## VLAN实例

```
┌──────────────────────────────────────┐
│          一台物理交换机               │
├──────────────────────────────────────┤
│  VLAN 10（教务办公室）：端口1-8       │
│  VLAN 20（学生机房）：端口9-16        │
│  VLAN 30（行政办公）：端口17-24       │
└──────────────────────────────────────┘

VLAN 10内的广播只到达端口1-8
VLAN 20内的广播只到达端口9-16
```

## VLAN标识——IEEE 802.1Q

交换机通过在数据帧中插入**VLAN标签**（802.1Q协议），标记数据帧属于哪个VLAN，只有同一VLAN的端口才能收到该帧。

### VLAN帧格式
```
┌─────────┬─────────┬──────┬───────────┬─────────┐
│ 目标MAC │ 源MAC   │ 802.1Q│ 类型/长度 │ 数据     │
│ (6B)   │ (6B)    │ (4B) │ (2B)     │ (46~1500B)│
└─────────┴─────────┴──────┴───────────┴─────────┘
                      └→ VLAN ID（12位，1~4094）
```

## VLAN间通信

不同VLAN之间默认**不能直接通信**。如果VLAN 10想访问VLAN 20，需要**三层设备**转发：
- **三层交换机**：可同时工作在L2（交换）和L3（路由）
- **路由器**：通过单臂路由实现VLAN间通信

> **对口升学考点**：VLAN的核心作用是隔离广播域。记住"将一个物理LAN划分为多个逻辑VLAN"。',
'VLAN将一个物理局域网划分为多个逻辑上相互隔离的广播域。三大作用：隔离广播域（最核心）、提高安全性、便于管理。不同VLAN间默认不能通信，需三层设备转发。',
5, '单元2 组建局域网', '任务3 组建虚拟局域网', 1105,
'VLAN记法："V=Virtual（虚拟），LAN=局域网"\nVLAN就像大办公室里的隔断——物理上一个空间，逻辑上分成独立小间。\n\nVLAN三作用："隔安管"——隔离广播、安全隔离、方便管理',
'【必考】①VLAN的定义（物理LAN→逻辑VLAN）②三大作用（隔离广播/提高安全/便于管理）③VLAN间通信需要三层设备',
2,
'["VLAN","虚拟局域网","广播域","隔离广播","802.1Q"]',
'["局域网技术","VLAN"]',
'[
  {"type":"choice","question":"VLAN的核心作用是什么？","options":["提高网速","隔离广播域","加密数据","延长传输距离"],"answer":"B","explanation":"VLAN的核心作用是隔离广播域——将一个物理LAN划分为多个逻辑VLAN，广播只在本VLAN内传播。"},
  {"type":"choice","question":"同一台交换机上的两个不同VLAN之间能否直接通信？","options":["能，交换机自动转发","不能，需要三层设备","取决于端口速率","取决于传输介质"],"answer":"B","explanation":"不同VLAN默认不能直接通信，需要三层设备（三层交换机或路由器）进行路由转发。"},
  {"type":"judge","question":"VLAN可以跨越多台物理交换机，同一VLAN的成员不必在物理上相邻。","answer":"T","explanation":"VLAN是逻辑划分，通过802.1Q标签跨交换机传递，同一VLAN的成员可以在不同楼层或不同交换机上。"},
  {"type":"multi","question":"VLAN的作用有哪些？（多选）","options":["隔离广播域","提高网络安全性","简化物理布线","便于网络管理","提高传输速率"],"answer":"A,B,D","explanation":"VLAN的作用：隔离广播、提高安全、便于管理。它不改变物理布线，也不直接提高传输速率。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10203, 10131, '什么是VLAN？', 'VLAN（虚拟局域网）将一个物理局域网划分为多个逻辑上独立的子网，每个子网是一个独立的广播域。同一VLAN内的广播不扩散到其他VLAN。', 1, 'DEFINITION'),
(10204, 10131, 'VLAN有哪三大作用？', '①隔离广播域（最核心）——广播不跨VLAN②提高安全性——不同VLAN默认隔离③便于管理——逻辑分组不受物理位置限制。', 2, 'DEFINITION'),
(10205, 10131, '为什么不同VLAN之间不能直接通信？如何解决？', '不同VLAN属于不同广播域，二层交换机不能跨VLAN转发。需要通过三层设备（三层交换机或路由器）进行路由转发实现VLAN间通信。', 3, 'SCENARIO');

-- ============================================================
-- 文章10132: VLAN划分方式（node=1106）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10132, 'VLAN的划分方式——如何区分不同VLAN',
'## 四种VLAN划分方式

### 1. 基于端口划分（最常用）

将交换机的物理端口分配到不同VLAN。

**配置方式**：
```
VLAN 10：端口1-8
VLAN 20：端口9-16
VLAN 30：端口17-24
```

| 优点 | 缺点 |
|:----:|:----:|
| 配置简单直观 | 用户换端口需重新配置VLAN |
| 管理方便 | 移动性差 |

> **最常用的VLAN划分方式**，中小企业普遍使用。

### 2. 基于MAC地址划分

根据计算机网卡的MAC地址决定属于哪个VLAN。

**示例**：
```
MAC AA-AA-AA → VLAN 10
MAC BB-BB-BB → VLAN 20
MAC CC-CC-CC → VLAN 10
```

| 优点 | 缺点 |
|:----:|:----:|
| **移动性好**——用户换任意端口，自动归属原VLAN | MAC地址多时配置工作量大 |
| 安全性较高 | 更换网卡需重新配置 |

### 3. 基于协议划分

根据数据帧的协议类型划分VLAN。

| 协议类型 | VLAN |
|:--------:|:----:|
| IPv4数据帧 | VLAN 10 |
| IPv6数据帧 | VLAN 20 |

### 4. 基于子网划分

根据IP子网地址划分VLAN。

| IP子网 | VLAN |
|:------:|:----:|
| 192.168.1.0/24 | VLAN 10 |
| 192.168.2.0/24 | VLAN 20 |

## 四种方式对比总结

| 划分方式 | 配置复杂度 | 灵活性 | 主流程度 |
|:--------:|:---------:|:------:|:--------:|
| **基于端口** | 低 | 低 | ⭐⭐⭐⭐⭐ 最常用 |
| **基于MAC** | 高 | 高（移动性好） | ⭐⭐ |
| **基于协议** | 中 | 中 | ⭐ |
| **基于子网** | 中 | 中 | ⭐ |

### 考试重点
- **最常用的方式**：基于端口划分（配置简单、管理直观）
- **最灵活的方式**：基于MAC地址划分（用户可在网络内自由移动）

> **判断技巧**：如果题目描述"把端口1-10划到VLAN 10"，这是基于端口；"根据MAC地址自动归入VLAN"——基于MAC。',
'VLAN的四种划分方式：基于端口（最常用）、基于MAC地址（最灵活）、基于协议、基于子网。基于端口配置简单但移动性差；基于MAC移动性好但配置复杂。',
5, '单元2 组建局域网', '任务3 组建虚拟局域网', 1106,
'VLAN划分方式记法："端口用MAC，协议找子网"\n端口→最常用，MAC→最灵活\n\n"端口简单不灵活，MAC灵活配置多"',
'【必考】①四种VLAN划分方式 ②基于端口（最常用）vs基于MAC（最灵活）的区别 ③从场景描述判断使用了哪种划分方式',
2,
'["VLAN划分","基于端口","基于MAC","VLAN配置","端口划分","MAC划分"]',
'["局域网技术","VLAN"]',
'[
  {"type":"choice","question":"目前最常用的VLAN划分方式是哪一种？","options":["基于MAC地址划分","基于端口划分","基于协议划分","基于子网划分"],"answer":"B","explanation":"基于端口划分是最常用、最直观的VLAN划分方式——将交换机的端口分配到不同VLAN。"},
  {"type":"choice","question":"基于MAC地址划分VLAN的最大优点是什么？","options":["配置简单","用户移动性好","安全性最高","成本最低"],"answer":"B","explanation":"基于MAC划分VLAN的最大优点是移动性好——用户更换端口后自动归入原VLAN，无需重新配置。"},
  {"type":"judge","question":"基于端口的VLAN划分方式中，用户更换交换机端口后仍然属于原来的VLAN。","answer":"F","explanation":"基于端口的方式中，VLAN归属取决于端口本身。用户换到其他端口就属于新端口所在的VLAN。基于MAC才有移动性。"},
  {"type":"multi","question":"以下哪些属于VLAN的划分方式？（多选）","options":["基于端口","基于MAC地址","基于IP地址","基于协议类型","基于子网"],"answer":"A,B,D,E","explanation":"VLAN四种划分方式：基于端口、基于MAC地址、基于协议、基于子网。基于IP地址不算VLAN的划分方式。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10206, 10132, 'VLAN有哪四种划分方式？', '①基于端口（最常用）②基于MAC地址（最灵活）③基于协议（如IPv4/IPv6）④基于子网（如根据IP网段）。', 1, 'DEFINITION'),
(10207, 10132, '基于端口和基于MAC的VLAN划分各有什么优缺点？', '基于端口：配置简单但用户移动需重配。基于MAC：用户可自由移动（灵活），但MAC多时配置复杂。端口常用，MAC灵活。', 2, 'COMPARISON'),
(10208, 10132, '某员工从A办公室换到B办公室，插上网线后发现不能访问原网络，可能是什么原因？', '可能是基于端口的VLAN划分——两个办公室的交换机端口分属不同VLAN。如果要保持原VLAN，需管理员将新端口划入原VLAN，或改用基于MAC的VLAN划分。', 3, 'SCENARIO');

-- ============================================================
-- 文章10133: 无线局域网基础（node=1107）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10133, '无线局域网（WLAN）——Wi-Fi的奥秘',
'## 什么是无线局域网？

无线局域网（Wireless LAN，WLAN）采用 **IEEE 802.11 标准**，就是我们常说的 **Wi-Fi**。

### 一句话理解
> WLAN=不用插网线的局域网。手机、笔记本、智能电视通过Wi-Fi连接无线路由器上网。

## Wi-Fi标准发展历程

| 标准 | 频段 | 最大速率 | 年份 | Wi-Fi代 |
|:----:|:----:|:--------:|:----:|:--------:|
| 802.11b | 2.4GHz | 11Mbps | 1999 | Wi-Fi 1 |
| 802.11g | 2.4GHz | 54Mbps | 2003 | Wi-Fi 3 |
| **802.11n** | **2.4/5GHz** | **600Mbps** | 2009 | **Wi-Fi 4** |
| **802.11ac** | **5GHz** | **6.9Gbps** | 2013 | **Wi-Fi 5** |
| **802.11ax** | **2.4/5GHz** | **9.6Gbps** | 2019 | **Wi-Fi 6** |

### 2.4GHz vs 5GHz

| 频段 | 覆盖范围 | 穿墙能力 | 速度 | 干扰 |
|:----:|:--------:|:--------:|:----:|:----:|
| **2.4GHz** | 远 | 好 | 慢 | 多（蓝牙/微波炉干扰）|
| **5GHz** | 近 | 差 | 快 | 少 |

> **现在的路由器大多是双频的**——支持2.4GHz和5GHz同时工作。

## WLAN的组成

| 设备 | 作用 | 类比 |
|:----:|:----:|:----:|
| **无线网卡** | 计算机端的无线收发设备 | 有线网卡的无线版 |
| **AP（Access Point）** | 无线接入点，连接有线/无线网络 | 无线信号的"基站" |
| **无线路由器** | AP+路由器+交换机三合一 | 家庭网络的核心 |

### 家庭常见的连接方式
```
Internet → 光猫 → 无线路由器 → 无线连接手机/电脑/电视
                            → 有线连接台式机/打印机
```

## WLAN的安全

| 加密方式 | 安全性 | 说明 |
|:--------:|:------:|:----:|
| **WEP** | ❌ 低 | 已被破解淘汰，不要使用 |
| **WPA** | ⚠️ 中 | 过渡标准 |
| **WPA2** | ✅ **高** | **当前主流标准** |
| **WPA3** | ✅ 最高 | 最新标准，逐步普及 |

> **重要**：在无线路由器设置中，**至少选择WPA2加密**。使用WEP加密的Wi-Fi极易被破解。

### 网络安全建议
1. 不要使用WEP加密
2. 设置复杂的Wi-Fi密码（大小写字母+数字+符号）
3. 关闭WPS功能（存在安全漏洞）
4. 定期更换路由器管理密码

> **对口升学考点**：WLAN采用IEEE 802.11标准、常见Wi-Fi频段（2.4GHz/5GHz）、WPA2是当前主流无线加密方式。',
'WLAN采用IEEE 802.11标准（Wi-Fi）。主要频段：2.4GHz（远/慢/穿墙好）和5GHz（近/快/干扰少）。安全加密：WEP→WPA→WPA2（主流）→WPA3。',
5, '单元2 组建局域网', '任务4 组建无线局域网', 1107,
'Wi-Fi标准记法："b慢g快n双频ac5G ax6代"\n频段记法："2.4穿墙远但慢，5G纯净近但快"\nWPA2=Wifi Protected Access第二代=目前最主流',
'【必考】①WLAN采用IEEE 802.11标准 ②2.4GHz和5GHz的区别 ③WPA2是当前主流无线加密方式 ④常见Wi-Fi标准',
1,
'["WLAN","无线局域网","Wi-Fi","802.11","无线网络","WPA2","2.4GHz","5GHz"]',
'["局域网技术","无线局域网"]',
'[
  {"type":"choice","question":"无线局域网（WLAN）采用哪个标准系列？","options":["IEEE 802.3","IEEE 802.11","IEEE 802.1Q","IEEE 802.15"],"answer":"B","explanation":"WLAN采用IEEE 802.11标准（Wi-Fi）。802.3是以太网标准，802.1Q是VLAN标签标准。"},
  {"type":"choice","question":"Wi-Fi的2.4GHz和5GHz两个频段相比，以下哪项正确？","options":["2.4GHz速度快，5GHz覆盖远","2.4GHz覆盖远穿墙好，5GHz速度快","5GHz覆盖远穿墙好，2.4GHz速度快","两者没有区别"],"answer":"B","explanation":"2.4GHz覆盖远、穿墙好但速度慢、干扰多。5GHz速度快、干扰少但覆盖近、穿墙差。"},
  {"type":"judge","question":"WEP是目前最常见的Wi-Fi安全加密方式。","answer":"F","explanation":"WEP安全性极低，已被破解淘汰。WPA2才是当前最主流的Wi-Fi安全加密标准。"},
  {"type":"multi","question":"以下哪些属于IEEE 802.11无线局域网标准？（多选）","options":["802.11b","802.11g","802.11n","802.3","802.11ac"],"answer":"A,B,C,E","explanation":"802.11b/g/n/ac都是Wi-Fi标准。802.3是有线以太网标准，不是无线标准。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10209, 10133, '无线局域网（WLAN）采用什么标准？', 'WLAN采用IEEE 802.11标准系列，就是我们常说的Wi-Fi。常见标准：802.11b（11Mbps）、802.11g（54Mbps）、802.11n（600Mbps）、802.11ac（6.9Gbps）。', 1, 'DEFINITION'),
(10210, 10133, '2.4GHz和5GHz两个Wi-Fi频段有什么区别？', '2.4GHz：覆盖远、穿墙好、速度慢、易受干扰（蓝牙/微波炉）。5GHz：覆盖近、穿墙差、速度快、干扰少。双频路由器两者兼得。', 2, 'COMPARISON'),
(10211, 10133, 'Wi-Fi安全加密方式有哪些？从低到高排序。', 'WEP（已淘汰）→ WPA（过渡）→ WPA2（当前主流）→ WPA3（最新）。建议至少使用WPA2加密Wi-Fi。', 3, 'DEFINITION');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v171] 单元2 知识库文章种子完成：共18篇文章，约54张记忆卡片。' AS result;
-- ============================================================================
-- v172: 网络应用基础 — 知识库文章种子（单元3 管理局域网）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖5个任务9个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元3：管理局域网
-- ═══════════════════════════════════════════════════════════════
-- 任务1 使用网络操作系统（节点1108）
-- 任务2 创建和管理域（节点1109）
-- 任务3 创建DNS和DHCP服务器（节点1110~1111）
-- 任务4 配置Internet信息服务（节点1112）
-- 任务5 应用网络命令（节点1113~1116）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10134: 网络操作系统基本概念（node=1108）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10134, '网络操作系统（NOS）——网络的指挥官',
'## 什么是网络操作系统？

网络操作系统（Network Operating System，NOS）是在普通操作系统基础上增加了**网络通信、资源共享、用户管理、安全控制**等网络功能的操作系统。

### 一句话理解
> 普通操作系统只管"本机工作"（如Windows 10），网络操作系统能管"全网工作"（如Windows Server）。

## 常见网络操作系统

| 系统 | 类型 | 特点 |
|:----:|:----:|:------|
| **Windows Server** | 商业 | 图形界面友好，适合中小企业，Active Directory域管理 |
| **Linux**（CentOS/Ubuntu Server） | 开源 | 命令行为主，稳定安全，适合Web服务器 |
| **Unix** | 商业 | 历史悠久，用于大型服务器，安全性极高 |

## 网络操作系统的核心功能

| 功能 | 说明 |
|:----:|:------|
| **网络通信** | 支持TCP/IP等协议栈，数据在网络上传输 |
| **资源共享** | 文件和打印机共享（Windows的SMB/CIFS协议） |
| **用户管理** | 域控制器集中管理用户账户和权限 |
| **安全服务** | 身份验证、访问控制列表（ACL） |

## 对等网 vs 客户机/服务器模式

| 对比维度 | 对等网（Peer-to-Peer） | 客户机/服务器（C/S） |
|:--------:|:---------------------:|:-------------------:|
| 管理方式 | 分散管理——每台机自己管 | 集中管理——服务器统一管 |
| 适用规模 | 小（≤10台） | 大（10台以上） |
| 成本 | 低（无需专用服务器） | 高（需专用服务器） |
| 安全性 | 低 | 高 |
| 典型场景 | 家庭/小办公室 | 学校机房/企业 |

> **考试重点**：对等网适合小规模、无专用服务器；C/S模式适合大规模、有域控制器统一管理。Windows Server是最常见的网络操作系统之一。',
'网络操作系统在普通操作系统上增加了网络通信、资源共享、用户管理和安全控制功能。常见NOS：Windows Server（商业）、Linux（开源）、Unix。对等网（≤10台）vs C/S模式（大规模集中管理）。',
5, '单元3 管理局域网', '任务1 使用网络操作系统', 1108,
'NOS记法："网络操作系统=本机OS+网络管理功能"\n\n对等网 vs C/S："小对等、大C/S、家用对等、学校C/S"',
'【必考】①NOS的概念（普通OS+网络功能）②Windows Server是商业、Linux是开源 ③对等网（≤10台/无服务器）vs C/S模式的区别',
1,
'["网络操作系统","NOS","Windows Server","Linux","对等网","C/S模式"]',
'["网络操作系统"]',
'[
  {"type":"choice","question":"以下哪个是开源免费的网络操作系统？","options":["Windows Server 2022","Linux（Ubuntu Server）","Unix","Windows 11"],"answer":"B","explanation":"Linux是开源免费的网络操作系统。Windows Server和Unix是商业系统。Windows 11是个人电脑操作系统。"},
  {"type":"choice","question":"对等网模式最适合以下哪种场景？","options":["大型企业500人","学校机房200台","家庭/小办公室5台","银行数据中心"],"answer":"C","explanation":"对等网适合10台以下的小规模网络（如家庭和小办公室），每台计算机既当客户机又当服务器。"},
  {"type":"judge","question":"Windows Server是目前常用的商业网络操作系统之一。","answer":"T","explanation":"Windows Server是Microsoft的商业网络操作系统，提供Active Directory、文件共享等服务，广泛应用于中小企业和学校。"},
  {"type":"multi","question":"网络操作系统应具备哪些核心功能？（多选）","options":["网络通信","资源共享","用户管理","游戏娱乐","安全控制"],"answer":"A,B,C,E","explanation":"NOS核心功能：网络通信、资源共享、用户管理、安全控制。游戏娱乐不属于网络操作系统的核心功能范畴。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10212, 10134, '什么是网络操作系统（NOS）？', '在普通操作系统基础上增加了网络通信、资源共享、用户管理和安全控制功能的操作系统。常见：Windows Server（商业）、Linux（开源）。', 1, 'DEFINITION'),
(10213, 10134, '对等网和C/S模式有什么区别？', '对等网：分散管理、≤10台、无专用服务器、成本低、安全性低。C/S：集中管理、10台以上、有专用服务器、安全性高。家用小对等，学校用C/S。', 2, 'COMPARISON'),
(10214, 10134, '网络操作系统的四大核心功能？', '①网络通信（TCP/IP协议栈）②资源共享（文件/打印机）③用户管理（账户/权限）④安全服务（身份验证/ACL）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10135: 域的基本概念（node=1109）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10135, '域（Domain）——Windows网络的集中管理',
'## 什么是域？

域是Windows网络中**安全管理的边界**，由一个或多个域控制器集中管理用户账户、计算机和权限。

### 一句话理解
> 域就像"学校的学生管理系统"——所有学生信息（账户）都在教务处的服务器（域控制器）上统一管理，你在任何教室（任何计算机）登录都能认。

## 核心组件

| 组件 | 英文 | 作用 |
|:----:|:----:|:------|
| **域控制器** | Domain Controller (DC) | 运行Active Directory的服务器，集中管理账户和策略 |
| **成员计算机** | Member Computer | 加入域的客户机，使用域账户登录 |
| **Active Directory** | AD | Windows的目录服务，保存所有对象的数据库 |

## 工作组 vs 域

| 对比维度 | 工作组（Workgroup） | 域（Domain） |
|:--------:|:-----------------:|:-----------:|
| 管理模式 | **分散管理**——每台机存自己的账户 | **集中管理**——域控制器统一存 |
| 账户存储 | 每台计算机本地SAM数据库 | Active Directory数据库 |
| 适用规模 | **10台以下** | **10台以上** |
| 安全性 | 低 | 高 |
| 单点登录 | 不支持——每台机都要有账户 | 支持——一个账户全网络通用 |
| 典型场景 | 家庭局域网 | 学校机房、企业办公 |

### 工作组的典型场景
> 家庭3台电脑，每台用不同的登录密码——这就是工作组。

### 域的典型场景
> 学校机房200台电脑，学生用统一的学号登录任意一台电脑——这就是域。

## 域的好处

| 好处 | 说明 |
|:----:|:------|
| **单点登录** | 一个域账户可登录域中任意计算机 |
| **集中管理** | 管理员在域控制器上统一配置安全策略（如密码策略、软件限制） |
| **漫游配置文件** | 用户在任何计算机上登录都是自己的桌面和文件 |
| **可扩展** | 支持成千上万的用户和计算机 |

> **考试重点**：域的本质是"集中管理"——账户、安全策略都由域控制器统一管理。与工作组（分散管理）对比是常考内容。',
'域是Windows网络中安全管理的边界，由域控制器（DC）集中管理账户和权限。核心组件：域控制器、Active Directory、成员计算机。工作组（≤10台分散）vs域（大规模集中）。',
5, '单元3 管理局域网', '任务2 创建和管理域', 1109,
'域记法："域=集中管理，工作组=各管各的"\n\n"单点登录"=一个账户走遍全网络，不用每台机建账户',
'【必考】①域的概念（安全边界/集中管理）②域控制器DC的作用 ③工作组vs域的区别（规模/管理方式/安全性）④单点登录的好处',
1,
'["域","Domain","域控制器","DC","Active Directory","工作组","单点登录"]',
'["网络操作系统"]',
'[
  {"type":"choice","question":"Windows域中集中管理用户账户的服务器叫什么？","options":["DNS服务器","域控制器（DC）","Web服务器","文件服务器"],"answer":"B","explanation":"域控制器运行Active Directory，在域中集中存储和管理所有用户账户、计算机和权限。"},
  {"type":"choice","question":"工作组模式适用于多少台计算机以下的网络？","options":["5台","10台","50台","100台"],"answer":"B","explanation":"工作组适用于10台以下的小型网络，超过10台建议使用域模式集中管理。"},
  {"type":"judge","question":"域模式下，用户可以用一个账户登录域中任意一台计算机。","answer":"T","explanation":"这是域的」单点登录」功能——域账户存储在域控制器上，用户在任何加入域的计算机上都能用同一账户登录。"},
  {"type":"multi","question":"域模式相比工作组有哪些优势？（多选）","options":["集中管理用户账户","支持单点登录","无需网络即可使用","可扩展支持大量用户","安全策略统一配置"],"answer":"A,B,D,E","description":"域的优势：集中管理、单点登录、可扩展、统一安全策略。域需要网络连接才能使用域账户登录。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10215, 10135, '什么是域（Domain）？', '域是Windows网络中安全管理的边界。由域控制器（DC）通过Active Directory集中管理用户账户、计算机和权限。一个域账户可登录任意加入域的计算机。', 1, 'DEFINITION'),
(10216, 10135, '工作组和域有什么区别？', '工作组：分散管理、≤10台、每台独立存账户、安全性低。域：集中管理、10台以上、域控统一存账户、安全性高、支持单点登录。', 2, 'COMPARISON'),
(10217, 10135, '域有哪些主要好处？', '①单点登录—一个账户全网通用②集中管理—管理员统一配置③漫游配置—任何电脑都是自己的桌面④可扩展—支持成千上万的用户。', 3, 'DEFINITION');

-- ============================================================
-- 文章10136: DNS服务器的功能（node=1110）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10136, 'DNS服务器——网络中的翻译官',
'## DNS服务器的核心功能

DNS服务器的主要功能是**域名解析**——将用户输入的域名转换为对应的IP地址。

| 功能 | 说明 |
|:----:|:------|
| **正向解析** | 域名 → IP地址（最常用，如 www.baidu.com → 183.2.172.185） |
| **反向解析** | IP地址 → 域名（如 183.2.172.185 → www.baidu.com） |
| **负载均衡** | 一个域名对应多个IP，DNS轮询返回不同IP分担压力 |

## DNS服务器类型

| 类型 | 说明 |
|:----:|:------|
| **本地DNS服务器** | 用户直接查询的服务器（ISP提供或学校自建） |
| **根DNS服务器** | 全球13组，知道所有顶级域（.com/.cn等）的位置 |
| **权威DNS服务器** | 存储特定域名的最终解析记录（如baidu.com的DNS） |

## 常见DNS记录类型

| 记录类型 | 作用 | 示例 |
|:--------:|:----:|:----:|
| **A记录** | 域名 → IPv4地址 | www.example.com → 192.168.1.10 |
| **AAAA记录** | 域名 → IPv6地址 | www.example.com → 2001:db8::1 |
| **MX记录** | 指定邮件服务器 | @ → mail.example.com |
| **CNAME记录** | 域名别名（指向另一个域名） | m.example.com → www.example.com |

### 记录类型记忆
> **A记录=Address（地址），AAAA=IPv6地址，MX=Mail eXchange（邮件交换）**

## 实际应用场景

- **学校自建DNS服务器**：加速校内网站访问，过滤不良网站
- **域名注册商**：提供DNS解析服务（如阿里云DNS、腾讯云DNS）
- **常用公共DNS**：114.114.114.114（国内快速）、8.8.8.8（Google全球）

> **考试重点**：DNS服务器的核心功能是域名→IP的解析。A记录=域名→IPv4，AAAA=域名→IPv6，MX=邮件服务器。',
'DNS服务器的核心功能是将域名解析为IP地址。常见记录类型：A（域名→IPv4）、AAAA（域名→IPv6）、MX（邮件服务器）。服务器类型：本地DNS、根DNS、权威DNS。',
5, '单元3 管理局域网', '任务3 创建DNS和DHCP服务器', 1110,
'DNS记法："DNS=Domain Name System=域名变IP"\n记录类型："A=Address地址，AAAA=IPv6，MX=Mail邮件"',
'【必考】①DNS核心功能：域名→IP地址 ②A记录（域名→IPv4）③MX记录（邮件服务器）④DNS服务器类型',
2,
'["DNS服务器","域名解析","A记录","AAAA记录","MX记录","域名系统"]',
'["TCP/IP协议与IP地址","DNS"]',
'[
  {"type":"choice","question":"DNS服务器中A记录的作用是什么？","options":["域名转为IPv4地址","域名转为IPv6地址","指定邮件服务器","设置域名别名"],"answer":"A","explanation":"A记录（Address Record）将域名解析为IPv4地址。AAAA记录才是用于IPv6。"},
  {"type":"choice","question":"MX记录在DNS中的作用是什么？","options":["域名解析到IPv6","指定邮件服务器","域名转发","负载均衡"],"answer":"B","explanation":"MX（Mail eXchange）记录指定处理该域名邮件的服务器地址，用于电子邮件系统。"},
  {"type":"judge","question":"全球共有13组根DNS服务器。","answer":"T","explanation":"根DNS服务器全球共有13组（编号A~M），由ICANN管理。它们知道所有顶级域DNS服务器的位置。"},
  {"type":"multi","question":"DNS服务器中常见的记录类型有哪些？（多选）","options":["A记录","AAAA记录","MX记录","DHCP记录","CNAME记录"],"answer":"A,B,C,E","explanation":"常见DNS记录：A（IPv4）、AAAA（IPv6）、MX（邮件）、CNAME（别名）。DHCP不是DNS记录类型。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10218, 10136, 'DNS服务器的核心功能是什么？', '将域名解析为IP地址（正向解析），也可将IP地址解析为域名（反向解析）。负载均衡：一个域名对应多个IP，轮询返回。', 1, 'DEFINITION'),
(10219, 10136, 'DNS的A记录、AAAA记录和MX记录各有什么作用？', 'A记录：域名→IPv4地址。AAAA记录：域名→IPv6地址。MX记录：指定域名的邮件服务器地址。', 2, 'DEFINITION'),
(10220, 10136, 'DNS服务器有哪几种类型？', '①本地DNS服务器（用户直接查询）②根DNS服务器（全球13组，知道顶级域位置）③权威DNS服务器（存储域名最终解析记录）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10137: DHCP服务器的功能（node=1111）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10137, 'DHCP服务器——自动分配网络参数',
'## DHCP服务器的核心功能

DHCP服务器在网络中**自动分配IP地址**给客户端，并配置子网掩码、默认网关、DNS等参数。

| 功能 | 说明 |
|:----:|:------|
| **自动分配IP** | 客户端开机自动获取IP，无需手动配置 |
| **避免冲突** | 保证同一网络中不分配重复的IP地址 |
| **集中管理** | 在DHCP服务器上统一修改网络配置（如更换DNS） |

## DHCP分配的网络参数

| 参数 | 说明 |
|:----:|:------|
| **IP地址** | 分配给客户端的网络地址 |
| **子网掩码** | 确定网络范围 |
| **默认网关** | 访问外网的出口 |
| **DNS服务器** | 域名解析服务地址 |
| **租期** | IP地址的有效使用时间（到期自动更新） |

## DHCP的工作过程（回顾）

```
客户端                   DHCP服务器
  │── DHCP Discover ───→  │  ①广播找服务器
  │←── DHCP Offer ──────  │  ②提供可用IP
  │── DHCP Request ────→  │  ③请求使用该IP
  │←── DHCP ACK ────────  │  ④确认分配
```

## 常见故障：169.254.x.x

当客户端无法联系到DHCP服务器时（如网络故障、DHCP服务停止），Windows会自动分配一个**169.254.x.x**的APIPA地址。

| 现象 | 原因 | 解决 |
|:----:|:----:|:------|
| IP显示169.254.x.x | DHCP服务器不可达 | 检查网线、重启DHCP服务、检查网络连通性 |
| 提示"IP地址冲突" | 局域网内有重复IP | 使用DHCP自动分配、检查手动配置 |

> **故障排查口诀**："IP变成169.254，DHCP没找到"。

> **考试重点**：DHCP自动分配IP等参数，169.254.x.x是DHCP失败的典型表现。',
'DHCP服务器自动分配IP地址、子网掩码、默认网关、DNS等参数。四步过程：DORA（发现→提供→请求→确认）。IP为169.254.x.x表示DHCP获取失败。',
5, '单元3 管理局域网', '任务3 创建DNS和DHCP服务器', 1111,
'169.254.x.x记法："16(9)2(5)4=DHCP失(4)败"—看到169.254就知道DHCP出问题了\n\nDORA口诀：发现→提供→请求→确认',
'【必考】①DHCP自动分配IP/掩码/网关/DNS ②DORA四步过程 ③169.254.x.x=DHCP失败 ④DHCP避免IP冲突',
1,
'["DHCP服务器","自动分配IP","169.254.x.x","DORA","APIPA"]',
'["TCP/IP协议与IP地址","DHCP"]',
'[
  {"type":"choice","question":"当计算机的IP地址显示为169.254.x.x时，通常表示什么？","options":["网络正常","DHCP服务器获取IP失败","DNS解析错误","网卡损坏"],"answer":"B","explanation":"169.254.x.x是APIPA自动私有地址，表示DHCP服务器不可达，Windows自动分配了该地址。"},
  {"type":"choice","question":"DHCP服务器的核心作用是什么？","options":["解析域名到IP","自动分配IP地址等网络参数","加密网络通信","路由数据包"],"answer":"B","explanation":"DHCP服务器自动为网络中的客户端分配IP地址、子网掩码、默认网关和DNS等参数。"},
  {"type":"judge","question":"DHCP服务器可以避免局域网内IP地址冲突。","answer":"T","explanation":"DHCP服务器维护已分配IP的记录，不会重复分配同一IP给不同设备，从而避免IP地址冲突。"},
  {"type":"multi","question":"DHCP服务器可以为客户端分配哪些网络参数？（多选）","options":["IP地址","子网掩码","默认网关","DNS服务器","MAC地址"],"answer":"A,B,C,D","explanation":"DHCP分配IP地址、子网掩码、默认网关和DNS服务器。MAC地址是网卡硬件地址，不由DHCP分配。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10221, 10137, 'DHCP服务器的核心功能是什么？', '自动为网络客户端分配IP地址、子网掩码、默认网关、DNS服务器等参数。四步过程：Discover→Offer→Request→ACK（DORA）。', 1, 'DEFINITION'),
(10222, 10137, 'IP地址为169.254.x.x是什么意思？如何排查？', '表示DHCP服务器不可达，Windows自动分配了APIPA地址。排查：检查网线连接、重启DHCP服务、检查DHCP服务器是否在线。', 2, 'APPLICATION'),
(10223, 10137, 'DHCP的DORA四步过程是什么？', 'D=Discover（广播找服务器）→O=Offer（服务器提供IP）→R=Request（请求使用）→A=ACK（确认分配）。', 3, 'PROCEDURE');

-- ============================================================
-- 文章10138: Internet信息服务IIS（node=1112）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10138, 'IIS——Windows下的Web服务器',
'## 什么是IIS？

IIS（Internet Information Services）是Microsoft Windows系统自带的**Web服务器软件**，用于发布网站和Web应用程序。

### 一句话理解
> IIS就是把你的电脑变成"网站服务器"的Windows自带功能——别人可以通过浏览器访问你电脑上的网页。

## IIS支持的服务

| 服务 | 说明 | 默认端口 |
|:----:|:------:|:--------:|
| **HTTP/HTTPS** | 发布网站 | 80/443 |
| **FTP** | 文件传输服务 | 21 |
| **SMTP** | 简易邮件发送 | 25 |

## 常见Web服务器对比

| 服务器 | 平台 | 开源 | 适用场景 |
|:------:|:----:|:----:|:---------|
| **IIS** | Windows | 否 | 中小型企业、学校（ASP.NET网站） |
| **Apache** | 跨平台 | 是（全球第一） | Linux服务器、PHP网站 |
| **Nginx** | 跨平台 | 是 | 高并发、静态资源、反向代理 |

## IIS的实际应用

- **学校内部网站**：OA办公系统、教学管理系统
- **ASP.NET网站发布**：Windows平台下的Web应用
- **FTP文件共享**：通过IIS搭建内部FTP服务器

### 启用IIS
> 在Windows Server/Windows中：控制面板 → 启用或关闭Windows功能 → 勾选Internet Information Services。

> **考试重点**：IIS是Microsoft Windows下的Web服务器软件，用于发布网站。知道它是微软的产品即可。',
'IIS是Microsoft Windows自带的Web服务器软件，用于发布网站（HTTP/HTTPS 80/443）、FTP服务（21）和SMTP服务（25）。常见Web服务器对比：IIS（Windows/商业）、Apache（跨平台/开源）、Nginx（跨平台/高并发）。',
5, '单元3 管理局域网', '任务4 配置Internet信息服务', 1112,
'IIS记法："IIS=I Internet Services=微软的Web服务器"\n\n"要发网站用IIS（微软）、用PHP用Apache（开源）、要高并发用Nginx"',
'【必考】①IIS是Microsoft的Web服务器软件 ②IIS可发布HTTP/HTTPS/FTP服务 ③常见Web服务器：IIS/Apache/Nginx',
1,
'["IIS","Internet信息服务","Web服务器","Windows Server","网站发布"]',
'["Internet信息服务","IIS"]',
'[
  {"type":"choice","question":"IIS是哪个公司的产品？","options":["Google","Microsoft","IBM","Oracle"],"answer":"B","explanation":"IIS（Internet Information Services）是Microsoft公司的Web服务器软件，随Windows系统提供。"},
  {"type":"choice","question":"IIS默认用于发布Web网站的端口是？","options":["21","25","80","110"],"answer":"C","explanation":"IIS默认HTTP端口80用于发布网站。21是FTP、25是SMTP、110是POP3。"},
  {"type":"judge","question":"IIS不仅支持Web服务，还支持FTP和SMTP服务。","answer":"T","explanation":"IIS集成多种服务：HTTP/HTTPS（Web网站）、FTP（文件传输）和SMTP（邮件发送）。"},
  {"type":"multi","question":"以下哪些是常见的Web服务器软件？（多选）","options":["IIS","Apache","Nginx","MySQL","Tomcat"],"answer":"A,B,C,E","explanation":"常见的Web服务器：IIS（微软）、Apache（开源第一）、Nginx（高并发）、Tomcat（Java）。MySQL是数据库，不是Web服务器。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10224, 10138, '什么是IIS？它可以提供哪些服务？', 'IIS（Internet Information Services）是微软Windows自带的Web服务器软件。支持HTTP/HTTPS（80/443）、FTP（21）、SMTP（25）等服务。', 1, 'DEFINITION'),
(10225, 10138, 'IIS、Apache和Nginx有什么区别？', 'IIS：Windows平台、商业软件、适合ASP.NET。Apache：跨平台、开源、全球使用最广、适合PHP。Nginx：跨平台、开源、高并发能力强。', 2, 'COMPARISON'),
(10226, 10138, 'IIS主要用于什么场景？', '用于在Windows服务器上发布网站和Web应用程序。常见场景：学校内部OA系统、教学管理系统、企业门户网站。', 3, 'APPLICATION');

-- ============================================================
-- 文章10139: ping命令（node=1113）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10139, 'ping命令——网络连通性的听诊器',
'## ping命令的功能

ping是**最常用的网络诊断命令**，用于测试本机与目标主机之间的网络连通性。

### 一句话理解
> ping就像"你喊一声看有没有人应"——你发一个数据包过去，对方回一个包过来，就说明网络通了。

## 工作原理

ping基于**ICMP**（Internet Control Message Protocol，网际控制报文协议）：

```
本机 ── ICMP Echo Request ──→ 目标主机
本机 ←─ ICMP Echo Reply ──── 目标主机
```

## 常用用法

| 命令 | 作用 |
|:----:|:------|
| `ping 192.168.1.1` | 测试到网关的连通性 |
| `ping www.baidu.com` | 测试到外网的连通性（还能检测DNS是否解析正常） |
| `ping 127.0.0.1` | 测试本机TCP/IP协议栈是否正常 |
| `ping -t 192.168.1.1` | 持续ping（按Ctrl+C停止） |
| `ping -n 10 192.168.1.1` | 发送10个ping包后停止 |

## 结果解读

| 输出 | 含义 |
|:----:|:------|
| `来自 192.168.1.1 的回复: 字节=32 时间<1ms TTL=64` | **网络通** ✓ |
| `请求超时。` | **网络不通** ✗ |
| `来自 192.168.1.1 的回复: 无法访问目标主机` | **路由不可达** |
| `找不到主机` | **DNS解析失败** |

## 故障排查顺序（由内到外）

| 步骤 | 命令 | 排查内容 |
|:----:|:----:|:---------|
| **①** | `ping 127.0.0.1` | 本机TCP/IP协议栈是否正常 |
| **②** | `ping 本机IP` | 网卡和IP配置是否正确 |
| **③** | `ping 网关IP` | 局域网连通性（网线/交换机） |
| **④** | `ping 外网域名` | Internet连通性 + DNS解析 |

### 排查口诀
> **"先ping自己再ping网关，最后ping百度看外网。"**

> **对口升学必考**：ping是最常用的网络诊断命令，基于ICMP协议。故障排查按"本机→网关→外网"的顺序进行。',
'ping基于ICMP协议测试网络连通性。常用用法：ping 127.0.0.1（测本机）、ping 网关（测局域网）、ping 域名（测外网+DNS）。故障排查顺序：本机→网关→外网。',
5, '单元3 管理局域网', '任务5 应用网络命令', 1113,
'ping排查口诀："先ping自己127.0.0.1，再ping网关看局域网，最后ping百度测外网"\n\n-ping结果：有回复=通，超时=不通',
'【必考】①ping基于ICMP协议 ②ping 127.0.0.1测本机协议栈 ③故障排查顺序（本机→网关→外网）④-t持续ping、-n指定次数',
2,
'["ping","网络诊断","ICMP","连通性测试","网络命令"]',
'["常用网络命令","ping"]',
'[
  {"type":"choice","question":"ping命令基于哪个协议工作？","options":["TCP","UDP","ICMP","HTTP"],"answer":"C","explanation":"ping基于ICMP（网际控制报文协议），通过发送Echo Request和接收Echo Reply测试连通性。"},
  {"type":"choice","question":"网络故障排查时，ping命令的第一步应该做什么？","options":["ping百度","ping网关","ping 127.0.0.1","ping 114.114.114.114"],"answer":"C","explanation":"故障排查由内到外：第一步ping 127.0.0.1测试本机TCP/IP协议栈是否正常。"},
  {"type":"judge","question":"ping命令用于测试网络连通性，能收到回复说明网络是通的。","answer":"T","explanation":"ping通=本机到目标主机之间网络连通。ping不通可能的原因：网络断开、防火墙拦截、目标主机未开机。"},
  {"type":"multi","question":"以下关于ping命令的说法，正确的有哪些？（多选）","options":["基于ICMP协议","-t参数可以持续ping","-n参数指定发送次数","ping不通一定是网线断了","127.0.0.1是本机地址"],"answer":"A,B,C,E","explanation":"ping基于ICMP、-t持续、-n指定次数、127.0.0.1本机均正确。ping不通有多种原因（防火墙/未开机等），不一定是网线断了。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10227, 10139, 'ping命令的功能和原理是什么？', 'ping测试网络连通性，基于ICMP协议。本机发Echo Request→目标回Echo Reply→收到回复即通。', 1, 'DEFINITION'),
(10228, 10139, '网络故障排查时ping命令应如何按顺序使用？', '①ping 127.0.0.1（测本机协议栈）②ping本机IP（测网卡）③ping网关（测局域网）④ping百度（测外网+DNS）。由内到外逐级排查。', 2, 'PROCEDURE'),
(10229, 10139, 'ping命令的常用参数有哪些？', 'ping -t（持续ping，Ctrl+C停止）、ping -n 10（发送10个包）、ping -l 1000（设置包大小）。', 3, 'APPLICATION');

-- ============================================================
-- 文章10140: ipconfig命令（node=1114）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10140, 'ipconfig命令——查看网络配置的利器',
'## ipconfig命令的功能

ipconfig是Windows系统中**查看IP配置信息**的网络命令。

### 一句话理解
> ipconfig就像"查看你的网络身份证"——你的IP地址、子网掩码、网关是多少，一看便知。

## 常用用法

| 命令 | 作用 |
|:----:|:------|
| `ipconfig` | 查看基本IP配置（IP地址、子网掩码、默认网关） |
| `ipconfig /all` | 查看**详细配置**（含MAC地址、DNS、DHCP信息、主机名） |
| `ipconfig /release` | 释放当前DHCP获得的IP地址 |
| `ipconfig /renew` | 重新向DHCP服务器申请IP地址 |
| `ipconfig /flushdns` | 清除DNS缓存（解决域名解析异常） |

## 输出解读

### ipconfig（基本）
```
以太网适配器 本地连接:
   IPv4 地址 . . . . . . . . . . . : 192.168.1.100   ← 本机IP
   子网掩码 . . . . . . . . . . . . : 255.255.255.0   ← 子网掩码
   默认网关 . . . . . . . . . . . . : 192.168.1.1     ← 路由器IP
```

### ipconfig /all（详细）
```
   物理地址 . . . . . . . . . . . . : 00-1A-2B-3C-4D-5E  ← MAC地址
   DHCP 已启用 . . . . . . . . . . : 是                ← 是否使用DHCP
   DHCP 服务器 . . . . . . . . . . : 192.168.1.1       ← DHCP服务器
   DNS 服务器 . . . . . . . . . . : 114.114.114.114   ← DNS服务器
```

## 故障诊断

| 现象 | 含义 |
|:----:|:------|
| IP为**169.254.x.x** | DHCP获取失败，自动分配了APIPA地址 |
| 显示**"媒体已断开"** | 网线未插好或网卡被禁用 |
| DNS缓存有问题 | 能ping通IP但打不开网页 → 用`ipconfig /flushdns`清除缓存 |

### 实用场景
> ①查看本机IP地址和MAC地址 → `ipconfig /all`
> ②修改了DHCP配置后让电脑重新获取IP → 先`ipconfig /release`再`ipconfig /renew`
> ③DNS解析异常 → `ipconfig /flushdns`

> **对口升学考点**：ipconfig查看IP配置、/all查看详细信息、/release释放IP、/renew重新获取、/flushdns刷新DNS缓存。',
'ipconfig查看本机IP配置。常用参数：/all（详细信息）、/release（释放IP）、/renew（重新获取）、/flushdns（清除DNS缓存）。169.254.x.x=DHCP异常，"媒体已断开"=网线未插好。',
5, '单元3 管理局域网', '任务5 应用网络命令', 1114,
'ipconfig参数记法："/all=全部信息，/release=释放IP，/renew=重新获取，/flushdns=刷DNS"',
'【必考】①ipconfig查看IP配置 ②/all查看MAC地址/DNS等详细信息 ③/release释放+/renew重新获取 ④/flushdns清除DNS缓存 ⑤169.254.x.x表示DHCP失败',
2,
'["ipconfig","网络配置","IP查看","DHCP","DNS缓存","网络命令"]',
'["常用网络命令","ipconfig"]',
'[
  {"type":"choice","question":"在ipconfig命令中，查看详细信息包含MAC地址和DNS应使用哪个参数？","options":["/release","/renew","/all","/flushdns"],"answer":"C","explanation":"ipconfig /all显示所有网络适配器的详细信息，包括MAC地址、DHCP服务器、DNS服务器等。"},
  {"type":"choice","question":"清除DNS缓存应使用哪个命令？","options":["ipconfig /release","ipconfig /flushdns","ipconfig /renew","ipconfig /all"],"answer":"B","explanation":"ipconfig /flushdns清除本地DNS缓存，适用于DNS解析异常时（能ping通IP但打不开网页）。"},
  {"type":"judge","question":"ipconfig /renew用于释放当前DHCP获取的IP地址。","answer":"F","explanation":"ipconfig /release才是释放IP，/renew是重新向DHCP服务器申请IP。两者常配合使用：先release释放，再renew重新获取。"},
  {"type":"multi","question":"以下哪些是ipconfig命令的常用参数？（多选）","options":["/all","/release","/renew","/flushdns","/tracert"],"answer":"A,B,C,D","explanation":"ipconfig常用参数：/all（详细信息）、/release（释放IP）、/renew（重新获取）、/flushdns（清除DNS缓存）。/tracert不是ipconfig的参数。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10230, 10140, 'ipconfig命令常用参数有哪些？', 'ipconfig（基本IP信息）、ipconfig /all（详细信息含MAC/DNS）、ipconfig /release（释放IP）、ipconfig /renew（重新获取）、ipconfig /flushdns（清除DNS缓存）。', 1, 'DEFINITION'),
(10231, 10140, 'ipconfig /all可以查看到哪些信息？', '本机所有网络适配器的详细配置：IP地址、子网掩码、默认网关、MAC地址（物理地址）、DNS服务器、DHCP状态等。', 2, 'APPLICATION'),
(10232, 10140, 'ipconfig的/release和/renew在什么场景下使用？', '修改了DHCP服务器配置后，让客户端重新获取IP：先运行ipconfig /release释放旧IP，再运行ipconfig /renew向DHCP申请新IP。', 3, 'PROCEDURE');

-- ============================================================
-- 文章10141: tracert命令（node=1115）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10141, 'tracert命令——追踪路由的导航图',
'## tracert命令的功能

tracert（Trace Route）用于**追踪从本机到目标主机所经过的每一跳路由器**——也就是你的数据包走了哪些"中转站"才到达目的地。

### 一句话理解
> tracert就像"查快递物流"——从发货到收货经过哪些中转站、每站花了多少时间，一目了然。

## 工作原理

利用IP数据包中的**TTL**（Time To Live，生存时间）字段：

- TTL初始值为1（第一跳路由器收到后TTL-1=0，回送超时信息→记录第一跳）
- TTL逐跳递增（2、3、4...），直到到达目标
- 每经过一个路由器TTL减1，TTL归零时路由器返回ICMP超时信息

```
本机 → 路由器A（TTL=1超时→记录IP）→ 路由器B（TTL=2超时→记录IP）→ ... → 目标
```

## 常用用法

| 命令 | 作用 |
|:----:|:------|
| `tracert www.baidu.com` | 追踪到百度服务器的路由路径 |
| `tracert 192.168.1.1` | 追踪到网关（通常只有1跳） |
| `tracert -d www.baidu.com` | 不解析IP为主机名（速度更快） |

## 输出解读

```
通过最多 30 个跃点跟踪到 www.baidu.com [110.242.68.66]:

  1    <1 ms    <1 ms    <1 ms  192.168.1.1        ← 第1跳：本地网关（路由器）
  2     2 ms     1 ms     2 ms  10.0.0.1           ← 第2跳：ISP接入点
  3     5 ms     4 ms     5 ms  61.148.3.45        ← 第3跳：城市骨干网
  4     *        *        *     请求超时。           ← 第4跳：某路由器不回应（常见）
  ...
 10    30 ms    31 ms    30 ms  110.242.68.66      ← 最终跳：目标服务器
```

### 输出字段含义
| 字段 | 含义 |
|:----:|:------|
| 第1列 | 跳数（从1到30） |
| 第2~4列 | 3次探测的往返时间（ms） |
| 第5列 | 该跳路由器的IP地址或主机名 |

## 故障诊断

| 现象 | 可能原因 |
|:----:|:---------|
| 某跳之后全部显示`* * * 请求超时` | 该处路由器不响应ICMP（或链路中断） |
| 跳数超过20跳仍未到达 | 目标太远，或有路由环路 |
| 某些路由器显示`*`但后续跳正常 | 个别路由器禁ping，不影响连通性 |

> **考试重点**：tracert用于追踪路由路径，原理基于TTL（生存时间）字段。最大跳数为30。',
'tracert追踪从本机到目标所经过的所有路由器。原理：利用TTL（生存时间）字段逐跳递增探测。最大30跳。每跳显示3次往返时间和路由器IP。',
5, '单元3 管理局域网', '任务5 应用网络命令', 1115,
'tracert记法："tracert=Trace Route=追踪路径"\n\n原理记法："TTL=1去第一跳，TTL=2去第二跳...逐跳递增"\n最大跳数30（默认）',
'【必考】①tracert的功能（追踪路由路径）②基于TTL原理 ③最大30跳 ④输出结果解读（跳数+时间+IP）',
2,
'["tracert","路由追踪","TTL","网络诊断","网络命令","路由路径"]',
'["常用网络命令","tracert"]',
'[
  {"type":"choice","question":"tracert命令的主要功能是什么？","options":["测试网络连通性","查看IP配置","追踪本机到目标的路由路径","查看网络连接状态"],"answer":"C","explanation":"tracert追踪数据包从本机到目标主机经过的所有路由器（每一跳）。ping测试连通性，tracert查路径。"},
  {"type":"choice","question":"tracert命令利用IP数据包中的哪个字段来逐跳探测？","options":["TTL（生存时间）","IP标识符","协议类型","校验和"],"answer":"A","explanation":"tracert利用TTL字段：TTL=1时第一跳路由器返回超时，TTL=2时第二跳返回……逐跳递增直到目标。"},
  {"type":"judge","question":"tracert命令的默认最大跳数是30跳。","answer":"T","explanation":"tracert默认最多追踪30跳。如果超过30跳仍未到达目标，则停止追踪并提示超出最大跳数。"},
  {"type":"multi","question":"以下哪些是Windows系统自带的网络诊断命令？（多选）","options":["ping","ipconfig","tracert","netstat","msconfig"],"answer":"A,B,C,D","explanation":"ping、ipconfig、tracert、netstat都是Windows自带的网络诊断命令。msconfig是系统配置工具，不是网络命令。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10233, 10141, 'tracert命令的功能和原理是什么？', 'tracert追踪从本机到目标主机经过的每一跳路由器。原理：利用TTL（生存时间）字段逐跳递增，TTL归零时路由器返回信息，从而记录该跳IP。', 1, 'DEFINITION'),
(10234, 10141, '如何解读tracert的输出结果？', '每行显示一跳：左起跳数→3次往返时间（ms）→路由器IP。如"1 <1ms <1ms <1ms 192.168.1.1"表示第一跳是本地网关。*表示该跳路由器不响应。', 2, 'APPLICATION'),
(10235, 10141, 'tracert和ping有什么区别？', 'ping：测试网络连通性（通不通）。tracert：追踪路由路径（怎么走）。ping查"能不能到"，tracert查"怎么到"。', 3, 'COMPARISON');

-- ============================================================
-- 文章10142: netstat命令（node=1116）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10142, 'netstat命令——网络连接的监控器',
'## netstat命令的功能

netstat（Network Statistics）用于显示本机的**网络连接状态**、监听端口、路由表等网络统计信息。

### 一句话理解
> netstat就像"网络连接的全景监控"——你的电脑正在和谁通信、哪些程序在监听端口、有没有可疑连接，一目了然。

## 常用用法

| 命令 | 作用 |
|:----:|:------|
| `netstat` | 显示所有活动TCP连接 |
| `netstat -a` | 显示**所有连接和监听端口** |
| `netstat -n` | 以数字形式显示地址和端口（不解析名称） |
| `netstat -an` | **最常用组合**——-a查看所有，-n数字显示 |
| `netstat -o` | 显示每个连接对应的进程PID |
| `netstat -b` | 显示进程程序名（需要管理员权限） |

## 输出解读

```
活动连接

  协议    本地地址                外部地址                状态
  TCP    192.168.1.100:49723     110.242.68.66:443      ESTABLISHED
  TCP    0.0.0.0:80              0.0.0.0:0              LISTENING
  TCP    127.0.0.1:3306          0.0.0.0:0              LISTENING
  UDP    0.0.0.0:53              *:*                    （UDP无状态）
```

### 常见连接状态

| 状态 | 含义 |
|:----:|:------|
| **ESTABLISHED** | 已建立连接——正在通信（正常上网时的状态） |
| **LISTENING** | 正在监听——等待别人来连接（服务器在等待请求） |
| **TIME_WAIT** | 连接已关闭，等待清理（短暂出现） |
| **CLOSE_WAIT** | 对方已关闭连接，本机等待关闭 |

## 实用场景

### ① 排查端口占用
> 启动服务时提示"端口被占用"：
> ```
> netstat -ano | findstr :8080
> ```
> 查看占用8080端口的进程PID → 在任务管理器中结束该进程。

### ② 检测木马/异常连接
> 运行 `netstat -an` 查看当前所有连接：
> - 看到大量不认识的IP在连接→可能有木马
> - 看到异常端口在LISTENING→可能有后门

### ③ 查看本机开启的服务
> `netstat -an` 显示LISTENING状态的端口就是本机正在提供的服务。

> **对口升学考点**：netstat查看本机网络连接和端口状态。-an最常用（查看所有连接+数字显示）。ESTABLISHED=已连接，LISTENING=正在监听。',
'netstat显示本机网络连接和端口状态。常用参数：-a（所有连接）、-n（数字显示）、-an（最常用组合）、-o（显示PID）。ESTABLISHED（已连接）、LISTENING（正在监听）。',
5, '单元3 管理局域网', '任务5 应用网络命令', 1116,
'netstat记法："netstat=网络状态"\n-an组合="看所有连接的不带名字（数字显示）"\n\n状态记忆：ESTABLISHED=已经连上了，LISTENING=等着人来连',
'【必考】①netstat查看网络连接状态 ②-an是最常用组合参数 ③ESTABLISHED（已连接）④LISTENING（正在监听）⑤排查端口占用方法',
2,
'["netstat","网络连接","端口状态","ESTABLISHED","LISTENING","网络命令"]',
'["常用网络命令","netstat"]',
'[
  {"type":"choice","question":"netstat命令中，」LISTENING「状态表示什么？","options":["已建立连接","正在监听等待连接","连接已关闭","数据正在传输"],"answer":"B","explanation":"LISTENING表示该端口正在监听中，等待客户端发起连接。如Web服务器在80端口LISTENING等待浏览器访问。"},
  {"type":"choice","question":"要查看本机所有网络连接和监听端口，且以数字显示，最常用的命令是什么？","options":["netstat","netstat -a","netstat -an","netstat -b"],"answer":"C","explanation":"netstat -an是最常用的组合——-a显示所有连接和监听端口，-n用数字显示地址和端口（不解析主机名）。"},
  {"type":"judge","question":"ESTABLISHED状态表示该端口正在监听，等待客户端连接。","answer":"F","explanation":"ESTABLISHED表示已建立连接，正在通信中。LISTENING才是正在监听等待连接的状态。"},
  {"type":"multi","question":"以下哪些是netstat命令的常用参数？（多选）","options":["-a","-n","-o","-b","-t"],"answer":"A,B,C,D","explanation":"netstat -a（所有连接）、-n（数字显示）、-o（显示PID）、-b（显示程序名）都是常用参数。-t不是netstat的参数。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10236, 10142, 'netstat命令的功能和常用参数？', 'netstat显示本机网络连接状态和端口信息。常用：-a（全部连接）、-n（数字显示）、-an（最常用）、-o（显示PID）、-b（显示程序名需管理员）。', 1, 'DEFINITION'),
(10237, 10142, 'ESTABLISHED和LISTENING状态分别代表什么？', 'ESTABLISHED=已建立连接，正在通信（如浏览器正在访问网站）。LISTENING=正在监听端口，等待连接（如Web服务器等待浏览器访问）。', 2, 'DEFINITION'),
(10238, 10142, '如何用netstat排查端口占用问题？', '运行netstat -ano | findstr :端口号（如8080）。找到占用该端口的PID，在任务管理器中结束对应的进程。', 3, 'APPLICATION');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v172] 单元3 知识库文章种子完成：共9篇文章，约27张记忆卡片。' AS result;
-- ============================================================================
-- v173: 网络应用基础 — 知识库文章种子（单元4 畅游Internet）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖5个任务9个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元4：畅游Internet
-- ═══════════════════════════════════════════════════════════════
-- 任务1 接入Internet（节点1117~1118）
-- 任务2 应用WWW服务（节点1119~1122）
-- 任务3 应用FTP服务（节点1123）
-- 任务4 应用Email服务（节点1124）
-- 任务5 应用远程登录服务（节点1125）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10143: Internet的概念与发展（node=1117）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10143, 'Internet——全球最大的网际网',
'## 什么是Internet？

Internet（因特网）是全球最大的、开放的、由众多网络互连而成的**网际网**。

### 核心特征
- 基于**TCP/IP**协议族
- 全球互联，无中心管理节点
- 提供WWW、Email、FTP等服务

> Internet ≠ WWW：Internet是"高速公路网"，WWW是"跑在高速上的快递服务"。

## Internet发展简史

| 时间 | 里程碑 | 说明 |
|:----:|:------:|:------|
| **1969年** | **ARPANET诞生** | 美国国防部高级研究计划局，最初4个节点 |
| **1974年** | TCP/IP雏形 | Kahn和Cerf提出TCP/IP协议 |
| **1983年** | **TCP/IP正式启用** | ARPANET全面切换到TCP/IP，现代Internet诞生 |
| **1989年** | WWW发明 | Tim Berners-Lee提出万维网概念 |
| **1991年** | WWW发布 | 第一个网站上线 |
| **1994年** | **中国全功能接入** | 通过中科院高能所接入Internet |
| 1998年 | Google成立 | 搜索引擎时代 |
| 今天 | 物联网/云计算/AI | 数十亿设备在线 |

## 中国互联网的关键节点

| 时间 | 事件 |
|:----:|:------|
| **1994年4月20日** | **中国全功能接入Internet**（通过中科院高能所64K专线） |
| CNNIC成立 | 中国互联网络信息中心，管理.cn域名和IP地址 |

## Internet提供的主要服务

| 服务 | 英文 | 说明 |
|:----:|:----:|:------|
| **万维网** | **WWW** | 网页浏览（最广泛使用的服务） |
| **电子邮件** | **Email** | 收发邮件 |
| **文件传输** | **FTP** | 上传和下载文件 |
| **远程登录** | **Telnet/SSH** | 远程管理计算机 |
| **即时通信** | IM | QQ、微信等 |

> **必考**：Internet的前身是**ARPANET**，Internet基于**TCP/IP**协议。中国在**1994年**全功能接入Internet。',
'Internet是全球最大的网际网，基于TCP/IP协议。前身是1969年的ARPANET。1983年TCP/IP正式启用。中国于1994年全功能接入。提供的服务：WWW、Email、FTP等。',
5, '单元4 畅游Internet', '任务1 接入Internet', 1117,
'互联网记法："1969ARPANET诞生，1983TCP/IP立功，1994中国接入"\n\nInternet≠WWW：Internet是路，WWW是车',
'【必考】①Internet前身=ARPANET ②基于TCP/IP协议 ③中国1994年接入 ④Internet提供的主要服务',
1,
'["Internet","因特网","ARPANET","TCP/IP","互联网历史","中国接入"]',
'["Internet基础","Internet概念与发展"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10239, 10143, 'Internet的起源和发展关键时间点？', '1969年ARPANET诞生（4个节点）→1983年TCP/IP正式启用→1991年WWW发布→1994年中国全功能接入。', 1, 'PROCEDURE'),
(10240, 10143, 'Internet的核心特征是什么？', '①基于TCP/IP协议族②全球互联无中心管理③提供WWW/Email/FTP等服务。Internet是网络基础设施，不等于WWW。', 2, 'DEFINITION'),
(10241, 10143, '中国互联网的关键节点是什么？', '1994年4月20日通过中科院高能所64K专线全功能接入Internet。CNNIC（中国互联网络信息中心）管理.cn域名。', 3, 'DEFINITION');

-- ============================================================
-- 文章10144: Internet接入方式（node=1118）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10144, 'Internet接入方式——从56K到千兆',
'## 常见Internet接入方式对比

| 接入方式 | 传输介质 | 典型速率 | 适用场景 |
|:--------:|:--------:|:--------:|:---------|
| **电话拨号** | 电话线+Modem | 56Kbps | ❌ 已淘汰 |
| **ADSL** | 电话线（频分复用） | 2~8Mbps下行 | ❌ 早期家庭宽带 |
| **光纤（FTTH）** | **光纤** | **100Mbps~1Gbps** | ✅ **当前家庭/学校主流** |
| **LAN接入** | 以太网双绞线 | 100Mbps~1Gbps | ✅ 小区/校园宽带 |
| **4G/5G** | 无线蜂窝网络 | 100Mbps~1Gbps | ✅ 手机/移动设备 |
| **Wi-Fi** | 无线 | 可达数百Mbps | ✅ 家庭/公共热点 |

## 各接入方式详解

### 光纤宽带（FTTH）——当前最主流
- **FTTH** = Fiber To The Home（光纤到户）
- **设备**：光猫（ONU）+ 路由器
- **特点**：速度快、稳定、抗干扰
- **速率**：100M/200M/500M/1000Mbps

### ADSL——已被光纤取代
- 利用电话线高频段传输数据，不影响通话
- 下行速率大于上行（非对称）
- 受距离影响大（离局端越远越慢）

### 4G/5G移动网络
| 世代 | 速率 | 时延 |
|:----:|:----:|:----:|
| 4G LTE | 100~150Mbps | 30~50ms |
| **5G** | **1~10Gbps** | **1~10ms** |

### 各接入方式所需设备

| 接入方式 | 所需设备 |
|:--------:|:---------|
| 电话拨号 | 56K Modem（已淘汰） |
| ADSL | ADSL Modem + 电话线 |
| **光纤** | **光猫（ONU）+ 路由器** |
| LAN接入 | 网卡 + 网线 |
| 4G/5G | SIM卡 + 蜂窝模块 |
| Wi-Fi | 无线路由器/AP + 无线网卡 |

> **考试重点**：当前家庭宽带的主流是光纤（FTTH），需要光猫和路由器。ADSL已基本淘汰。',
'Internet接入方式从电话拨号（56Kbps）发展到光纤（100Mbps~1Gbps）。当前主流是FTTH光纤到户，需光猫+路由器。其他方式：ADSL（将淘汰）、4G/5G、Wi-Fi。',
5, '单元4 畅游Internet', '任务1 接入Internet', 1118,
'接入方式发展："56K电话→ADSL→光纤千兆"\n主流=FTTH光纤到户=光猫+路由器\n\nFTTH记法："Fiber To The Home=光纤到家"',
'【必考】①光纤FTTH是当前主流 ②光纤需光猫+路由器 ③ADSL已基本淘汰 ④各接入方式的设备区别',
1,
'["Internet接入","FTTH","光纤","ADSL","4G","5G","宽带接入"]',
'["Internet基础","Internet接入方式"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10242, 10144, '当前主流的Internet接入方式是什么？需要哪些设备？', '光纤FTTH（Fiber To The Home）是当前主流。需要光猫（ONU）将光信号转为电信号，再接路由器供多设备上网。速率可达1Gbps。', 1, 'DEFINITION'),
(10243, 10144, '常见的Internet接入方式有哪些？', '①光纤FTTH（当前主流）②ADSL（已淘汰）③LAN接入（小区/校园）④4G/5G（移动）⑤Wi-Fi（无线）。从56K拨号到千兆光纤。', 2, 'DEFINITION'),
(10244, 10144, 'ADSL和光纤在原理上有什么不同？', 'ADSL：利用电话线高频段传输数据，速率受距离影响大，非对称。光纤：利用光信号在光纤中传输，速率高、抗干扰、不受距离影响。光纤全面替代了ADSL。', 3, 'COMPARISON');

-- ============================================================
-- 文章10145: WWW万维网概念（node=1119）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10145, 'WWW万维网——Internet上最亮的星',
'## 什么是WWW？

WWW（World Wide Web，万维网）是基于**HTTP协议**的**超文本信息系统**，通过超链接将全球信息互联。

### 一句话理解
> 你每天用浏览器打开的网页，就是WWW。WWW是Internet上**最广泛使用的服务**。

## 发明者

| 人物 | 国籍 | 成就 | 时间 |
|:----:|:----:|:------|:----:|
| **Tim Berners-Lee** | 英国 | 发明WWW、提出URL/HTTP/HTML | **1989年提出，1991年发布** |

- 1989年在欧洲核子研究中心（CERN）提出万维网概念
- 1991年8月6日第一个网站在CERN上线
- 他将这项发明无偿公开，未申请专利

> 如果Tim Berners-Lee为WWW申请了专利，今天的互联网可能完全不同。他是互联网的"大慈善家"。

## WWW三要素

| 要素 | 全称 | 作用 |
|:----:|:----:|:------|
| **URL** | 统一资源定位符 | 告诉浏览器"去哪里找"——定位资源 |
| **HTTP** | 超文本传输协议 | 告诉浏览器"怎么传输"——传输协议 |
| **HTML** | 超文本标记语言 | 告诉浏览器"怎么显示"——编写网页 |

### 三要素关系
> **URL**定位资源 → **HTTP**获取资源 → **HTML**展示资源

## 重要概念区分

| 概念 | 关系 |
|:----:|:------|
| **Internet** | 全球计算机网络基础设施 |
| **WWW** | 运行在Internet上的**服务之一** |
| **浏览器** | 访问WWW服务的**客户端软件** |

> Internet ≠ WWW。Internet是高速公路网，WWW是跑在上面的快递服务。Internet还包括Email、FTP、在线游戏等非Web服务。',
'WWW是Internet上最广泛使用的服务，发明者Tim Berners-Lee（1989年）。三要素：URL（定位）、HTTP（传输）、HTML（显示）。Internet≠WWW（基础设施≠其上服务）。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1119,
'WWW三要素："用URL找→用HTTP传→用HTML展示"\n\nTim Berners-Lee记法："1989年提出，1991年上线"\n\nInternet vs WWW："Internet是马路，WWW是路上的车"',
'【必考】①WWW的发明者Tim Berners-Lee（1989年）②三要素：URL+HTTP+HTML ③Internet≠WWW',
2,
'["WWW","万维网","Tim Berners-Lee","HTTP","HTML","URL","超文本"]',
'["Internet基础","WWW"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10245, 10145, 'WWW（万维网）由谁在什么时候发明的？', '英国科学家Tim Berners-Lee。1989年在CERN提出万维网概念，1991年发布了第一个网站。他无偿公开了这项发明。', 1, 'DEFINITION'),
(10246, 10145, 'WWW三要素是什么？各有什么作用？', '①URL（统一资源定位符）—定位资源去哪找②HTTP（超文本传输协议）—规定如何传输③HTML（超文本标记语言）—编写网页内容。', 2, 'DEFINITION'),
(10247, 10145, 'Internet和WWW有什么区别？', 'Internet是全球互联的网络基础设施（高速公路网）。WWW是运行在Internet上的一个服务（跑在路上的快递）。Internet还包括Email、FTP等服务。', 3, 'COMPARISON');

-- ============================================================
-- 文章10146: URL统一资源定位符（node=1120）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10146, 'URL——Internet上的门牌号',
'## 什么是URL？

URL（Uniform Resource Locator，统一资源定位符）在Internet上**唯一定位一个资源**。

### 一句话理解
> URL就像"门牌号+房间号"——告诉浏览器要去哪个服务器、找哪个文件。

## URL格式

```
协议://主机名(域名或IP):端口/路径?查询参数#锚点
```

### 完整格式详解
```
https://www.example.com:443/teacher/index.html?id=123#top
└─┬──┘ └───────┬───────┘ └┬┘ └────────┬─────────┘└─┬─┘└┬┘
  协议          主机名      端口         路径         参数  锚点
```

| 部分 | 含义 | 示例 |
|:----:|:------|:------|
| **协议** | 使用的应用层协议 | `https`、`http`、`ftp` |
| **主机名** | 服务器的域名或IP | `www.example.com` |
| **端口** | 服务器上的端口号（可省略） | `:443`（HTTPS默认） |
| **路径** | 服务器上资源的位置 | `/teacher/index.html` |
| **查询参数** | 传递给服务器的参数 | `?id=123` |
| **锚点** | 网页内的书签跳转 | `#top` |

## URL示例解析

| URL | 解析 |
|:----|:-----|
| `https://www.baidu.com/` | 协议https，域名www.baidu.com，省略路径表示首页 |
| `http://192.168.1.1/login.html` | 协议http，IP地址，路径login.html |
| `ftp://files.example.com/software/` | 协议ftp，目录路径software |
| `https://item.jd.com/10001234.html` | 京东商品页面，路径包含商品ID |

## 常见URL协议

| 协议 | 默认端口 | 用途 |
|:----:|:--------:|:------|
| `http://` | 80 | 普通网页访问 |
| `https://` | 443 | 安全网页访问 |
| `ftp://` | 21 | 文件传输访问 |
| `mailto:` | — | 邮件链接（打开邮件客户端） |

> **考试重点**：URL格式为`协议://域名/路径`。各部分含义要能对应识别。',
'URL在Internet上唯一定位资源。格式：协议://主机名:端口/路径?参数#锚点。常见协议：http（80）、https（443）、ftp（21）。路径表示资源在服务器上的位置。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1120,
'URL记法："协议://域名/路径"\n\n就像快递地址：协议=快递方式、主机名=城市、路径=小区门牌',
'【必考】①URL格式：协议://域名/路径 ②各部分的含义识别 ③常见协议前缀（http/https/ftp）',
2,
'["URL","统一资源定位符","HTTP","HTTPS","域名","路径"]',
'["Internet基础","URL"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10248, 10146, '什么是URL？它的基本格式是什么？', 'URL（统一资源定位符）在Internet上唯一定位资源。基本格式：协议://主机名/路径。完整格式：协议://主机名:端口/路径?参数#锚点。', 1, 'DEFINITION'),
(10249, 10146, 'URL由哪些部分组成？各部分的含义？', '①协议（http/https/ftp）②主机名（域名或IP）③端口（默认可省略）④路径（服务器上的位置）⑤查询参数（?key=value）⑥锚点（#位置）。', 2, 'DEFINITION'),
(10250, 10146, 'URL中http://、https://、ftp://的区别？', 'http://（端口80，明文）、https://（端口443，加密）、ftp://（端口21，文件传输）。前缀告诉浏览器使用什么协议来访问资源。', 3, 'COMPARISON');

-- ============================================================
-- 文章10147: 浏览器的基本使用（node=1121）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10147, 'Web浏览器——通往WWW的大门',
'## 什么是浏览器？

浏览器是访问WWW服务的**客户端软件**，负责发送HTTP请求并渲染展示网页内容。

### 一句话理解
> 浏览器就像"一扇窗户"——你通过它看到Internet上万维网世界的一切。

## 常见浏览器

| 浏览器 | 内核 | 特点 |
|:------:|:----:|:------|
| **Microsoft Edge** | Chromium（Blink） | Windows 11默认，与Chrome兼容 |
| **Google Chrome** | Blink | 全球使用最广 |
| **Mozilla Firefox** | Gecko | 开源注重隐私 |
| **Apple Safari** | WebKit | macOS/iOS默认 |
| 360浏览器 | Trident+Blink | 国产双核 |

## 浏览器基本操作

| 操作 | 说明 | 快捷键 |
|:----:|:------|:------:|
| **地址栏输入URL** | 直接访问网站 | Alt+D |
| **收藏夹/书签** | 保存常用网站 | Ctrl+D |
| **历史记录** | 查看访问过的网页 | Ctrl+H |
| **下载管理** | 管理下载的文件 | Ctrl+J |
| **刷新页面** | 重新加载当前页面 | **F5** |
| **前进/后退** | 切换浏览历史 | Alt+← / Alt+→ |
| **新标签页** | 打开新标签 | Ctrl+T |

## 浏览器常见设置

| 设置项 | 说明 |
|:------:|:------|
| **主页设置** | 启动时自动打开的页面 |
| **清除浏览数据** | 清除历史记录、缓存、Cookie（Ctrl+Shift+Del） |
| **安全级别** | 高/中/低（影响脚本和ActiveX运行） |
| **弹窗拦截** | 阻止自动弹出的广告窗口 |
| **Cookie管理** | 允许或禁止网站保存登录状态 |

### 隐私保护
> 定期清除浏览历史、缓存和Cookie可以保护个人隐私。**无痕模式/隐私模式**下浏览器不保存历史记录。

> **考试重点**：浏览器是访问WWW的客户端软件。F5刷新、Ctrl+D收藏、Ctrl+H历史、Ctrl+J下载。常用浏览器：Edge、Chrome、Firefox、Safari。',
'浏览器是访问WWW服务的客户端软件。常见浏览器：Edge、Chrome、Firefox、Safari。操作：地址栏输入URL、F5刷新、Ctrl+D收藏、Ctrl+H历史、Ctrl+J下载。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1121,
'快捷键记法："F5=刷新，Ctrl+D=收藏，Ctrl+H=历史，Ctrl+J=下载，Ctrl+T=新标签"\n清除数据："Ctrl+Shift+Del=清除浏览数据"',
'【必考】①浏览器的概念（访问WWW的客户端软件）②常见浏览器名称 ③基本快捷键（F5刷新/Ctrl+D收藏/Ctrl+H历史）',
1,
'["浏览器","Web浏览器","Edge","Chrome","Firefox","Safari","网页浏览"]',
'["Internet基础","浏览器"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10251, 10147, '常见的Web浏览器有哪些？', '①Microsoft Edge（Windows默认）②Google Chrome（全球最广）③Mozilla Firefox（开源隐私）④Apple Safari（苹果默认）。', 1, 'DEFINITION'),
(10252, 10147, '浏览器的常用快捷键有哪些？', 'F5刷新、Ctrl+D收藏、Ctrl+H历史记录、Ctrl+J下载管理、Ctrl+T新标签页、Alt+D定位地址栏、Ctrl+Shift+Del清除浏览数据。', 2, 'APPLICATION'),
(10253, 10147, '浏览器的主要功能是什么？', '作为客户端软件向Web服务器发送HTTP请求，接收服务器返回的HTML/CSS/JavaScript代码，解析渲染为可视化的网页内容。', 3, 'DEFINITION');

-- ============================================================
-- 文章10148: 搜索引擎的使用（node=1122）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10148, '搜索引擎——互联网的信息导航',
'## 什么是搜索引擎？

搜索引擎是帮助用户在Internet上查找信息的**信息检索系统**。

### 一句话理解
> 搜索引擎像"图书馆的图书检索系统"——你输入想找的内容，它告诉你哪些网页包含这些内容。

## 常见搜索引擎

| 搜索引擎 | 网址 | 特点 |
|:--------:|:----:|:------|
| **百度** | www.baidu.com | 中文搜索最常用，国内市场第一 |
| **Google** | www.google.com | 全球最大搜索引擎 |
| **必应** | www.bing.com | 微软出品，国外搜索结果好 |
| 搜狗 | www.sogou.com | 国产，接入微信搜索 |

## 基本搜索技巧

| 技巧 | 语法 | 示例 | 作用 |
|:----:|:----:|:------|:----:|
| **精确匹配** | 加英文双引号 | `"计算机网络定义"` | 搜索结果必须包含完整短语 |
| **排除词** | 减号 | `计算机病毒 -木马` | 排除包含"木马"的结果 |
| **站内搜索** | **site:** | **`site:edu.cn 招生`** | 只在.edu.cn网站内搜索 |
| **文件类型** | **filetype:** | **`计算机基础 filetype:pdf`** | 只搜索PDF文件 |
| 通配符 | 星号`*` | `计算机*技术` | 匹配任意词 |

> **注意**：搜索语法中的冒号、引号、减号都是**英文半角**符号。

## 搜索引擎工作原理

```
①爬虫（Spider/Crawler）→按链接抓取网页内容
        ↓
②索引（Index）→提取关键词建立索引数据库
        ↓
③排序（Rank）→根据相关性（百度：超链分析；Google：PageRank）返回结果
        ↓
④用户输入关键词→在索引中匹配→展示排序结果
```

## 高级搜索

| 百度高级搜索 | 说明 |
|:------------:|:------|
| `intitle:关键词` | 搜索标题中包含关键词的网页 |
| `inurl:关键词` | 搜索URL中包含关键词的网页 |
| `date:2024` | 限定时间范围 |

### 搜索建议
> 使用**多个关键词**比单个关键词更精准。如搜"四川对口升学计算机考试大纲"比只搜"考试大纲"效果好得多。

> **考试重点**：搜索引擎的使用（关键词搜索、高级搜索语法）。filetype:搜索文件类型、site:限定网站。百度是国内最常用搜索引擎。',
'搜索引擎帮用户在Internet上查找信息。常见：百度（中文最常用）、Google（全球最大）、必应。搜索技巧：""精确匹配、-排除词、site:站内搜、filetype:文件类型。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1122,
'搜索技巧口诀："双引号精准找，减号排除掉，site站内搜，filetype看文件类型"\n工作原理："爬虫抓→建索引→排顺序→返回结果"',
'【必考】①百度是国内最常用搜索引擎 ②filetype:按文件类型搜索 ③site:限定网站搜索 ④关键词搜索用多个关键词更精准',
1,
'["搜索引擎","百度","Google","搜索技巧","filetype","site","信息检索"]',
'["Internet基础","搜索引擎"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10254, 10148, '常用的搜索引擎有哪些？', '百度（www.baidu.com）—中文最常用、Google（www.google.com）—全球最大、必应（www.bing.com）—微软出品。', 1, 'DEFINITION'),
(10255, 10148, '搜索引擎的常用搜索语法有哪些？', '①"关键词"—精确匹配②site:域名—站内搜索③filetype:格式—按文件类型搜索④减号—排除关键词。语法符号必须用英文半角。', 2, 'APPLICATION'),
(10256, 10148, '搜索引擎的三步工作原理是什么？', '①爬虫（Spider）按链接抓取网页②建立索引数据库（提取关键词）③按相关性算法排序返回结果。百度用超链分析，Google用PageRank。', 3, 'PROCEDURE');

-- ============================================================
-- 文章10149: FTP文件传输协议（node=1123）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10149, 'FTP文件传输——在Internet上搬文件',
'## FTP在Internet中的作用

FTP（File Transfer Protocol）用于在Internet上将文件从一台计算机传输到另一台计算机——**上传**和**下载**。

### 一句话理解
> 你从某个网站下载软件安装包，或者把自己的文件上传到云服务器，背后就是FTP在工作。

## FTP回顾

| 知识点 | 说明 |
|:------:|:------|
| **端口** | 控制连接 **21**，数据连接 **20** |
| **传输层协议** | TCP（保证文件完整） |
| **功能** | 上传（本地→服务器）、下载（服务器→本地） |

## FTP传输模式

| 模式 | 适用于 | 说明 |
|:----:|:------:|:------|
| **ASCII模式** | 纯文本文件（.txt/.html/.css） | 自动转换不同系统的换行符 |
| **二进制模式** | 非文本文件（图片/程序/压缩包） | 原样传输，不做转换 |

> **错误提示**：如果下载的图片打不开或程序无法运行，通常是FTP传输模式选错了——应该用二进制模式。

## FTP的三种访问方式

| 方式 | 优点 | 缺点 |
|:----:|:------|:------|
| **命令行** | Windows自带`ftp`命令，无需安装 | 操作不便 |
| **浏览器** | 方便快捷，直接输入`ftp://...` | 功能有限 |
| **FTP客户端软件** | 图形界面、支持拖拽、断点续传 | 需安装 |

### 常用FTP客户端

| 软件 | 平台 | 特点 |
|:----:|:----:|:------|
| **FileZilla** | Windows/Mac/Linux | 免费开源，最常用 |
| CuteFTP | Windows | 老牌商业软件 |
| FlashFXP | Windows | 支持多线程 |

> **考试重点**：FTP功能=文件上传/下载，端口21。FileZilla是常用的FTP客户端。',
'FTP在Internet上实现文件上传和下载。端口21（控制）、20（数据）。两种传输模式：ASCII（文本）和二进制（非文本）。三种访问方式：命令行、浏览器、FTP客户端（FileZilla最常用）。',
5, '单元4 畅游Internet', '任务3 应用FTP服务', 1123,
'FTP两种模式："文本用ASCII，图片程序用二进制"\n访问方式："命令/浏览器/客户端"\nFileZilla="文件小动物🔵🟡"—最常用FTP客户端',
'【必考】①FTP功能=文件上传下载 ②端口21 ③两种传输模式（ASCII/二进制）的区别 ④FileZilla是常用FTP客户端',
1,
'["FTP","文件传输","上传","下载","FileZilla","ASCII","二进制"]',
'["Internet基础","FTP"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10257, 10149, 'FTP在Internet上的作用是什么？', '在Internet上实现文件的上传（本地→服务器）和下载（服务器→本地）。默认端口21（控制连接），基于TCP传输保证文件完整。', 1, 'DEFINITION'),
(10258, 10149, 'FTP的ASCII模式和二进制模式有什么区别？', 'ASCII模式适用于文本文件，自动转换换行符。二进制模式适用于非文本文件（图片/程序/压缩包），原样传输不转换。图片下载后打不开通常是模式选错了。', 2, 'COMPARISON'),
(10259, 10149, '有哪些方式可以访问FTP服务器？', '①命令行：Windows自带ftp命令②浏览器：地址栏输入ftp://服务器地址③FTP客户端：FileZilla等图形界面工具，支持拖拽和断点续传。', 3, 'APPLICATION');

-- ============================================================
-- 文章10150: 电子邮件系统（node=1124）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10150, '电子邮件——Internet上最古老的服务',
'## 电子邮件基础

### 邮箱地址格式
```
用户名@域名
示例：admin@school.edu.cn
      └─┬─┘└─────┬──────┘
      用户账号    邮件服务器域名
```
- **@** 读作"at"（在），表示"在...上"
- `admin@school.edu.cn` = "admin在学校邮件服务器上的邮箱"

### 电子邮件系统组成

| 组件 | 英文 | 作用 | 生活类比 |
|:----:|:----:|:------|:--------:|
| **邮件用户代理** | **MUA** | 用户使用的邮件客户端 | 你写信/收信的工具 |
| **邮件传输代理** | **MTA** | 邮件服务器，负责转发 | 邮局的邮递员/分拣机 |
| **邮件投递代理** | **MDA** | 将邮件投递到用户邮箱 | 送到你家信箱的快递员 |

## 邮件收发流程

```
发件人(Outlook) ──SMTP(25)──→ 发件服务器(MTA)
                                    │
                              SMTP(25) 互联网转发
                                    ↓
收件人(Web邮箱) ←──POP3(110)─── 收件服务器(MTA)
```

### 完整过程示例
> 张三用163邮箱发邮件给李四的QQ邮箱：
> 1. 张三在163网页写邮件点击发送
> 2. 发送到163邮件服务器（MTA）
> 3. 163服务器通过SMTP将邮件转发到QQ邮件服务器
> 4. 李四打开QQ邮箱查看（通过POP3/IMAP从服务器取邮件）

## Web邮箱 vs 客户端

| 方式 | 配置 | 使用场景 |
|:----:|:----:|:---------|
| **Web邮箱** | 无需配置，浏览器访问网页即可 | 临时/移动使用 |
| **邮件客户端**（Outlook/Foxmail） | 需配置SMTP和POP3服务器 | 日常办公/批量管理 |

### 客户端配置要点
> 使用邮件客户端时需填写的参数：
> - **SMTP服务器**：发件服务器地址（如 smtp.163.com）
> - **POP3服务器**：收件服务器地址（如 pop.163.com）
> - 对应的端口：SMTP=25，POP3=110

> **考试重点**：邮箱格式`用户名@域名`。SMTP=发邮件（25），POP3=收邮件（110）。Web邮箱无需配置，客户端需配置SMTP/POP3。',
'邮箱地址格式：用户名@域名。邮件系统由MUA（客户端）、MTA（传输代理）、MDA（投递代理）组成。SMTP（端口25）发邮件，POP3（端口110）收邮件。Web邮箱无需配置。',
5, '单元4 畅游Internet', '任务4 应用Email服务', 1124,
'@记法："@=at=在……上面"\n\nSMTP发=PUSH（推），POP3收=PULL（拉）\n\n"发邮件找SMTP（25），收邮件找POP3（110）"',
'【必考】①邮箱格式：用户名@域名 ②SMTP发邮件（25）③POP3收邮件（110）④Web邮箱vs客户端的区别',
2,
'["电子邮件","Email","SMTP","POP3","MUA","MTA","MDA","邮箱"]',
'["Internet基础","电子邮件"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10260, 10150, '电子邮件地址的格式是什么？', '用户名@域名。如admin@school.edu.cn。@读作"at"，表示"在……上"。', 1, 'DEFINITION'),
(10261, 10150, '电子邮件系统由哪些组件组成？', '①MUA（Mail User Agent）—用户使用的邮件客户端②MTA（Mail Transfer Agent）—服务器间转发邮件③MDA（Mail Delivery Agent）—投递邮件到用户邮箱。', 2, 'DEFINITION'),
(10262, 10150, 'Web邮箱和邮件客户端有什么区别？', 'Web邮箱：浏览器直接访问，无需配置，方便临时使用。邮件客户端（Outlook/Foxmail）：需配置SMTP和POP3服务器，适合日常办公批量管理邮件。', 3, 'COMPARISON');

-- ============================================================
-- 文章10151: 远程登录（node=1125）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10151, '远程登录——远程管理计算机',
'## 什么是远程登录？

远程登录是指通过网络从本地计算机登录到**远程计算机**并执行操作的技术。

### 一句话理解
> 你在家里通过电脑连接到学校机房的服务器，像坐在机房面前一样操作它——这就是远程登录。

## Telnet

| 属性 | 说明 |
|:----:|:------|
| **端口** | **23** |
| **传输方式** | **明文传输**（包括用户名和密码） |
| **安全性** | 低——数据易被窃听 |
| **状态** | ✅ 已基本被SSH取代 |

### Telnet的使用
```cmd
telnet 192.168.1.100       # 远程登录到192.168.1.100
telnet www.example.com 80  # 测试目标主机的80端口是否开放
```

> 在Windows中Telnet客户端默认未安装，需在"启用或关闭Windows功能"中开启。

## SSH（安全外壳协议）

| 属性 | 说明 |
|:----:|:------|
| **端口** | **22** |
| **传输方式** | **加密传输** |
| **安全性** | 高——数据加密防止窃听 |
| **状态** | ✅ 当前远程管理的**主流标准** |

### SSH的典型应用
```cmd
ssh root@192.168.1.100     # SSH登录到Linux服务器
```

### SSH的优势
> ①加密传输——密码和命令不泄露 ②身份验证——防止中间人攻击 ③支持文件传输（SFTP）和端口转发

## Telnet vs SSH

| 对比维度 | Telnet | SSH |
|:--------:|:------:|:---:|
| **端口** | **23** | **22** |
| **加密** | ❌ 无（明文） | ✅ 加密 |
| **安全性** | 低 | 高 |
| **速度** | 略快（无加密开销） | 略慢（加密解密） |
| **适用场景** | 简单测试/老旧设备 | 服务器远程管理（主流） |
| **状态** | 基本淘汰 | 当前标准 |

> **对口升学考点**：Telnet=明文传输（端口23），SSH=加密传输（端口22）。SSH更安全，是目前远程登录的主流方式。',
'远程登录通过网络远程操作另一台计算机。Telnet（端口23，明文传输，不安全，已淘汰）vs SSH（端口22，加密传输，安全，当前主流）。Linux服务器远程管理主要使用SSH。',
5, '单元4 畅游Internet', '任务5 应用远程登录服务', 1125,
'Telnet vs SSH："Telnet=23/明文/淘汰，SSH=22/加密/主流"\n\n"SSH比Telnet多了一个S=Secure（安全）=加密"',
'【必考】①远程登录的概念 ②Telnet端口23、明文传输 ③SSH端口22、加密传输 ④SSH比Telnet更安全，是当前主流',
1,
'["远程登录","Telnet","SSH","23端口","22端口","加密传输","远程管理"]',
'["Internet基础","远程登录"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10263, 10151, '什么是远程登录？Telnet和SSH分别使用什么端口？', '远程登录通过网络操作远程计算机。Telnet端口23（明文传输）。SSH端口22（加密传输）。', 1, 'DEFINITION'),
(10264, 10151, 'Telnet和SSH有什么区别？', 'Telnet：端口23、明文传输（含密码）、不安全、已淘汰。SSH：端口22、加密传输、安全、当前远程管理主流标准。SSH=Telnet+S（Secure）。', 2, 'COMPARISON'),
(10265, 10151, '为什么SSH取代了Telnet？', 'Telnet所有数据（包括密码）都是明文传输，在网络上可以被轻松窃听。SSH对数据进行加密，即使被截获也无法破解。安全性是SSH取代Telnet的根本原因。', 3, 'SCENARIO');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v173] 单元4 知识库文章种子完成：共9篇文章，约27张记忆卡片。' AS result;
-- ============================================================================
-- v174: 网络应用基础 — 知识库文章种子（单元5 运用网络安全技术）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖3个任务7个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元5：运用网络安全技术
-- ═══════════════════════════════════════════════════════════════
-- 任务1 认识加密和认证技术（节点1126~1128）
-- 任务2 防治计算机病毒（节点1129~1130）
-- 任务3 使用防火墙（节点1131~1132）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10152: 网络安全威胁（node=1126）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10152, '网络安全威胁——网络世界的五大公敌',
'## 常见网络安全威胁

### 计算机病毒（Virus）
- **寄生在正常程序中**，执行时自我复制并感染其他文件
- **需要宿主程序**（不可独立运行）
- **五大特征**：传染性（最基本）、破坏性、隐蔽性、潜伏性、可触发性

> 类比：病毒就像"生物病毒"——寄生在宿主细胞中，自我复制，感染其他细胞。

### 木马（Trojan Horse）
- **伪装成正常软件**，诱导用户下载安装
- **窃取密码、远程控制、文件盗取**
- 与病毒的区别：木马**不自我复制**，依赖用户主动安装

> 类比：木马就像"特洛伊木马"——外表是礼物（正常软件），里面藏着士兵（恶意代码）。

### DDoS攻击（分布式拒绝服务）
- 利用大量受控计算机（僵尸网络）同时向目标发起请求
- 耗尽目标带宽或资源，导致正常用户无法访问
- 常见攻击对象：网站、游戏服务器

### SQL注入（SQL Injection）
- 在用户输入中混入SQL代码，欺骗**数据库**执行恶意操作
- 防范：**参数化查询**、输入过滤
- 示例：登录框输入`'' OR 1=1 --`绕过密码验证

### XSS（跨站脚本攻击）
- 在网页中注入恶意**JavaScript脚本**
- 其他用户浏览页面时脚本在浏览器中执行
- 防范：输入输出过滤、CSP策略

## 五大威胁对比速查

| 威胁 | 攻击目标 | 核心机制 | 主要防范 |
|:----:|:--------:|:--------:|:---------|
| **病毒** | 文件/系统 | 自我复制+寄生 | 杀毒软件 |
| **木马** | 账号/隐私 | 伪装+窃取 | 不下载不明软件 |
| **DDoS** | 服务器 | 海量请求耗尽资源 | 流量清洗+防火墙 |
| **SQL注入** | 数据库 | 拼接恶意SQL | 参数化查询 |
| **XSS** | 其他用户 | 注入恶意脚本 | 输入过滤 |

> **考试重点**：区分病毒（自我复制需要宿主）和木马（伪装不复制）。SQL注入利用输入漏洞。DDoS是资源耗尽型攻击。',
'五大网络安全威胁：病毒（寄生+自我复制）、木马（伪装+窃取）、DDoS（海量请求耗尽资源）、SQL注入（恶意SQL操纵数据库）、XSS（注入恶意脚本）。',
5, '单元5 运用网络安全技术', '任务1 认识加密和认证技术', 1126,
'病毒vs木马："病毒=能下崽（自我复制），木马=不能下崽（靠你装）"\n\n五类威胁："病毒木马搞文件，DDoS搞服务器，SQL注入搞数据库，XSS搞浏览器"',
'【必考】①病毒=寄生+自我复制 ②木马=伪装+不复制 ③DDoS=海量请求 ④SQL注入=操纵数据库 ⑤XSS=注入脚本',
2,
'["网络安全","病毒","木马","DDoS","SQL注入","XSS","安全威胁"]',
'["网络安全","网络安全威胁"]',
'[
  {"type":"choice","question":"计算机病毒和木马最主要的区别是什么？","options":["病毒会自我复制，木马不会","木马会自我复制，病毒不会","两者没有区别","病毒是硬件问题，木马是软件问题"],"answer":"A","explanation":"病毒能自我复制并感染其他文件（有传染性），木马不自我复制，依赖用户主动安装。"},
  {"type":"choice","question":"SQL注入攻击利用了什么漏洞？","options":["系统未打补丁","用户输入未过滤，拼接了恶意SQL代码","Wi-Fi密码泄露","DNS解析错误"],"answer":"B","explanation":"SQL注入在用户输入中混入SQL代码，程序未做过滤直接拼接到SQL语句中，导致数据库执行了恶意操作。"},
  {"type":"judge","question":"DDoS攻击通过大量请求耗尽目标系统资源，导致正常用户无法访问。","answer":"T","explanation":"DDoS（分布式拒绝服务）利用僵尸网络向目标发送海量请求，耗尽带宽或服务器资源，导致瘫痪。"},
  {"type":"multi","question":"以下哪些属于常见的网络安全威胁？（多选）","options":["计算机病毒","木马","DDoS攻击","SQL注入","显示器故障"],"answer":"A,B,C,D","explanation":"病毒、木马、DDoS、SQL注入都是常见网络安全威胁。显示器故障属于硬件问题，不属于网络安全威胁范畴。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10266, 10152, '计算机病毒和木马有什么区别？', '病毒：寄生在正常程序中，能自我复制并感染其他文件（能"下崽"）。木马：伪装成正常软件，不自我复制，靠用户主动安装。病毒=传染，木马=伪装。', 1, 'COMPARISON'),
(10267, 10152, '什么是SQL注入和XSS？', 'SQL注入：在输入框中填入恶意SQL代码，操纵数据库执行非法操作。XSS：在网页中注入恶意脚本，其他用户浏览时执行。两者都是输入过滤不严导致的。', 2, 'DEFINITION'),
(10268, 10152, 'DDoS攻击的原理是什么？', '攻击者控制大量"肉鸡"（受感染的计算机）形成僵尸网络，同时向目标服务器发送海量请求，耗尽目标的带宽或资源，使正常用户无法访问。', 3, 'DEFINITION');

-- ============================================================
-- 文章10153: 对称加密与非对称加密（node=1127）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10153, '加密技术——保护数据的铠甲',
'## 对称加密

| 特征 | 说明 |
|:----:|:------|
| **密钥** | 加密和解密使用**同一把密钥** |
| **优点** | 速度快，适合大量数据加密 |
| **缺点** | 密钥分发困难（如何安全地把钥匙交给对方？） |
| **常见算法** | **DES**（56位，已不安全）、**AES**（128/256位，当前主流） |

### 一句话理解
> 对称加密就像"一把钥匙开一把锁"——你和我用同一把钥匙加密/解密。问题是这把钥匙怎么安全地交给对方？

## 非对称加密

| 特征 | 说明 |
|:----:|:------|
| **密钥** | 一对密钥——**公钥**（公开）+ **私钥**（保密） |
| **加密/解密** | 公钥加密→私钥解密（反之亦可） |
| **优点** | 解决密钥分发问题，安全性高 |
| **缺点** | 速度慢（比对称加密慢约1000倍） |
| **常见算法** | **RSA**、ECC（椭圆曲线） |

### 公钥和私钥
> **公钥就像公开的锁**——任何人都可以用它锁上箱子（加密），但只有你手里的私钥能打开。
> **私钥就像你随身带的钥匙**——绝不能给别人。

## 两种加密的对比

| 对比维度 | 对称加密 | 非对称加密 |
|:--------:|:--------:|:----------:|
| 密钥数量 | **1把** | **2把**（公钥+私钥） |
| 加密速度 | **快** | 慢（约慢1000倍） |
| 密钥分发 | 困难（需要安全通道） | 简单（公钥可公开） |
| 安全性 | 较低 | **高** |
| 典型算法 | DES/AES | RSA/ECC |

## 实际应用——HTTPS的混合加密

HTTPS = **非对称加密**（握手协商密钥） + **对称加密**（数据传输）

```
①浏览器→服务器：用服务器的公钥加密一个临时密钥（非对称）
②服务器：用自己的私钥解密得到临时密钥
③之后数据传输：用这个临时密钥进行对称加密（速度快）
```

> **利用非对称加密安全传递对称密钥，再用对称加密高效传输数据**——兼顾安全性和速度。

> **考试重点**：对称加密=一把密钥（速度快，密钥分发困难）。非对称加密=公钥+私钥（安全性高，速度慢）。HTTPS用混合加密。',
'对称加密（1把密钥，DES/AES，速度快）和非对称加密（公钥+私钥，RSA，安全性高）。HTTPS=非对称加密协商密钥+对称加密传输数据。',
5, '单元5 运用网络安全技术', '任务1 认识加密和认证技术', 1127,
'对称vs非对称："对称=一把钥匙开锁（快但给钥匙难），非对称=公钥锁私钥开（安全但慢）"\n\nHTTPS="非对称传钥匙→对称传数据"',
'【必考】①对称加密：1把密钥、DES/AES、速度快 ②非对称加密：公钥+私钥、RSA、安全性高 ③HTTPS=非对称+对称混合加密',
2,
'["对称加密","非对称加密","DES","AES","RSA","公钥","私钥","密钥"]',
'["网络安全","加密技术"]',
'[
  {"type":"choice","question":"对称加密使用的密钥数量是多少？","options":["0把（无需密钥）","1把","2把（公钥+私钥）","3把"],"answer":"B","explanation":"对称加密只用1把密钥，加密和解密使用同一把。非对称加密才使用公钥和私钥2把密钥。"},
  {"type":"choice","question":"以下哪个是非对称加密算法的例子？","options":["DES","AES-256","RSA","MD5"],"answer":"C","explanation":"RSA是非对称加密算法，使用公钥+私钥对。DES和AES是对称加密算法。MD5是哈希算法，不是加密算法。"},
  {"type":"judge","question":"非对称加密的速度比对称加密快，适合大量数据加密。","answer":"F","explanation":"恰恰相反——非对称加密速度比对称加密慢约1000倍。HTTPS的优化做法是用非对称加密协商临时密钥，再用对称加密传输数据。"},
  {"type":"multi","question":"以下关于对称加密和非对称加密的描述，正确的有哪些？（多选）","options":["对称加密使用1把密钥","非对称加密使用公钥+私钥","AES是常见对称加密算法","RSA是常见对称加密算法","非对称加密解决了密钥分发问题"],"answer":"A,B,C,E","explanation":"对称加密1把密钥（AES/DES），非对称加密公钥+私钥（RSA）。非对称加密解决密钥分发问题。D错误，RSA是非对称加密。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10269, 10153, '对称加密和非对称加密有什么区别？', '对称：1把密钥、速度快、密钥分发困难、算法DES/AES。非对称：2把（公钥+私钥）、速度慢、解决分发问题、算法RSA。', 1, 'COMPARISON'),
(10270, 10153, 'HTTPS是如何混合使用两种加密的？', '①用非对称加密（RSA）安全传递临时对称密钥②用对称加密（AES）加密实际传输数据。非对称负责安全传递密钥，对称负责高效传输数据。', 2, 'PROCEDURE'),
(10271, 10153, '非对称加密中的公钥和私钥各有什么用途？', '公钥：可以公开给任何人，用于加密数据或验证签名。私钥：必须保密，用于解密数据或生成签名。公钥加密的数据只有对应私钥能解密。', 3, 'DEFINITION');

-- ============================================================
-- 文章10154: 数字证书与数字签名（node=1128）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10154, '数字证书与数字签名——网络世界的身份证和印章',
'## 数字证书

数字证书证明**公钥的所有者身份**——就像网络世界的身份证。

| 属性 | 说明 |
|:----:|:------|
| **作用** | 证明"这个公钥确实属于这个网站" |
| **颁发者** | **CA**（Certificate Authority，数字证书认证中心） |
| **内容** | 持有者信息、公钥、CA的数字签名、有效期 |
| **验证** | 浏览器内置可信任的CA根证书列表 |

### 一句话理解
> 数字证书就像"身份证"——证明持有公钥的人确实是ta声称的身份。浏览器看到HTTPS网站的证书，就知道这个网站是可信的。

### CA证书信任链
```
根CA（浏览器内置信任）
  └── 中级CA
        └── 网站证书（你访问的网站）
```

> 浏览器地址栏的🔒图标，表示该网站有有效的数字证书。

## 数字签名

数字签名验证消息的**完整性**和发送者的**身份**——就像纸质文件的"亲笔签名+印章"。

### 数字签名过程
```
发送方：
  ①对消息计算哈希值（摘要）
  ②用私钥加密哈希值→数字签名
  ③将消息+签名一起发送给接收方

接收方：
  ④用发送方公钥解密签名→得到哈希值
  ⑤对收到的消息计算哈希值
  ⑥比较两个哈希值→一致则签名有效
```

### 数字签名的三重保证

| 保证 | 原理 | 类比 |
|:----:|:------|:----:|
| **身份认证** | 私钥只有发送方持有 | 亲笔签名 |
| **数据完整性** | 哈希值对比——篡改即不匹配 | 骑缝章 |
| **不可否认性** | 发送方无法否认发送过 | 签名不可抵赖 |

### 数字证书 vs 数字签名

| 对比 | 数字证书 | 数字签名 |
|:----:|:--------:|:---------:|
| 作用 | 证明身份（你是谁） | 证明数据未被篡改（数据是真的） |
| 类比 | **身份证** | **亲笔签名+印章** |
| 核心机制 | CA颁发+公钥 | 私钥签名+公钥验证 |

> **考试重点**：数字证书=CA颁发的身份证明（证明"我是谁"）。数字签名=私钥签名+公钥验证（证明"我没被改过"）。',
'数字证书由CA颁发，证明公钥所有者身份（网络身份证）。数字签名用私钥签名+公钥验证，保证数据完整性和不可否认性。HTTPS浏览器🔒标志表示有有效证书。',
5, '单元5 运用网络安全技术', '任务1 认识加密和认证技术', 1128,
'数字证书=身份证（证明"你是谁"）\n数字签名=亲笔签名+骑缝章（证明"没被改过"）\n\nCA=Certificate Authority=发证机关',
'【必考】①数字证书由CA颁发 ②数字证书证明身份 ③数字签名保证完整性和不可否认性 ④CA证书信任链',
2,
'["数字证书","数字签名","CA","公钥","私钥","HTTPS","SSL证书"]',
'["网络安全","加密技术"]',
'[
  {"type":"choice","question":"数字证书由谁颁发？","options":["浏览器厂商","CA（数字证书认证中心）","网站所有者自己","操作系统"],"answer":"B","explanation":"CA（Certificate Authority）是受信任的第三方机构，负责颁发和验证数字证书。浏览器内置了可信CA列表。"},
  {"type":"choice","question":"数字签名可以实现以下哪项功能？","options":["加快网络速度","保证数据完整性和发送者身份认证","加密所有数据","分配IP地址"],"answer":"B","explanation":"数字签名通过私钥签名+公钥验证，保证数据未被篡改（完整性）且确实由声称的发送方发出（身份认证）。"},
  {"type":"judge","question":"浏览器地址栏的🔒图标表示该网站持有有效的数字证书。","answer":"T","explanation":"🔒图标表示网站使用了HTTPS，且数字证书有效（由受信任的CA颁发，未过期，域名匹配）。"},
  {"type":"multi","question":"数字签名可以提供哪些保证？（多选）","options":["身份认证","数据完整性","不可否认性","数据加密","提高传输速度"],"answer":"A,B,C","explanation":"数字签名三重保证：身份认证（私钥唯一）、数据完整性（哈希对比）、不可否认性（无法抵赖）。它不加密数据，也不提高速度。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10272, 10154, '什么是数字证书？由谁颁发？', '数字证书由CA（Certificate Authority）颁发，证明公钥所有者的身份。内容：持有者信息、公钥、CA签名、有效期。浏览器🔒标志=证书有效。', 1, 'DEFINITION'),
(10273, 10154, '数字签名是如何工作的？', '发送方：对消息算哈希→用私钥加密哈希→得到签名。接收方：用公钥解密签名得到哈希→对比自己算的消息哈希→一致则签名有效。', 2, 'PROCEDURE'),
(10274, 10154, '数字证书和数字签名有什么区别？', '数字证书=证明身份（"我是谁"），由CA颁发，类比身份证。数字签名=证明数据没被改（"这个是我发的、没被改过"），类比亲笔签名。', 3, 'COMPARISON');

-- ============================================================
-- 文章10155: 计算机病毒的概念与特征（node=1129）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10155, '计算机病毒——数字世界的传染病',
'## 什么是计算机病毒？

计算机病毒是人为编制的、具有**自我复制能力**的、对计算机系统造成破坏的**恶意程序**。

### 一句话理解
> 计算机病毒就像"生物病毒"——寄生在正常程序（宿主）中，自我复制传播，感染其他程序，有的潜伏一段时间后发作。

## 病毒的五大特征

| 特征 | 说明 | 类比 |
|:----:|:------|:----:|
| **传染性** | 能自我复制并感染其他文件/系统——**最基本特征** | 流感病毒传染给别人 |
| **破坏性** | 删除文件、占用资源、篡改数据 | 病毒让人发烧咳嗽 |
| **隐蔽性** | 隐藏自身存在，不易被用户察觉 | 潜伏期没有症状 |
| **潜伏性** | 感染后不立即发作，等待特定条件 | 病毒在体内潜伏 |
| **可触发性** | 满足特定条件时激活（如特定日期） | 遇到过敏原才发作 |

> **传染性是最基本特征**——没有传染性就不能叫病毒。

## 病毒分类

| 类型 | 寄生对象 | 示例 |
|:----:|:---------|:------|
| **文件型病毒** | .exe/.com/.dll可执行文件 | CIH病毒（1998年，破坏BIOS） |
| **引导型病毒** | 磁盘引导扇区（MBR） | Michelangelo（3月6日发作） |
| **宏病毒** | Office文档的宏代码 | Melissa（1999年，邮件传播） |
| **蠕虫** | 独立程序，通过网络自我传播 | SQL Slammer（极快传播） |
| **勒索病毒** | 加密用户文件，勒索赎金 | WannaCry（2017年，全球爆发） |

### 蠕虫 vs 病毒
> 蠕虫是病毒的一种特殊类型——**不需要宿主文件**，独立运行，通过网络自我复制传播。传播速度比普通病毒快得多。

## 病毒传播途径

| 途径 | 说明 |
|:----:|:------|
| 网络下载 | 恶意网站、盗版软件 |
| U盘/移动硬盘 | 自动运行病毒 |
| 邮件附件 | 伪装成正常附件 |
| 即时通讯 | 通过文件传输传播 |

> **考试重点**：病毒五大特征中最基本的是**传染性**。能区分病毒（需宿主、自我复制）和木马（伪装、不复制）。蠕虫是特殊病毒（独立传播）。',
'计算机病毒是具有自我复制能力的恶意程序。五大特征：传染性（最基本）、破坏性、隐蔽性、潜伏性、可触发性。病毒需宿主文件，蠕虫可独立传播。传播途径：下载/U盘/邮件。',
5, '单元5 运用网络安全技术', '任务2 防治计算机病毒', 1129,
'五大特征记法："传破隐潜触"——传染性（最基本）、破坏性、隐蔽性、潜伏性、可触发性\n\n"病毒像感冒：传染别人（传染性）、让人难受（破坏性）、看不出有病（隐蔽性）、到时间才发作（潜伏性+触发性）"',
'【必考】①五大特征：传染性（最基本）/破坏性/隐蔽性/潜伏性/可触发性 ②病毒需宿主+自我复制 ③蠕虫可独立传播 ④传播途径',
2,
'["计算机病毒","病毒特征","传染性","蠕虫","勒索病毒","恶意程序"]',
'["网络安全","计算机病毒"]',
'[
  {"type":"choice","question":"计算机病毒最根本的特征是什么？","options":["破坏性","隐蔽性","传染性","潜伏性"],"answer":"C","explanation":"传染性（自我复制并感染其他文件）是病毒最基本的特征。没有传染性就不叫病毒。"},
  {"type":"choice","question":"以下关于蠕虫的描述，正确的是？","options":["蠕虫需要宿主程序","蠕虫不需要宿主，可独立传播","蠕虫寄生在Office文档中","蠕虫只能通过U盘传播"],"answer":"B","explanation":"蠕虫是病毒的特殊类型，不需要宿主文件，独立运行并通过网络自我复制传播，传播速度极快。"},
  {"type":"judge","question":"计算机病毒只能通过网络传播。","answer":"F","explanation":"病毒传播途径很多：网络下载、U盘/移动硬盘、邮件附件、即时通讯等。不限于网络一种途径。"},
  {"type":"multi","question":"计算机病毒的五大特征包括哪些？（多选）","options":["传染性","破坏性","隐蔽性","潜伏性","可编译性"],"answer":"A,B,C,D","explanation":"五大特征：传染性（最基本）、破坏性、隐蔽性、潜伏性、可触发性。可编译性不是病毒特征。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10275, 10155, '计算机病毒的五大特征是什么？', '①传染性（最基本）—自我复制感染②破坏性—删除文件/占用资源③隐蔽性—隐藏存在④潜伏性—等待发作⑤可触发性—条件满足才激活。', 1, 'DEFINITION'),
(10276, 10155, '蠕虫和普通病毒有什么区别？', '普通病毒需要宿主程序（寄生在exe/dll等文件中），不能独立运行。蠕虫不需要宿主，独立运行，通过网络自我复制传播（如Slammer、WannaCry）。', 2, 'COMPARISON'),
(10277, 10155, '计算机病毒有哪些传播途径？', '①网络下载（恶意网站/盗版软件）②U盘/移动硬盘（自动运行）③邮件附件（伪装正常文件）④即时通讯文件传输。', 3, 'DEFINITION');

-- ============================================================
-- 文章10156: 杀毒软件的安装与配置（node=1130）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10156, '杀毒软件与病毒防治',
'## 杀毒软件的功能

| 功能 | 说明 |
|:----:|:------|
| **实时监控** | 持续监视文件操作、网络连接，发现威胁立即拦截 |
| **病毒查杀** | 扫描并清除已感染的病毒/木马 |
| **防火墙** | 控制程序联网，阻止恶意连接 |
| **漏洞修复** | 检测系统漏洞并安装补丁 |
| **隐私保护** | 防止个人信息被窃取 |

## 常见杀毒软件

| 软件 | 平台 | 特点 |
|:----:|:----:|:------|
| **Windows Defender** | Windows | 系统自带，免费，够用 |
| **360安全卫士** | Windows | 国产免费，功能全面 |
| **火绒安全** | Windows | 轻量、无广告、拦截弹窗 |
| **腾讯电脑管家** | Windows | 集成QQ安全防护 |
| 卡巴斯基 | 跨平台 | 国际知名，查杀率高 |
| 诺顿 | Windows/Mac | 老牌杀毒软件 |

## 杀毒软件的正确使用方法

| 步骤 | 操作 | 频率 |
|:----:|:------|:----:|
| ① | **安装杀毒软件**（只装1款，不要装多个） | 首次 |
| ② | **更新病毒库**到最新版本 | 建议自动更新 |
| ③ | **全盘扫描** | 建议每周一次 |
| ④ | 开启**实时监控** | 始终开启 |
| ⑤ | 定期**系统修复**和漏洞扫描 | 建议每月 |

> **重要原则**：**只安装一款杀毒软件**。多个杀毒软件同时运行会相互冲突，导致系统变慢甚至防护失效。

## 病毒防治策略

| 策略 | 说明 |
|:----:|:------|
| 安装杀毒软件并定期更新 | 基础防护 |
| **安装系统安全补丁** | 修复系统漏洞（Windows Update） |
| 不打开可疑邮件附件 | 防止恶意代码执行 |
| **使用正版软件** | 盗版软件可能含病毒 |
| **定期备份重要数据** | 减小损失（勒索病毒尤其需要） |
| 不随意插拔不明U盘 | 防止U盘病毒自动运行 |

### 重要数据备份（3-2-1原则）
> 至少**3份**副本，保存在**2种**不同介质上，其中**1份**异地存放。

> **考试重点**：安装杀毒软件+定期更新病毒库是最基本的防毒措施。只装1款杀毒软件。Windows Defender是Windows自带。',
'杀毒软件功能：实时监控、病毒查杀、防火墙、漏洞修复。常见：Windows Defender（自带）、360、火绒。只装1款。防治：安装杀毒+更新系统补丁+使用正版+备份数据。',
5, '单元5 运用网络安全技术', '任务2 防治计算机病毒', 1130,
'杀毒软件使用："装1款→更新库→全盘扫→开监控→打补丁"\n\n"杀毒软件只装1个，装多了打架"',
'【必考】①只安装1款杀毒软件（不装多个）②定期更新病毒库 ③Windows Defender是Windows自带 ④防治策略：打补丁+用正版+备份',
1,
'["杀毒软件","Windows Defender","360","火绒","病毒防治","病毒库"]',
'["网络安全","计算机病毒"]',
'[
  {"type":"choice","question":"使用杀毒软件时，以下哪项做法是正确的？","options":["同时安装多款杀毒软件以加强防护","只安装1款杀毒软件并保持更新","从不更新病毒库","只安装杀毒软件不做全盘扫描"],"answer":"B","explanation":"只安装1款杀毒软件（多款会冲突），保持病毒库自动更新，定期全盘扫描是最佳实践。"},
  {"type":"choice","question":"Windows系统自带的杀毒软件叫什么？","options":["360安全卫士","火绒安全","Windows Defender","诺顿"],"answer":"C","explanation":"Windows Defender是Windows 10/11系统自带的免费杀毒软件，微软持续更新，对大多数用户来说足够使用。"},
  {"type":"judge","question":"同时安装多款杀毒软件可以更好地保护计算机安全。","answer":"F","explanation":"这是常见误区。多款杀毒软件会相互冲突，争夺系统资源，导致系统变慢，甚至防护效果下降。只装1款即可。"},
  {"type":"multi","question":"以下哪些是有效的计算机病毒防治措施？（多选）","options":["安装杀毒软件并更新病毒库","定期安装系统安全补丁","使用正版软件","定期备份重要数据","从不开启防火墙"],"answer":"A,B,C,D","explanation":"安装杀毒软件+更新系统补丁+使用正版+备份数据都是有效防治措施。关闭防火墙会降低安全性。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10278, 10156, '杀毒软件应如何正确使用？', '①只装1款②保持病毒库自动更新③每周全盘扫描④始终开启实时监控⑤每月系统修复。Windows Defender是Windows自带杀毒软件。', 1, 'PROCEDURE'),
(10279, 10156, '常见的杀毒软件有哪些？', '①Windows Defender（系统自带）②360安全卫士（国产免费）③火绒安全（轻量无广告）④腾讯电脑管家⑤卡巴斯基（国际知名）。', 2, 'DEFINITION'),
(10280, 10156, '计算机病毒防治的主要策略有哪些？', '①安装杀毒软件并更新②安装系统补丁（Windows Update）③不打开可疑邮件附件④使用正版软件⑤定期备份数据（3-2-1原则）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10157: 防火墙的概念与类型（node=1131）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10157, '防火墙——网络安全的守门员',
'## 什么是防火墙？

防火墙是位于**内部网络和外部网络之间**的一道安全屏障，通过预定义的安全规则控制进出网络的数据流。

### 一句话理解
> 防火墙就像小区保安——检查每个进出小区的人（数据包），符合规则（有通行证）就放行，不符合就拦住。

## 防火墙的三种类型

| 类型 | 工作层 | 原理 | 特点 |
|:----:|:------:|:------|:----:|
| **包过滤防火墙** | 网络层/传输层 | 检查IP包头信息（源IP、目标IP、端口） | ✅速度快、❌安全性较低 |
| **应用代理防火墙** | 应用层 | 为每种应用提供代理，彻底检查内容 | ✅安全性高、❌速度慢 |
| **状态检测防火墙** | 网络~应用层 | 结合包过滤+连接状态追踪 | ✅当前主流、✅兼顾安全和速度 |

### 各类型详解

**包过滤防火墙**
> 检查数据包的"信封"（源IP、目标IP、端口号），不拆开"信"看内容。速度快但只能做粗粒度控制。

**应用代理防火墙**
> 拆开"信"逐字检查内容，可以识别应用层攻击（如SQL注入）。安全性最高但速度最慢。

**状态检测防火墙**
> 不仅检查单个包，还追踪整个连接的状态（如TCP三次握手是否完整）。现代防火墙的主流技术。

## 防火墙基本规则示例

```
默认策略: 一切禁止（白名单模式）

允许 内网 → 外网 :80（HTTP）    ← 允许访问网页
允许 内网 → 外网 :443（HTTPS）  ← 允许安全访问
拒绝 外网 → 内网 :3389（RDP）   ← 阻止远程桌面入侵
允许 内网 → 外网 :53（DNS）    ← 允许域名解析
```

### 两种默认策略
| 策略 | 说明 | 安全性 |
|:----:|:------|:------:|
| **默认禁止**（白名单） | 只放行明确允许的 | 高（推荐） |
| **默认允许**（黑名单） | 只拦截已知危险的 | 低 |

## 硬件防火墙 vs 软件防火墙

| 类型 | 说明 | 适用场景 |
|:----:|:------|:---------|
| **硬件防火墙** | 专用网络设备 | 企业/学校网络边界 |
| **软件防火墙** | 操作系统上的程序 | 个人电脑、服务器 |

### Windows防火墙
> 控制面板 → Windows Defender防火墙。可设置**入站规则**和**出站规则**。程序首次联网时提示是否允许。

> **考试重点**：防火墙是内网和外网之间的安全屏障。包过滤（快但不深）、应用代理（慢但彻底）、状态检测（当前主流）。',
'防火墙控制内部网络和外部网络之间的数据流。三种类型：包过滤（网络层/速度快）、应用代理（应用层/安全性高）、状态检测（当前主流）。硬件vs软件防火墙。',
5, '单元5 运用网络安全技术', '任务3 使用防火墙', 1131,
'防火墙类型记法："包过滤看信封不拆信（快），应用代理拆信逐字查（安全慢），状态检测两者兼顾（主流）"\n\n"防火墙=小区保安，查通行证放行"',
'【必考】①防火墙是内外网之间的安全屏障 ②三种类型：包过滤/应用代理/状态检测 ③包过滤=网络层，应用代理=应用层 ④默认禁止>默认允许',
2,
'["防火墙","包过滤","应用代理","状态检测","Windows防火墙","网络安全"]',
'["网络安全","防火墙"]',
'[
  {"type":"choice","question":"防火墙的主要作用是什么？","options":["加速网络传输","控制内外网络之间的数据流量","分配IP地址","解析域名"],"answer":"B","explanation":"防火墙位于内部网络和外部网络之间，通过预设规则控制进出网络的数据流，阻止非法访问。"},
  {"type":"choice","question":"包过滤防火墙工作在哪一层？","options":["应用层","网络层/传输层","数据链路层","物理层"],"answer":"B","explanation":"包过滤防火墙检查IP包的源IP、目标IP、端口号等信息，工作在网络层和传输层。"},
  {"type":"judge","question":"防火墙的默认策略中，」默认禁止」比」默认允许」更安全。","answer":"T","explanation":"默认禁止（白名单）只放行明确允许的流量，安全性高。默认允许（黑名单）只拦截已知危险流量，可能遗漏新威胁。"},
  {"type":"multi","question":"以下哪些是防火墙的类型？（多选）","options":["包过滤防火墙","应用代理防火墙","状态检测防火墙","杀毒软件防火墙","邮件防火墙"],"answer":"A,B,C","explanation":"三种防火墙类型：包过滤（网络层/速度快）、应用代理（应用层/安全性高）、状态检测（当前主流）。杀毒软件和邮件不是防火墙类型。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10281, 10157, '什么是防火墙？它的作用是什么？', '防火墙是内外网络之间的安全屏障，通过预设规则控制数据流进出。类比：小区保安检查通行证。', 1, 'DEFINITION'),
(10282, 10157, '防火墙有哪三种类型？各有什么特点？', '①包过滤（网络层、速度快、安全性低）②应用代理（应用层、速度慢、安全性高）③状态检测（当前主流、兼顾安全和速度）。', 2, 'COMPARISON'),
(10283, 10157, 'Windows防火墙可以做什么？', 'Windows防火墙是软件防火墙，可设置入站规则和出站规则，控制程序能否联网。在控制面板的Windows Defender防火墙中配置。', 3, 'APPLICATION');

-- ============================================================
-- 文章10158: 安全上网习惯（node=1132）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10158, '安全上网习惯——保护自己从习惯开始',
'## 密码安全

| 原则 | 说明 |
|:----:|:------|
| **复杂密码** | 至少8位，含大小写字母+数字+特殊符号（如 `Abc@2024#`） |
| **不同站不同密码** | 防止"撞库"攻击（一个网站泄露，全部账号沦陷） |
| **定期更换密码** | 建议3~6个月更换一次 |
| **开启两步验证** | 密码+短信验证码/邮箱验证/指纹（大幅提升安全性） |

### 常见弱密码
> ❌ 123456、password、111111、生日、手机号、qwerty
> ✅ `Jszx#2024@School`（头字母+年份+特殊符号）

## 浏览安全

| 习惯 | 原因 |
|:----:|:------|
| **使用HTTPS网站** | 数据加密传输，防止被窃听 |
| **不打开可疑链接** | 可能是钓鱼网站或恶意下载 |
| **确认网站域名** | 防止假冒网站（如 `pa1.com` 冒充 `pal.com`） |
| **不在公共Wi-Fi输入密码** | 公共Wi-Fi可能被监听（中间人攻击） |

## 反钓鱼技巧

钓鱼邮件/链接的常见特征：
| 特征 | 说明 | 正常情况 |
|:----:|:------|:--------:|
| 发件地址异常 | `@gmail.com` 冒充 `@school.edu.cn` | 域名与官方一致 |
| 制造紧迫感 | "您的账户将立即关闭！" | 不会威胁恐吓 |
| 链接与声称不符 | 链接指向 `www.xxx.com` 但说自己是"银行" | 链接域名应一致 |
| 索要敏感信息 | 要求提供密码/验证码/银行卡号 | 正规网站不会索要密码 |

## 日常安全习惯清单

| 习惯 | 优先级 |
|:----:|:------:|
| ✅ 定期更新操作系统和软件 | ⭐⭐⭐ |
| ✅ 安装杀毒软件并保持病毒库更新 | ⭐⭐⭐ |
| ✅ 备份重要文件（3-2-1原则） | ⭐⭐⭐ |
| ✅ 不在不明网站输入个人信息 | ⭐⭐⭐ |
| ✅ 不随意插入不明U盘 | ⭐⭐ |
| ✅ 离开电脑时锁定屏幕（Win+L） | ⭐⭐ |

### 数据备份 3-2-1 原则
> **3**份副本、**2**种不同介质（如硬盘+云盘）、**1**份异地存放

> **考试重点**：安全上网的基本常识——复杂密码+不同站不同密码+两步验证、使用HTTPS、不点可疑链接、定期备份。',
'安全上网习惯：复杂密码+两步验证、不同站不同密码、确认HTTPS网站、不点可疑链接、不在公共Wi-Fi输密码、定期备份（3-2-1原则）。钓鱼邮件特征：地址异常+紧迫感+索要密码。',
5, '单元5 运用网络安全技术', '任务3 使用防火墙', 1132,
'密码安全记法："8位以上+大小写+数字+符号+不同站不同密码+两步验证"\n\n反钓鱼口诀："急急忙忙让你点，要你密码验证码——100%是钓鱼"',
'【必考】①密码安全：复杂/不同站/定期换/两步验证 ②HTTPS比HTTP安全 ③不点可疑链接 ④钓鱼邮件特征 ⑤3-2-1备份原则',
1,
'["安全上网","密码安全","钓鱼邮件","HTTPS","两步验证","数据备份"]',
'["网络安全","安全上网"]',
'[
  {"type":"choice","question":"以下哪个密码安全性最高？","options":["123456","password","Abc@2024#School","111111"],"answer":"C","explanation":"Abc@2024#School含大小写字母+数字+特殊符号，长度超过8位，符合密码安全要求。其余均为常见弱密码。"},
  {"type":"choice","question":"钓鱼邮件的常见特征不包括哪项？","options":["制造紧迫感要求立即操作","发件地址与官方一致","要求提供密码或验证码","链接域名与声称的不一致"],"answer":"B","explanation":"钓鱼邮件的特征：发件地址异常（非官方域名）、制造紧迫感、索要密码、链接与声称不符。发件地址与官方一致是正常邮件的特征。"},
  {"type":"judge","question":"不同网站使用相同的密码是安全的，方便记忆。","answer":"F","explanation":"这是非常危险的做法。一旦一个网站泄露密码，攻击者可以用相同密码登录你的其他所有账号（撞库攻击）。不同站要用不同密码。"},
  {"type":"multi","question":"以下哪些是安全上网的好习惯？（多选）","options":["使用HTTPS网站","定期更换复杂密码","在公共Wi-Fi登录网银","不打开可疑链接","定期备份重要数据"],"answer":"A,B,D,E","explanation":"使用HTTPS、定期更换复杂密码、不点可疑链接、备份数据都是好习惯。公共Wi-Fi可能被监听，不应登录网银等敏感账户。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10284, 10158, '密码安全的原则有哪些？', '①至少8位含大小写+数字+特殊符号②不同网站用不同密码③3~6个月更换一次④开启两步验证。避免生日/手机号/123456等弱密码。', 1, 'DEFINITION'),
(10285, 10158, '如何识别钓鱼邮件？', '①发件地址异常（冒充官方）②制造紧迫感("立即关闭你的账户"）③链接域名与声称不符④索要密码/验证码/银行卡号。四条中任何一条都该警惕。', 2, 'APPLICATION'),
(10286, 10158, '什么是数据备份的3-2-1原则？', '3份副本（原件+2个备份）、2种不同介质（如硬盘+云盘+光盘）、1份异地存放（防止火灾/盗窃）。确保数据在灾难情况下仍可恢复。', 3, 'DEFINITION');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v174] 单元5 知识库文章种子完成：共7篇文章，约21张记忆卡片。' AS result;
-- ============================================================================
-- v175: 网络应用基础 — 知识库文章种子（单元6 设计制作网页）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖5个任务13个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元6：设计制作网页
-- ═══════════════════════════════════════════════════════════════
-- 任务1 创建网站（节点1133）
-- 任务2 设计简单网页（节点1134~1138）
-- 任务3 建立列表和超链接（节点1139~1140）
-- 任务4 运用CSS（节点1141~1142）
-- 任务5 使用表单（节点1143~1145）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10159: HTML基本结构（node=1133）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10159, 'HTML基本结构——网页的骨架',
'## 什么是HTML？

HTML（HyperText Markup Language，超文本标记语言）是编写网页的**标准语言**。它使用**标签**来描述网页的结构和内容。

### 一句话理解
> HTML就像盖房子的"框架"——标签是房梁/柱子/墙壁，浏览器把这些框架渲染成你看到的网页。

## HTML基本结构

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <title>网页标题</title>
  </head>
  <body>
    网页可见内容
  </body>
</html>
```

### 各部分说明

| 标签 | 含义 | 说明 |
|:----:|:----:|:------|
| `<!DOCTYPE html>` | 文档类型声明 | 告诉浏览器使用HTML5标准解析 |
| `<html>` | HTML根标签 | 整个HTML文档的根，所有内容都在里面 |
| `<head>` | 头部区域 | 存放元数据、标题、样式、脚本等**不可见**内容 |
| `<meta charset="utf-8">` | 字符编码声明 | 告诉浏览器用UTF-8编码，支持中文显示 |
| `<title>` | 网页标题 | 显示在浏览器标签栏上 |
| `<body>` | 主体区域 | 网页**可见**内容都在这里 |

### 结构记忆
> `<html>` = 房子整体 → `<head>` = 房产证信息（看不到） → `<body>` = 房子里能看到的一切

## HTML标签基本语法

```html
<标签名 属性名="属性值">内容</标签名>
```

| 部分 | 含义 | 示例 |
|:----:|:------|:------|
| **开始标签** | 标签名称+属性 | `<a href="https://www.baidu.com">` |
| **内容** | 标签包围的文本/子标签 | `百度一下` |
| **结束标签** | 带斜杠的标签名 | `</a>` |

### 空标签
> 有些标签没有内容，称为**空标签**，不需要结束标签：
> `<br>`（换行）、`<hr>`（水平线）、`<img>`（图片）、`<input>`（输入框）

> **对口升学必考**：HTML基本结构中`<html>`是根标签，`<head>`放元数据，`<body>`放可见内容。`<title>`定义浏览器标签栏标题。',
'HTML是网页的标准标记语言。基本结构：`<html>`（根）→`<head>`（元数据/标题）→`<body>`（可见内容）。`<title>`定义标签栏标题。`<!DOCTYPE html>`声明HTML5标准。',
5, '单元6 设计制作网页', '任务1 创建网站', 1133,
'HTML结构记法："html是房子整体，head是房产证（看不见），body是屋里摆设（看得见）"\n\n空标签记法："br/hr/img/input=单身汉，没内容就自己"',
'【必考】①HTML基本结构：html/head/title/body ②title在标签栏显示 ③body放可见内容 ④空标签（br/hr/img/input）⑤`<!DOCTYPE html>`是HTML5声明',
1,
'["HTML","超文本标记语言","网页结构","html","head","body","title"]',
'["HTML网页制作基础","HTML基本结构"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10287, 10159, 'HTML的基本结构是什么？', '<!DOCTYPE html><html><head><title>标题</title></head><body>可见内容</body></html>。html=根、head=元数据、body=可见内容。', 1, 'DEFINITION'),
(10288, 10159, 'HTML中<head>和<body>各有什么作用？', '<head>存放元数据、标题、样式、脚本等不可见内容。<body>存放网页的可见内容（文字/图片/表格等）。简单说：head=房产证，body=屋里摆设。', 2, 'DEFINITION'),
(10289, 10159, '什么HTML标签属于空标签？', '空标签没有内容、不需要结束标签。常见：<br>（换行）、<hr>（水平线）、<img>（图片）、<input>（输入框）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10160: 标题与段落标签（node=1134）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10160, '标题标签 h1~h6 与段落标签 p',
'## 标题标签 h1~h6

HTML提供了6级标题，从`<h1>`到`<h6>`，重要性逐级递减。

```html
<h1>一级标题（最重要）</h1>
<h2>二级标题</h2>
<h3>三级标题</h3>
<h4>四级标题</h4>
<h5>五级标题</h5>
<h6>六级标题（最不重要）</h6>
```

### 特点

| 标签 | 字体大小 | 语义重要性 | 使用建议 |
|:----:|:--------:|:---------:|:---------|
| `<h1>` | 最大 | 最高（文章主题） | 每页只用1个 |
| `<h2>` | 较大 | 高（章节标题） | 可以有多个 |
| `<h3>`~`<h6>` | 逐渐变小 | 逐级降低 | 用于子章节 |

> **重要**：`<h1>`在SEO（搜索引擎优化）中权重最高，每页只应有一个`<h1>`。

## 段落标签 p

`<p>`标签定义一个段落，浏览器会自动在段落前后添加间距。

```html
<p>这是第一个段落。段落标签会自动在前后加空行。</p>
<p>这是第二个段落。浏览器会自动换行显示。</p>
```

### 换行 vs 段落
| 标签 | 效果 | 间距 |
|:----:|:------|:----:|
| `<p>` | 段落 | 上下有大间距 |
| `<br>` | 换行 | 无额外间距，直接换到下一行 |

> **对口升学考点**：h1最大最重要、h6最小最不重要。p标签定义段落，浏览器自动添加间距。h1每页只用一个。',
'HTML标题：h1（最大/最重要/每页一个）~h6（最小）。p标签定义段落，浏览器自动添加上下间距。注意p（段落有大间距）和br（换行无间距）的区别。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1134,
'h1~h6记法："h1最大最重要用一次，h2次之随便用，越往后越小越不重要"\n\np vs br："p=段落（上方空一行），br=换行（直接下去）"',
'【必考】①h1最大最重要、h6最小 ②h1每页只用1个 ③p定义段落 ④p和br的区别（p有间距、br无间距）',
1,
'["HTML","标题标签","h1","h2","段落标签","p","SEO"]',
'["HTML网页制作基础","标题与段落"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10290, 10160, 'HTML的h1~h6标签有什么特点？', '共6级标题，h1最大最重要（每页只用1个），h6最小最不重要。搜索引擎对h1权重最高。按h1→h2→h3→h4→h5→h6逐级使用。', 1, 'DEFINITION'),
(10291, 10160, '<p>标签的作用是什么？', '<p>定义段落，浏览器自动在段落前后添加间距。一个<p>标签包一段文字，多个<p>标签的文字会分段显示。', 2, 'DEFINITION'),
(10292, 10160, '<p>和<br>有什么区别？', '<p>（段落）：有上下间距，适合段落分隔。<br>（换行）：直接换到下一行，无额外间距。p=新段落空一行，br=直接换行。', 3, 'COMPARISON');

-- ============================================================
-- 文章10161: 换行与水平线（node=1135）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10161, '换行标签 br 与水平线 hr',
'## 换行标签 br

`<br>`标签在网页中**强制换行**，是空标签（不需结束标签）。

```html
<p>
  第一行文字<br>
  第二行文字（换行后）<br>
  第三行文字（再换行）
</p>
```

### 显示效果
```
第一行文字
第二行文字（换行后）
第三行文字（再换行）
```

> 使用`<br>`换行后没有额外间距，直接到下一行。

## 水平线标签 hr

`<hr>`标签在网页中显示一条**水平分割线**，也是空标签。

```html
<h2>章节一</h2>
<p>第一章的内容...</p>
<hr>
<h2>章节二</h2>
<p>第二章的内容...</p>
```

### hr标签的属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `width` | 水平线宽度（像素或百分比） | `<hr width="50%">` |
| `size` | 水平线粗细（像素） | `<hr size="5">` |
| `color` | 水平线颜色 | `<hr color="red">` |
| `align` | 对齐方式（left/center/right） | `<hr align="center">` |

### 显示效果
```
─── 章节一 ───
第一章的内容...
────────────────  ← <hr> 水平分割线
─── 章节二 ───
第二章的内容...
```

> **对口升学考点**：br=换行（空标签）、hr=水平线（空标签）。hr常用属性：width、size、color、align。',
'<br>强制换行（空标签，无间距）。<hr>显示水平分割线（空标签），常用属性：width（宽度）、size（粗细）、color（颜色）、align（对齐）。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1135,
'br="break换行"，hr="horizontal水平线"\n\n"br是回车键，hr是分割线"\n\nhr属性记法："宽(度)粗(细)颜色对齐——width/size/color/align"',
'【必考】①<br>强制换行（空标签）②<hr>水平线（空标签）③hr属性：width/size/color/align',
1,
'["HTML","br","hr","换行","水平线","空标签"]',
'["HTML网页制作基础","换行与水平线"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10293, 10161, '<br>标签的作用是什么？', '<br>强制换行，将文本换到下一行显示。空标签，不需要结束标签。使用后无额外间距，直接到下一行。', 1, 'DEFINITION'),
(10294, 10161, '<hr>标签的作用和常用属性？', '<hr>显示水平分割线，空标签。常用属性：width（宽度，像素或百分比）、size（粗细）、color（颜色）、align（对齐left/center/right）。', 2, 'DEFINITION'),
(10295, 10161, '<br>和<hr>有什么共同特点和区别？', '共同：都是空标签（无需结束标签）。区别：<br>换行（内容换到下一行），<hr>添加水平分割线（页面分隔）。', 3, 'COMPARISON');

-- ============================================================
-- 文章10162: 文本格式标签（node=1136）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10162, '文本格式标签——让文字变样',
'## HTML文本格式标签

HTML提供了多种标签来改变文字的样式和语义。

### 常用文本格式标签

```html
<b>加粗文字</b>
<i>斜体文字</i>
<u>下划线文字</u>
<s>删除线文字</s>
<font color="red" size="5">红色大号文字</font>
```

### 各标签效果

| 标签 | 效果 | 语义（含义） |
|:----:|:----:|:-------------|
| `<b>` | **加粗** | 仅仅是视觉加粗，无语义 |
| `<strong>` | **加粗** | 表示重要内容（语义加强） |
| `<i>` | *斜体* | 仅仅是斜体，无语义 |
| `<em>` | *斜体* | 表示强调（语义加强） |
| `<u>` | 下划线 | 添加下划线 |
| `<s>` | ~~删除线~~ | 表示已删除的内容 |
| `<font>` | 设置字体 | 通过color/size/face设置颜色/大小/字体 |

### `<font>`标签属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `color` | 文字颜色（英文名/十六进制） | `color="red"`、`color="#FF0000"` |
| `size` | 文字大小（1~7） | `size="5"`（默认3） |
| `face` | 字体名称 | `face="宋体"`、`face="Arial"` |

### 示例代码
```html
<p>
  <b>加粗</b> · <i>斜体</i> · <u>下划线</u> · <s>删除线</s><br>
  <font color="blue" size="4">蓝色4号字</font><br>
  <font color="#FF0000" size="6" face="黑体">红色6号黑体</font>
</p>
```

> **对口升学考点**：b=加粗、i=斜体、u=下划线、s=删除线。font标签的color/size/face属性。',
'文本格式标签：b（加粗）、i（斜体）、u（下划线）、s（删除线）。font标签设置文字样式：color（颜色）、size（大小1~7）、face（字体）。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1136,
'b/i/u/s记法："b=bold加粗，i=italic斜体，u=underline下划线，s=strikethrough删除线"\n\nfont属性："color颜色/size大小/face字体"',
'【必考】①b=加粗、i=斜体、u=下划线、s=删除线 ②font标签的color/size/face三个属性 ③size取值1~7（默认3）',
1,
'["HTML","文本格式","b","i","u","s","font","加粗","斜体","下划线","删除线"]',
'["HTML网页制作基础","文本格式"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10296, 10162, 'HTML中b/i/u/s标签分别有什么作用？', 'b=加粗（bold）、i=斜体（italic）、u=下划线（underline）、s=删除线（strikethrough）。用于改变文字的外观样式。', 1, 'DEFINITION'),
(10297, 10162, '<font>标签有哪些属性？各有什么作用？', 'color：文字颜色（red/#FF0000）。size：文字大小（1~7，默认3）。face：字体名称（宋体/Arial）。已不推荐使用，建议用CSS代替。', 2, 'DEFINITION'),
(10298, 10162, '<b>和<strong>有什么区别？', '<b>仅仅是视觉加粗，无语义含义。<strong>也表示加粗，但有语义强调的含义，搜索引擎和屏幕阅读器更重视<strong>。', 3, 'COMPARISON');

-- ============================================================
-- 文章10163: 图片标签 img（node=1137）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10163, '图片标签 img——在网页中插入图片',
'## img标签

`<img>`标签在网页中**插入图片**，是空标签（不需要结束标签）。

```html
<img src="图片地址" alt="替代文本">
```

### 必选属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| **`src`** | 图片文件的**路径/URL**（必填） | `src="logo.jpg"`、`src="https://xxx.com/1.png"` |
| **`alt`** | 图片无法显示时的**替代文本**（必填） | `alt="公司Logo"` |

### 可选属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `width` | 图片宽度（像素或百分比） | `width="200"`、`width="50%"` |
| `height` | 图片高度（像素或百分比） | `height="150"` |
| `title` | 鼠标悬停时显示的提示文字 | `title="点击查看大图"` |
| `border` | 图片边框宽度（像素） | `border="1"` |

### 完整示例
```html
<img src="campus.jpg" alt="校园风光"
     width="400" height="300" title="点击放大">
```

### src的两种路径

| 路径类型 | 说明 | 示例 |
|:--------:|:------|:------|
| **相对路径** | 相对于当前HTML文件的路径 | `src="images/photo.jpg"`、`src="../logo.png"` |
| **绝对路径** | 完整的网络URL | `src="https://www.example.com/images/1.jpg"` |

### 图片显示优化
> 建议设置`width`和`height`，这样图片加载前浏览器就知道占多大空间，防止页面布局跳动。

> **对口升学考点**：img标签的两个必选属性——src（图片路径）和alt（替代文本）。空标签。',
'<img>标签在网页中插入图片。必选属性：src（图片路径）、alt（替代文本）。可选：width（宽度）、height（高度）、title（提示文字）、border（边框）。空标签。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1137,
'img标签记法："img=image图片"\n\n必选属性2个："src告诉浏览器去哪找图片，alt告诉浏览器图片坏了显示什么"',
'【必考】①img是空标签 ②src=图片路径（必选）③alt=替代文本（必选）④相对路径vs绝对路径 ⑤width/height设置宽高',
1,
'["HTML","img","图片","src","alt","相对路径","绝对路径"]',
'["HTML网页制作基础","图片标签"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10299, 10163, 'HTML中如何插入图片？', '使用<img>标签。必选属性：src="图片路径"、alt="替代文本"。可选：width（宽）、height（高）、title（提示）、border（边框）。空标签。', 1, 'DEFINITION'),
(10300, 10163, '<img>的src属性使用相对路径和绝对路径有什么区别？', '相对路径：相对于当前HTML文件的位置，如images/logo.jpg。绝对路径：完整的URL，如https://xxx.com/logo.jpg。', 2, 'DEFINITION'),
(10301, 10163, '为什么<img>标签最好设置width和height？', '设置宽高后，浏览器在图片加载前就知道占多大空间，防止图片加载时页面布局突然跳动（布局偏移），提升用户体验。', 3, 'APPLICATION');

-- ============================================================
-- 文章10164: 表格标签 table（node=1138）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10164, '表格标签——用表格展示数据',
'## HTML表格结构

HTML表格使用`<table>`标签创建，由行（`<tr>`）和单元格（`<td>`/`<th>`）组成。

```html
<table border="1" width="80%" align="center">
  <tr>
    <th>姓名</th>
    <th>科目</th>
    <th>成绩</th>
  </tr>
  <tr>
    <td>张三</td>
    <td>计算机网络</td>
    <td>95</td>
  </tr>
  <tr>
    <td>李四</td>
    <td>计算机网络</td>
    <td>88</td>
  </tr>
</table>
```

### 表格相关标签

| 标签 | 含义 | 说明 |
|:----:|:----:|:------|
| `<table>` | 表格容器 | 整个表格的根标签 |
| `<tr>` | 行（Table Row） | 一行中可以放多个单元格 |
| `<th>` | 表头单元格（Table Header） | 自动加粗居中，用于列标题 |
| `<td>` | 普通单元格（Table Data） | 默认左对齐，存放数据 |

### table常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `border` | 表格边框宽度（像素） | `border="1"` |
| `width` | 表格宽度（像素或百分比） | `width="80%"`、`width="500"` |
| `align` | 表格对齐方式 | `align="center"` |
| `bgcolor` | 表格背景颜色 | `bgcolor="#f0f0f0"` |
| `cellpadding` | 单元格内边距（内容到边框的距离） | `cellpadding="5"` |
| `cellspacing` | 单元格间距（单元格之间的距离） | `cellspacing="0"` |

### tr/td常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `align` | 行/单元格内水平对齐 | `align="center"` |
| `valign` | 垂直对齐（top/middle/bottom） | `valign="middle"` |
| `colspan` | 跨列合并（水平合并单元格） | `colspan="2"` |
| `rowspan` | 跨行合并（垂直合并单元格） | `rowspan="3"` |

### 合并单元格示例
```html
<table border="1">
  <tr>
    <td colspan="2">合并两列</td>
    <td>普通单元格</td>
  </tr>
  <tr>
    <td rowspan="2">合并两行</td>
    <td>数据A</td>
    <td>数据B</td>
  </tr>
</table>
```

> **对口升学考点**：table=表格、tr=行、th=表头（加粗居中）、td=普通单元格。border设置边框，colspan/rowspan合并单元格。',
'HTML表格：<table>（表格）、<tr>（行）、<th>（表头加粗居中）、<td>（普通单元格）。属性：border边框、width宽度、colspan跨列合并、rowspan跨行合并。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1138,
'table结构记法："table=桌子，tr=一排（行），th和td=桌上的格子"\n\nth=表头（加粗居中），td=数据（左对齐）\ncolspan=横向合并占几列，rowspan=纵向合并占几行',
'【必考】①table/tr/th/td四个标签的关系 ②border设置边框 ③th自动加粗居中 ④colspan（跨列）和rowspan（跨行）合并单元格',
2,
'["HTML","表格","table","tr","th","td","colspan","rowspan","合并单元格"]',
'["HTML网页制作基础","表格"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10302, 10164, 'HTML表格的基本结构是什么？', '<table><tr><th>表头</th><th>表头</th></tr><tr><td>数据</td><td>数据</td></tr></table>。table=表格、tr=行、th=表头、td=单元格。', 1, 'DEFINITION'),
(10303, 10164, 'colspan和rowspan各有什么作用？', 'colspan（跨列合并）：一个单元格占据多列宽度。rowspan（跨行合并）：一个单元格占据多行高度。用于制作复杂表头。', 2, 'DEFINITION'),
(10304, 10164, '<th>和<td>有什么不同？', '<th>（表头单元格）默认加粗居中，用于列标题。<td>（数据单元格）默认左对齐，存放实际数据。', 3, 'COMPARISON');

-- ============================================================
-- 文章10165: 有序列表与无序列表（node=1139）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10165, '列表标签——有序与无序',
'## 无序列表 ul/li

无序列表使用**圆点**作为项目符号，适合表示不分先后的项目。

```html
<h3>课程列表（无序）</h3>
<ul>
  <li>计算机网络基础</li>
  <li>网页设计与制作</li>
  <li>数据库应用</li>
  <li>编程语言基础</li>
</ul>
```

### 显示效果
```
• 计算机网络基础
• 网页设计与制作
• 数据库应用
• 编程语言基础
```

`<ul>`的`type`属性可改变项目符号样式：
| type值 | 效果 |
|:------:|:------:|
| `disc` | ● 实心圆（默认） |
| `circle` | ○ 空心圆 |
| `square` | ■ 实心方块 |

## 有序列表 ol/li

有序列表使用**数字编号**，适合表示有先后顺序的项目。

```html
<h3>开机步骤（有序）</h3>
<ol>
  <li>按下电源按钮</li>
  <li>等待系统启动</li>
  <li>输入用户名和密码</li>
  <li>进入桌面</li>
</ol>
```

### 显示效果
```
1. 按下电源按钮
2. 等待系统启动
3. 输入用户名和密码
4. 进入桌面
```

`<ol>`的`type`属性可改变编号类型：
| type值 | 效果 | 示例 |
|:------:|:------|:----:|
| `1` | 阿拉伯数字（默认） | 1. 2. 3. |
| `A` | 大写字母 | A. B. C. |
| `a` | 小写字母 | a. b. c. |
| `I` | 大写罗马数字 | I. II. III. |
| `i` | 小写罗马数字 | i. ii. iii. |

> **对口升学考点**：ul=无序列表（圆点符号）、ol=有序列表（数字编号）、li=列表项。type属性改变符号/编号样式。',
'ul（无序列表）=圆点符号、ol（有序列表）=数字编号。li定义列表项。ol的type属性可改为字母（A/a）或罗马数字（I/i）。ul的type可改为disc/circle/square。',
5, '单元6 设计制作网页', '任务3 建立列表和超链接', 1139,
'ul vs ol："ul=圆点不分先后，ol=数字按顺序"\n\nol的type："1数字/A大写/a小写/I罗马大写/i罗马小写"',
'【必考】①ul=无序（圆点）②ol=有序（数字）③li=列表项 ④ol的type属性改变编号类型（1/A/a/I/i）',
1,
'["HTML","列表","ul","ol","li","无序列表","有序列表"]',
'["HTML网页制作基础","列表"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10305, 10165, '无序列表ul和有序列表ol有什么区别？', 'ul无序列表：使用圆点●作为项目符号，项目不分先后。ol有序列表：使用数字1/2/3编号，项目有先后顺序。两者都用<li>定义列表项。', 1, 'COMPARISON'),
(10306, 10165, '有序列表<ol>的type属性有哪些取值？', 'type="1"（默认数字）、type="A"（大写字母）、type="a"（小写字母）、type="I"（大写罗马）、type="i"（小写罗马）。', 2, 'DEFINITION'),
(10307, 10165, '无序列表<ul>的type属性有哪些取值？', 'type="disc"（实心圆点●默认）、type="circle"（空心圆○）、type="square"（实心方块■）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10166: 超链接标签 a（node=1140）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10166, '超链接 a——网页间的桥梁',
'## 超链接标签 a

`<a>`标签定义超链接，用于从一个页面跳转到另一个页面或其他资源。

```html
<a href="目标地址" target="打开方式">链接文本</a>
```

### a标签的核心属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| **`href`** | 链接目标的URL（**必填**） | `href="https://www.baidu.com"` |
| **`target`** | 链接的打开方式 | `target="_blank"`（新窗口打开） |
| `title` | 鼠标悬停时的提示文字 | `title="点击访问百度"` |

### target取值

| 值 | 说明 |
|:----:|:------|
| `_self` | 在当前窗口打开（默认） |
| `_blank` | 在**新窗口/新标签页**打开（最常用） |

## 四种超链接类型

### 1. 外部链接——链接到其他网站
```html
<a href="https://www.baidu.com" target="_blank">访问百度</a>
```

### 2. 内部链接——链接到本站其他页面
```html
<a href="about.html">关于我们</a>
<a href="news/2024.html">2024年新闻</a>
```

### 3. 锚点链接——页面内跳转
```html
<!-- 先定义锚点 -->
<h2 id="section1">第一章</h2>

<!-- 链接到锚点 -->
<a href="#section1">跳转到第一章</a>

<!-- 从其他页面跳转到锚点 -->
<a href="help.html#faq">查看常见问题</a>
```

### 4. 邮件链接——点击发送邮件
```html
<a href="mailto:admin@school.edu.cn">发送邮件给管理员</a>
```
点击后自动打开默认邮件客户端，收件人地址已填好。

### 链接的样式
> 浏览器默认样式：未访问=蓝色下划线，已访问=紫色下划线，悬停=变手形指针。

> **对口升学必考**：a标签的href（链接地址）和target（_blank新窗口）属性。四种链接类型：外部/内部/锚点/邮件。',
'<a>标签定义超链接。href（必填）指定链接地址，target="_blank"新窗口打开。四种类型：外部链接（其他网站）、内部链接（本站页面）、锚点链接（#id页面内跳转）、邮件链接（mailto:）。',
5, '单元6 设计制作网页', '任务3 建立列表和超链接', 1140,
'a标签记法："a=anchor锚"\n\n四种链接："外部连别人，内部连自己，锚点连本页，#号，邮件连邮箱mailto:"',
'【必考】①<a href="...">链接文本</a> ②target="_blank"新窗口打开 ③四种链接类型：外部/内部/锚点/邮件 ④锚点链接用#id',
2,
'["HTML","超链接","a标签","href","target","_blank","锚点链接","邮件链接"]',
'["HTML网页制作基础","超链接"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10308, 10166, '<a>标签中href和target属性的作用？', 'href（必填）：指定链接目标地址。target：指定打开方式，_blank在新窗口打开，_self（默认）在当前窗口打开。', 1, 'DEFINITION'),
(10309, 10166, '超链接有哪四种类型？各怎么实现？', '①外部：href="https://..." ②内部：href="page.html" ③锚点：href="#id" ④邮件：href="mailto:邮箱地址"。', 2, 'DEFINITION'),
(10310, 10166, 'target="_blank"和默认的_self有什么区别？', 'target="_blank"：在新窗口或新标签页打开链接（原页面保留）。默认情况：在当前窗口打开，原页面被替换。外部链接推荐使用_blank。', 3, 'COMPARISON');

-- ============================================================
-- 文章10167: CSS引入方式（node=1141）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10167, 'CSS三种引入方式——给网页穿衣服',
'## 什么是CSS？

CSS（Cascading Style Sheets，层叠样式表）用于控制网页的**外观样式**——颜色、字体、布局、背景等。

### 一句话理解
> HTML是房子的"框架结构"，CSS是房子的"装修设计"——把灰白的HTML变得漂亮。

## CSS的三种引入方式

### 1. 行内样式（Inline Style）

在HTML标签的`style`属性中直接写CSS。

```html
<p style="color: red; font-size: 20px;">红色20像素的文字</p>
```

| 优点 | 缺点 |
|:----:|:------|
| 最直接，针对单个元素 | 不利于复用，代码冗余 |
| 优先级最高 | 维护困难 |

### 2. 内部样式表（Internal Style Sheet）

在`<head>`中使用`<style>`标签定义样式，当前页面可复用。

```html
<head>
  <style>
    p {
      color: blue;
      font-size: 16px;
    }
    h1 {
      color: #333;
      text-align: center;
    }
  </style>
</head>
```

| 优点 | 缺点 |
|:----:|:------|
| 本页面可复用，不用每个标签都写 | 其他页面无法使用 |
| 结构和样式部分分离 | 多页面时重复 |

### 3. 外部样式表（External Style Sheet）

将CSS写在一个单独的`.css`文件中，在HTML中用`<link>`引入。

**style.css文件：**
```css
body {
  background-color: #f0f0f0;
  font-family: "宋体", Arial, sans-serif;
}
p {
  color: #666;
  line-height: 1.8;
}
```

**HTML文件中引入：**
```html
<head>
  <link rel="stylesheet" type="text/css" href="style.css">
</head>
```

| 优点 | 缺点 |
|:----:|:------|
| 多个页面共享同一样式 | 需要额外加载CSS文件 |
| 结构和样式完全分离 | 第一次加载稍慢 |
| 维护最方便（改一个文件全站变） | — |

## 三种方式对比

| 引入方式 | 复用性 | 维护性 | 优先级 |
|:--------:|:------:|:------:|:------:|
| **行内样式** | 最低 | 最低 | **最高** |
| **内部样式表** | 本页面 | 中等 | 中 |
| **外部样式表** | 全站 | 最高 | 最低 |

### 优先级原则
> **行内 > 内部 > 外部**（就近原则——越靠近标签的样式优先级越高）。

> **对口升学必考**：三种CSS引入方式——行内（style属性）、内部（<style>标签）、外部（<link>引入.css文件）。各自的优缺点和优先级。',
'CSS三种引入方式：行内（标签style属性）、内部（<head>中<style>）、外部（<link>引入.css文件）。行内优先级最高、复用最差。外部优先级最低、复用最好。',
5, '单元6 设计制作网页', '任务4 运用CSS', 1141,
'CSS三种方式："行内=直接写在标签里（最优先）、内部=写在head的style里（本页面用）、外部=独立.css文件（全站用）"\n\n优先级："行内>内部>外部——越靠近标签优先级越高"',
'【必考】①三种CSS引入方式 ②行内（style属性）/内部（style标签）/外部（link引入）③优先级：行内>内部>外部 ④各自优缺点',
2,
'["CSS","层叠样式表","行内样式","内部样式表","外部样式表","link","style"]',
'["HTML网页制作基础","CSS"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10311, 10167, 'CSS有哪三种引入方式？', '①行内样式：标签style属性（优先级最高）②内部样式表：<head>中<style>标签（本页面用）③外部样式表：<link>引入.css文件（全站通用）。', 1, 'DEFINITION'),
(10312, 10167, '三种CSS引入方式的优先级顺序是什么？', '行内样式（最高）> 内部样式表 > 外部样式表（最低）。原则：越靠近HTML标签的样式优先级越高——就近原则。', 2, 'COMPARISON'),
(10313, 10167, '外部样式表有什么优缺点？', '优点：多个页面可共享同一CSS文件、修改一个文件全站更新、结构与样式完全分离。缺点：需额外加载CSS文件、第一次加载稍慢。', 3, 'DEFINITION');

-- ============================================================
-- 文章10168: 常用CSS属性（node=1142）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10168, '常用CSS属性——装修网页的工具箱',
'## CSS基本语法

```css
选择器 {
  属性名: 属性值;
  属性名: 属性值;
}
```

示例：
```css
p {
  color: red;
  font-size: 16px;
}
```

## 常用CSS属性

### 文字样式

| CSS属性 | 说明 | 示例 |
|:-------:|:------|:------|
| `color` | 文字颜色 | `color: red;`、`color: #FF0000;` |
| `font-size` | 文字大小 | `font-size: 16px;`、`font-size: 1.2em;` |
| `font-weight` | 文字粗细 | `font-weight: bold;`（加粗）、`normal` |
| `font-style` | 文字风格 | `font-style: italic;`（斜体） |
| `font-family` | 字体名称 | `font-family: "宋体", Arial;` |
| `text-align` | 水平对齐 | `text-align: center;`（居中） |
| `text-decoration` | 文字装饰 | `text-decoration: underline;`（下划线） |
| `line-height` | 行高 | `line-height: 1.8;` |

### 背景样式

| CSS属性 | 说明 | 示例 |
|:-------:|:------|:------|
| `background-color` | 背景颜色 | `background-color: #f0f0f0;` |
| `background-image` | 背景图片 | `background-image: url("bg.jpg");` |

### 盒模型属性（间距和边框）

```css
div {
  margin: 20px;        /* 外边距——元素与其他元素之间的距离 */
  padding: 10px;       /* 内边距——元素内容与边框之间的距离 */
  border: 1px solid black;  /* 边框：粗细 样式 颜色 */
}
```

| CSS属性 | 说明 | 示例 |
|:-------:|:------|:------|
| **`margin`** | 外边距（元素外部间距） | `margin: 10px;`（四边）、`margin-top: 5px;` |
| **`padding`** | 内边距（元素内部间距） | `padding: 10px;`、`padding-left: 15px;` |
| **`border`** | 边框（简写属性） | `border: 1px solid #ccc;` |
| `width` | 元素宽度 | `width: 200px;`、`width: 50%;` |
| `height` | 元素高度 | `height: 100px;` |

### border详解
```css
/* 简写方式：粗细  样式  颜色 */
border: 2px solid red;     /* 实线边框 */
border: 2px dashed blue;   /* 虚线边框 */
border: 2px dotted green;  /* 点线边框 */

/* 分别设置各边 */
border-top: 1px solid #333;
border-bottom: 2px solid #ccc;
```

### 完整示例
```css
.card {
  width: 300px;
  margin: 20px auto;         /* 上下20px，左右居中 */
  padding: 15px;
  border: 1px solid #ddd;
  background-color: #fff;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
}
```

> **对口升学考点**：常用CSS属性——color（颜色）、font-size（大小）、background-color（背景色）、margin（外边距）、padding（内边距）、border（边框）。',
'CSS常用属性：color（颜色）、font-size（大小）、background-color（背景色）、margin（外边距）、padding（内边距）、border（边框粗细/样式/颜色）、text-align（对齐）、font-weight（粗细）。',
5, '单元6 设计制作网页', '任务4 运用CSS', 1142,
'常用CSS记法："color颜色/font-size字号/background-color背景色/margin外距/padding内距/border边框"\n\nborder简写："border: 粗细 样式 颜色"如"border: 1px solid red"',
'【必考】①color设置文字颜色 ②font-size设置文字大小 ③background-color设置背景色 ④margin=外边距、padding=内边距 ⑤border=边框（粗细/样式/颜色）',
2,
'["CSS","属性","color","font-size","background-color","margin","padding","border","盒模型"]',
'["HTML网页制作基础","CSS"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10314, 10168, 'CSS中控制文字样式的常用属性？', 'color（颜色）、font-size（大小）、font-weight（粗细bold/normal）、font-style（风格italic斜体）、text-align（对齐center/left/right）、text-decoration（装饰underline下划线）。', 1, 'DEFINITION'),
(10315, 10168, 'CSS中margin和padding的区别？', 'margin=外边距，元素边框到相邻元素的距离（拉开元素距离）。padding=内边距，元素内容到边框的距离（撑大元素尺寸）。', 2, 'COMPARISON'),
(10316, 10168, 'CSS中border属性的写法是什么？', 'border: 粗细 样式 颜色。如border: 1px solid red（1像素红色实线）。样式值：solid实线、dashed虚线、dotted点线。', 3, 'DEFINITION');

-- ============================================================
-- 文章10169: 表单与input类型（node=1143）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10169, '表单标签 form 与 input 输入框',
'## 表单标签 form

`<form>`标签用于创建HTML表单，收集用户输入的数据。

```html
<form action="提交地址" method="post">
  <!-- 表单控件放在这里 -->
  <input type="text" name="username">
  <input type="submit" value="提交">
</form>
```

### form常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `action` | 数据提交到的服务器地址 | `action="/login"` |
| `method` | 数据提交方式 | `method="get"`（URL可见）或`method="post"`（URL不可见） |

## input标签——最常用的表单控件

`<input>`是**空标签**，通过`type`属性变换成不同的输入控件。

### 常用input类型

```html
<!-- 文本输入框 -->
用户名：<input type="text" name="username">

<!-- 密码输入框（输入内容隐藏为●） -->
密码：<input type="password" name="pwd">

<!-- 单选框（同一name只能选一项） -->
性别：
<input type="radio" name="gender" value="male"> 男
<input type="radio" name="gender" value="female" checked> 女

<!-- 复选框（同一name可多选） -->
爱好：
<input type="checkbox" name="hobby" value="read"> 阅读
<input type="checkbox" name="hobby" value="sport"> 运动

<!-- 提交按钮 -->
<input type="submit" value="注册">

<!-- 重置按钮 -->
<input type="reset" value="重新填写">
```

### input类型汇总

| type值 | 作用 | 特点 |
|:------:|:------|:------|
| `text` | 单行文本输入 | 最常用的输入框 |
| `password` | 密码输入 | 输入内容显示为● |
| `radio` | 单选框 | 同一name只选一项 |
| `checkbox` | 复选框 | 同一name可选多项 |
| `submit` | 提交按钮 | 将表单数据发送到action地址 |
| `reset` | 重置按钮 | 将表单所有控件恢复为默认值 |

### input常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `type` | 输入框类型 | `type="text"` |
| `name` | 控件名称（提交时作为参数名） | `name="username"` |
| `value` | 控件的值（提交时的数据） | `value="张三"` |
| `checked` | 单选框/复选框默认选中 | `checked` |
| `placeholder` | 输入框提示文字 | `placeholder="请输入用户名"` |
| `readonly` | 只读（不可修改） | `readonly` |
| `disabled` | 禁用（不可操作且不提交） | `disabled` |

> **对口升学必考**：input的type属性——text（文本）、password（密码）、radio（单选）、checkbox（多选）、submit（提交）、reset（重置）。',
'<form>创建表单，action指定提交地址、method指定提交方式（get/post）。<input>的type：text文本、password密码（●）、radio单选、checkbox多选、submit提交、reset重置。',
5, '单元6 设计制作网页', '任务5 使用表单', 1143,
'input type记法："text文本/password密码/radio单选/checkbox多选/submit提交/reset重置"',
'【必考】①input常用type：text/password/radio/checkbox/submit/reset ②radio同一name只能选一个 ③checkbox同一name可多选 ④name是提交时的参数名',
2,
'["HTML","表单","form","input","text","password","radio","checkbox","submit","reset"]',
'["HTML网页制作基础","表单"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10317, 10169, '<input>标签的常用type值有哪些？', 'text（文本输入）、password（密码●）、radio（单选）、checkbox（多选）、submit（提交按钮）、reset（重置按钮）。', 1, 'DEFINITION'),
(10318, 10169, 'radio和checkbox有什么区别？', 'radio（单选框）：同一name只能选一项，用于性别/选择等互斥选项。checkbox（复选框）：同一name可选多项，用于爱好/特长等多选场景。', 2, 'COMPARISON'),
(10319, 10169, '<form>标签的action和method属性各有什么作用？', 'action：指定表单数据提交到的服务器地址。method：指定提交方式——get（数据附在URL后，可见）或post（数据在请求体中，不可见）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10170: textarea与select（node=1144）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10170, '多行文本框 textarea 与下拉列表 select',
'## 多行文本框 textarea

`<textarea>`标签用于输入**多行文本**，适合留言、评论、简介等场景。

```html
<textarea name="intro" rows="5" cols="40">
默认文本内容
</textarea>
```

### textarea常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `name` | 控件名称 | `name="introduction"` |
| `rows` | 可见行数（高度） | `rows="5"`（显示5行） |
| `cols` | 可见列数（宽度） | `cols="40"`（每行40个字符宽） |
| `placeholder` | 提示文字 | `placeholder="请填写个人简介"` |
| `readonly` | 只读 | `readonly` |

### 与input text的区别
| 对比 | `<input type="text">` | `<textarea>` |
|:----:|:---------------------:|:------------:|
| 行数 | 单行 | **多行** |
| 大小 | 默认宽度小 | 可设置rows和cols |
| 回车 | 不能换行 | **可以换行** |
| 结束标签 | 空标签（无） | **有**`</textarea>` |

## 下拉列表 select/option

`<select>`标签创建下拉列表，`<option>`定义列表中的选项。

```html
<select name="city">
  <option value="beijing">北京</option>
  <option value="shanghai" selected>上海</option>
  <option value="chengdu">成都</option>
</select>
```

### select常用属性

| 属性 | 说明 |
|:----:|:------|
| `name` | 控件名称 |
| `size` | 可见选项数（大于1时变为列表框） |
| `multiple` | 允许按住Ctrl多选 |

### option常用属性

| 属性 | 说明 |
|:----:|:------|
| `value` | 提交时的值（不写则提交标签内文本） |
| `selected` | 默认选中项 |

### 分组显示
```html
<select name="course">
  <optgroup label="计算机类">
    <option value="network">计算机网络</option>
    <option value="web">网页设计</option>
  </optgroup>
  <optgroup label="电子类">
    <option value="circuit">电路基础</option>
  </optgroup>
</select>
```

> **对口升学考点**：textarea=多行文本（有结束标签、可换行）。select=下拉列表、option=选项、selected=默认选中。',
'<textarea>多行文本框（name/rows/cols），与input text的区别：textarea可换行。<select>下拉列表+<option>选项，selected属性默认选中，value属性为提交值。',
5, '单元6 设计制作网页', '任务5 使用表单', 1144,
'textarea vs input text："textarea=多行文本可换行，input text=单行不能换行"\n\nselect记法："select是下拉框，option是里面的选项，selected是默认选中"',
'【必考】①textarea多行文本（rows行数/cols列数）②select下拉列表+option选项 ③selected默认选中 ④value属性是提交值',
1,
'["HTML","textarea","多行文本","select","下拉列表","option","selected","表单"]',
'["HTML网页制作基础","表单"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10320, 10170, '<textarea>标签的作用和属性？', '多行文本输入框。属性：name（名称）、rows（行数高度）、cols（列数宽度）、placeholder（提示文字）。有结束标签</textarea>。', 1, 'DEFINITION'),
(10321, 10170, '<select>和<option>标签如何使用？', '<select>创建下拉列表，name属性指定控件名称。<option>定义选项，value属性指定提交值，selected属性默认选中。<optgroup>可对选项分组。', 2, 'DEFINITION'),
(10322, 10170, '<textarea>和<input type="text">有什么不同？', 'textarea：多行、可换行、有结束标签、rows/cols设大小。input text：单行、不能换行、空标签、默认宽度小。', 3, 'COMPARISON');

-- ============================================================
-- 文章10171: 多媒体标签 audio/video（node=1145）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10171, '多媒体标签——在网页中嵌入音视频',
'## audio标签——嵌入音频

`<audio>`标签在网页中嵌入**音频文件**，提供播放控制。

```html
<audio src="music.mp3" controls>
  您的浏览器不支持音频播放。
</audio>
```

### audio常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| **`src`** | 音频文件路径 | `src="music.mp3"` |
| **`controls`** | 显示播放控件（播放/暂停/音量） | `controls` |
| `autoplay` | 自动播放（页面加载后自动播放） | `autoplay` |
| `loop` | 循环播放 | `loop` |
| `muted` | 静音播放 | `muted` |

### 支持多种格式
```html
<audio controls>
  <source src="music.mp3" type="audio/mpeg">
  <source src="music.ogg" type="audio/ogg">
  您的浏览器不支持音频播放。
</audio>
```
浏览器会自动选择第一个支持的格式播放。

## video标签——嵌入视频

`<video>`标签在网页中嵌入**视频文件**。

```html
<video src="movie.mp4" controls width="600">
  您的浏览器不支持视频播放。
</video>
```

### video常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| **`src`** | 视频文件路径 | `src="movie.mp4"` |
| **`controls`** | 显示播放控件 | `controls` |
| `width`/`height` | 视频播放器宽高 | `width="640" height="360"` |
| `autoplay` | 自动播放 | `autoplay` |
| `loop` | 循环播放 | `loop` |
| `muted` | 静音播放 | `muted` |
| `poster` | 视频封面的图片 | `poster="cover.jpg"` |

### 多格式支持
```html
<video controls width="640">
  <source src="movie.mp4" type="video/mp4">
  <source src="movie.webm" type="video/webm">
  您的浏览器不支持视频播放。
</video>
```

### audio vs video对比

| 对比 | audio | video |
|:----:|:-----:|:-----:|
| 播放内容 | 音频（音乐/语音） | 视频（画面+声音） |
| 视觉 | 仅显示控件 | 显示画面+控件 |
| 特有属性 | — | width、height、poster |

> **必记**：controls属性显示播放控件（必须有才能让用户控制播放）。autoplay自动播放（很多浏览器限制自动播放声音）。两者都不是空标签，有结束标签。',
'<audio>嵌入音频、<video>嵌入视频。核心属性：src（文件路径）、controls（显示控件）、autoplay（自动播放）、loop（循环）。<source>标签提供多种格式供浏览器选择。',
5, '单元6 设计制作网页', '任务5 使用表单', 1145,
'audio/video记法："audio听歌，video看片"\n共同属性："src路径/controls控件/autoplay自动/loop循环"\nvideo独有："width宽/height高/poster封面"',
'【必考】①audio嵌入音频 ②video嵌入视频 ③controls=显示播放控件（必记）④autoplay=自动播放 ⑤src=文件路径',
1,
'["HTML","audio","video","多媒体","音频","视频","controls","autoplay","HTML5"]',
'["HTML网页制作基础","多媒体标签"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10323, 10171, '<audio>和<video>标签各有什么作用？', '<audio>在网页中嵌入音频文件。<video>在网页中嵌入视频文件。两者都是HTML5新增标签，支持controls/autoplay/loop等属性。', 1, 'DEFINITION'),
(10324, 10171, '<audio>和<video>的常用属性有哪些？', 'src（文件路径）、controls（显示控件首选）、autoplay（自动播放）、loop（循环播放）、muted（静音）。video还有width/height（宽高）、poster（封面）。', 2, 'DEFINITION'),
(10325, 10171, '如何让<video>支持多种视频格式？', '使用<source>标签提供多个来源：<video><source src="a.mp4" type="video/mp4"><source src="a.webm" type="video/webm">您的浏览器不支持。</video>。浏览器自动选第一个支持的格式。', 3, 'APPLICATION');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v175] 单元6 知识库文章种子完成：共13篇文章，约39张记忆卡片。' AS result;
-- ============================================================================
-- v176: 网络应用基础 — 知识库文章补充（应会技能补齐 + 难度3扩展）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，补齐应会技能2篇 + 难度3扩展
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 补充1：工作组网络的设置与文件共享（应会技能，原缺失）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10172: 工作组网络的设置与文件共享（node=1108 扩展）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10172, '工作组网络的设置与文件共享——从理论到实操',
'## 工作组回顾

工作组（Workgroup）是Windows网络中的**对等网络模式**，每台计算机自己管理自己的用户账户，适合**10台以下**的小型网络。

> 家庭3台电脑互相传文件，不需要服务器——这就是工作组。

## 实操一：设置工作组名称

要让多台电脑在同一个工作组中，必须设置相同的**工作组名称**（Windows默认是"WORKGROUP"）。

### 操作步骤（以Windows 10/11为例）

| 步骤 | 操作 | 图示说明 |
|:----:|:------|:---------|
| ① | 右键「此电脑」→「属性」 | 打开系统信息 |
| ② | 点击「高级系统设置」 | 左侧导航栏 |
| ③ | 切换到「计算机名」选项卡 | 看到当前计算机名和工作组 |
| ④ | 点击「更改」按钮 | 打开更改窗口 |
| ⑤ | 在「隶属于」下选「工作组」，输入统一名称（如 `SCHOOL-LAB`）| 所有电脑必须保持一致 |
| ⑥ | 点击确定 → 重启电脑 | 设置生效 |

### 验证工作组设置
```cmd
# 方法1：系统属性中查看
# 方法2：命令提示符
net config workstation
```
输出中的"工作站域"就是当前工作组名称。

## 实操二：文件共享设置

工作组中共享文件的核心是设置**共享文件夹**。

### 开启网络发现和文件共享

```cmd
控制面板 → 网络和共享中心 → 高级共享设置 →
  ✅ 启用网络发现
  ✅ 启用文件和打印机共享
```

> 如果网络类型是"公用网络"，需要先切换到"专用网络"才能启用网络发现。

### 共享文件夹步骤

| 步骤 | 操作 |
|:----:|:------|
| ① | 右键要共享的文件夹 → 「属性」 |
| ② | 切换到「共享」选项卡 → 点击「共享」按钮 |
| ③ | 在弹出的窗口中选择要共享的用户（Everyone=所有人） |
| ④ | 设置权限级别：读取（只能看）或读取/写入（可修改） |
| ⑤ | 点击「共享」→ 完成 |

### 在共享文件夹上放文件

```
共享后其他电脑可通过网络访问此文件夹，
路径格式: \\计算机名\共享文件夹名
如: \\PC-01\课件
```

## 实操三：访问共享文件

### 方法1：网络邻居
```
打开文件资源管理器 → 左侧点击「网络」→
双击目标计算机 → 双击共享文件夹
```

### 方法2：直接输入路径
```
按 Win+R 打开运行 → 输入 \\目标电脑名\共享名
例如: \\STUDENT-PC-05\课件
```

### 方法3：映射网络驱动器（常用）
```
右键「网络」中的共享文件夹 → 「映射网络驱动器」
→ 选择驱动器号（如 Z:）→ 完成
```
之后在文件管理器中直接访问 Z: 盘即可。

## 实操四：共享打印机

| 步骤 | 操作 |
|:----:|:------|
| ① | 在连接打印机的电脑上：设置 → 蓝牙和其他设备 → 打印机和扫描仪 |
| ② | 点击要共享的打印机 → 「打印机属性」→ 「共享」选项卡 |
| ③ | 勾选「共享这台打印机」→ 设置共享名称 |
| ④ | 在其他电脑上：设置 → 打印机 → 添加打印机 → 选择共享打印机 |

### 常见问题排查

| 现象 | 可能原因 | 解决方法 |
|:----:|:---------|:---------|
| 网络里看不到其他电脑 | 网络发现未开启 | 控制面板→高级共享设置→启用网络发现 |
| 访问共享提示"无权限" | 权限设置不对 | 检查共享权限，Everyone添加读取 |
| 找不到共享打印机 | 防火墙拦截 | 检查防火墙是否阻止了文件和打印机共享 |
| 工作组名称不同 | 未统一设置 | 所有电脑改为同一工作组名 |

> **对口升学必考**：工作组的设置核心是"同一工作组名称+启用网络发现+共享文件夹"。会考操作步骤的判断和排序。',
'工作组网络的实操步骤：统一工作组名→启用网络发现→设置共享文件夹→访问共享资源（网络邻居/直接路径/映射驱动器）。文件共享路径格式：\\计算机名\共享名。',
5, '单元3 管理局域网', '任务1 使用网络操作系统', 1108,
'工作组实操三步走："统一组名→启用发现→共享文件夹"\n\n访问共享三种方法："网络邻居点、直接路径\\、映射驱动器Z:"\n\n路径格式："\\计算机名\共享文件夹名"',
'【必考】①工作组要求所有电脑同名 ②网络发现必须开启 ③共享文件夹权限（读取vs读取/写入）④访问共享的三种方式 ⑤映射网络驱动器',
2,
'["工作组","文件共享","网络邻居","映射驱动器","网络发现","共享文件夹","打印机共享"]',
'["网络操作系统","常见网络命令"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10326, 10172, '工作组中设置文件共享的步骤是什么？', '①统一工作组名（系统属性→更改）②开启网络发现（控制面板→高级共享设置）③右键文件夹→属性→共享→选择Everyone→设置权限→完成共享。', 1, 'PROCEDURE'),
(10327, 10172, '访问网络共享文件的三种方法是什么？', '①网络邻居：文件资源管理器→网络→双击电脑②运行路径：Win+R→输入\\计算机名\共享名③映射驱动器：右键共享文件夹→映射网络驱动器→选盘符。', 2, 'PROCEDURE'),
(10328, 10172, '工作组中访问共享提示"无权限"如何解决？', '在共享文件夹所在电脑上：右键文件夹→属性→共享→重新设置权限，将Everyone添加并至少勾选"读取"权限。如果仍不行，检查防火墙是否拦截。', 3, 'SCENARIO'),
(10329, 10172, 'UNC路径格式是什么？', 'UNC（Universal Naming Convention）路径格式为"\\计算机名\共享文件夹名"，用于在Windows网络中定位共享资源。如\\PC-01\课件。', 4, 'DEFINITION');

-- ============================================================
-- 文章10173: 网页保存与浏览器预览（node=1121 扩展）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10173, '网页保存与浏览器预览——从代码到网页',
'## 为什么要保存网页和预览？

在学习和制作网页时，经常需要：
- **保存网页内容**供离线查阅
- **在浏览器中预览**自己编写的HTML文件

## 实操一：在浏览器中预览HTML文件

### 方法1：直接拖拽到浏览器
```
在文件管理器中找到 HTML 文件
→ 直接拖拽到浏览器窗口
→ 浏览器立即显示网页内容
```

### 方法2：文件菜单打开（Ctrl+O）
| 步骤 | 操作 |
|:----:|:------|
| ① | 打开浏览器（Chrome/Edge/Firefox） |
| ② | 按 `Ctrl+O` 或在地址栏输入 `file:///` |
| ③ | 在弹出的文件选择框中找到你的 HTML 文件 |
| ④ | 双击打开，浏览器显示渲染后的网页 |

### 方法3：右键 → 打开方式
```
右键 HTML 文件 → 打开方式 → 选择浏览器
（Chrome / Edge / Firefox）
```

### 方法4：命令行快速打开
```cmd
# 在HTML文件所在目录下
start index.html
```
Windows会自动用默认浏览器打开。

> **选择浏览器的建议**：建议使用Chrome或Edge预览，它们的开发者工具（F12）对学习HTML很有帮助。

## 实操二：网页保存方法

### 保存整个网页到本地

在浏览器中打开目标网页后：

| 方法 | 操作 |
|:----:|:------|
| 快捷键 | `Ctrl+S`（或 Mac: Command+S） |
| 菜单 | 浏览器菜单 → 另存为/保存页面 |
| 右键 | 在页面空白处右键 → 另存为 |

### 保存类型（四种）

| 保存类型 | 说明 | 适用场景 |
|:--------:|:------|:---------|
| **"网页，全部"（.htm + 文件夹）** | 保存HTML+所有资源（图片/CSS/JS） | 需要完整离线查看时 |
| **"网页，仅HTML"（.htm）** | 只保存HTML结构，无资源 | 学习网页结构时 |
| **"文本文件"（.txt）** | 只保存网页中的纯文字 | 只需要文字内容时 |
| **"单个文件"（.mht）** | 所有内容打包在一个文件中 | 便于分享和传输 |
```html
<!-- 保存类型对比表 -->
网页，全部：保存最完整，但会产生一个文件夹 + 一个html文件
网页，仅HTML：只保留骨架，图片和样式会丢失
文本文件：只保留文字，完全丢失格式
```

### 保存网页中的图片

| 方法 | 操作 |
|:----:|:------|
| 右键保存 | 右键图片 → 图片另存为 → 选择保存位置 |
| 拖拽保存 | 从浏览器拖拽图片到文件夹中 |
| 复制粘贴 | 右键图片 → 复制 → 粘贴到文件夹或文档中 |

### 保存网页中的部分内容

```
选中想要的文字 → 右键 → 复制（Ctrl+C）
→ 粘贴到 Word/记事本（Ctrl+V）
```

## 实操三：查看网页源代码

### 目的
学习他人的网页是如何用HTML/CSS编写的——这是学习前端最有效的方法之一。

### 三种查看方式

| 方式 | 操作 | 用途 |
|:----:|:------|:------|
| 查看源代码 | 右键网页空白处 → 「查看网页源代码」(Ctrl+U) | 看原始HTML代码 |
| 检查元素 | 右键 → 「检查」(F12) → Elements面板 | 实时查看和修改HTML/CSS |
| 查看样式 | F12 → Styles面板 | 看某个元素的完整CSS样式 |

### 实操示例
> 打开任意网页 → Ctrl+U 查看源代码 → 你会发现网页的结构正是由`<html>`、`<head>`、`<body>`等标签组成的——和课本上学的HTML基本结构完全一致。

> **对口升学考点**：Ctrl+S保存网页、Ctrl+O打开文件、F12开发者工具、右键查看源代码。预览HTML文件可通过拖拽或Ctrl+O。',
'浏览器预览HTML文件：拖拽/Ctrl+O/右键打开。保存网页：Ctrl+S（四种类型：全部/仅HTML/文本/单文件）。查看源代码：Ctrl+U或F12检查元素。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1121,
'预览HTML："拖拽法、双Ctrl（Ctrl+O打开、Ctrl+S保存）、右键选浏览器"\n\n保存类型："全部=完整版，仅HTML=骨架版，文本=纯文字，单文件=打包版"\n\n看源码："Ctrl+U看源码，F12检查元素"',
'【必考】①Ctrl+S保存网页 ②Ctrl+O在浏览器中打开文件 ③预览HTML的三种方法 ④保存四种类型的区别 ⑤F12开发者工具 ⑥Ctrl+U查看源代码',
1,
'["浏览器","网页保存","预览HTML","Ctrl+S","F12开发者工具","查看源代码","离线浏览"]',
'["Internet基础","浏览器","HTML网页制作基础"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10330, 10173, '如何预览本地的HTML文件？', '四种方法：①拖拽到浏览器②Ctrl+O选择文件③右键→打开方式→选浏览器④命令行start index.html。推荐使用Chrome/Edge预览。', 1, 'PROCEDURE'),
(10331, 10173, '保存网页有哪四种类型？', '①网页全部（.htm+文件夹，最完整）②仅HTML（仅骨架）③文本文件（纯文字）④单个文件（.mht打包）。学习HTML建议用"仅HTML"。', 2, 'COMPARISON'),
(10332, 10173, '如何查看网页的HTML源代码？有什么学习价值？', 'Ctrl+U查看源代码、F12开发者工具→Elements面板。学习价值：能看到别人网页的实际HTML结构，是学习前端最有效的方法之一。', 3, 'APPLICATION'),
(10333, 10173, 'F12开发者工具在网页学习中有什么用？', 'F12打开开发者工具：Elements面板实时查看和修改HTML/CSS、Console看错误信息、Network看网络请求。学习网页制作时经常使用。', 4, 'APPLICATION');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v176] 知识库文章补充完成：共2篇应会文章，约8张记忆卡片。' AS result;
-- ============================================================================
-- v181: 语文[职高] 知识库空白修复
-- 修复内容:
--   1. 4个新L3节点补充概述内容（标点符号/修辞手法辨析/古诗词鉴赏/文学常识与名句默写）
--   2. 清理病句辨析下残留的旧修辞L4节点（4个，已迁移至修辞手法辨析）
--   3. 清理应用文/话题作文的重复L4节点（4个）
-- ============================================================================

-- 语文[职高] subject_id = 20 (硬编码避免字符集问题)
SET @yy_zg_sid = 20;

-- ============================================================================
-- Part 1: 补充 4 个新 L3 概述内容
-- ============================================================================

-- 1a. 标点符号（9种标点用法辨析）
UPDATE knowledge_nodes SET content = '【一句话定义】\n**标点符号**是书面语言的辅助工具，用于表示停顿、语气、词语性质和修辞功能。四川省对口高考主要考查**顿号、逗号、分号、问号、引号、省略号、破折号、书名号、连接号与间隔号**9种常用标点的规范使用。\n\n【考查范围】\n① 顿号与逗号的层级区分 ② 分号的并列分句用法 ③ 问号的选择问与连续问 ④ 引号的双引单引嵌套 ⑤ 省略号的六点规范 ⑥ 破折号的解释与转折功能 ⑦ 书名号的使用范围与嵌套 ⑧ 连接号与间隔号的区分 ⑨ 多标点组合综合辨析\n\n【常见错误】\n1. 选择问句中间误用问号——\"你是学计算机专业？还是学会计专业？\"应在句末用一个问号。2. 书名号滥用——活动名称、课程名称不应加书名号。3. 顿号与逗号层级混淆——大并列用逗号，小并列用顿号。\n\n【出题方向】\n选择题第4-5题常考标点符号使用正误判断，四个选项中各有一个标点使用错误，要求选出全对或全错的一项。', updated_at = NOW()
WHERE id = 3650 AND subject_id = @yy_zg_sid AND level = 3;

-- 1b. 修辞手法辨析（8种修辞格+易混辨析）
UPDATE knowledge_nodes SET content = '【一句话定义】\n**修辞手法**是为增强语言表达效果而采用的修饰方法，四川省对口高考重点考查**比喻、比拟、借代、夸张、对偶、排比、反问、设问**8种修辞格的辨识与作用分析，以及易混修辞的辨析。\n\n【考查范围】\n① 比喻（明喻/暗喻/借喻）② 比拟（拟人+拟物）③ 借代（特征代本体/部分代整体）④ 夸张（扩大/缩小）⑤ 对偶（正对/反对）⑥ 排比（三个及以上结构相似）⑦ 反问（无疑而问，答在问中）⑧ 设问（自问自答）⑨ 易混辨析：借代vs借喻、比喻vs比拟、对偶vs排比、设问vs反问\n\n【常见错误】\n1. 借喻和借代混淆——借喻基于相似性（可改为明喻），借代基于相关性（不能改为明喻）。2. 有\"像\"不一定是比喻——\"他长得像他爸爸\"是同类相比，不是比喻。3. 排比和对偶混淆——排比是三项及以上，对偶是两项对称。\n\n【出题方向】\n选择题考查修辞辨识（给出句子判断修辞类型），阅读简答题考查修辞效果分析（\"这句话用了什么修辞手法？有什么表达效果？\"）。', updated_at = NOW()
WHERE id = 3651 AND subject_id = @yy_zg_sid AND level = 3;

-- 1c. 古诗词鉴赏（7个维度）
UPDATE knowledge_nodes SET content = '【一句话定义】\n**古诗词鉴赏**是四川省对口高考文言文阅读的重要组成部分，指定篇目包括《诗经》选篇、唐诗《将进酒》《茅屋为秋风所破歌》、宋词《念奴娇·赤壁怀古》《雨霖铃》，考查意象分析、表达技巧、思想情感和语言赏析四个维度。\n\n【考查范围】\n① 指定篇目精读：关雎/蒹葭/将进酒/茅屋为秋风所破歌/念奴娇·赤壁怀古/雨霖铃 ② 诗歌意象与意境分析 ③ 表达技巧鉴赏（抒情方式/描写手法/修辞）④ 思想情感与观点态度评价 ⑤ 语言赏析（炼字/诗眼/风格）\n\n【常见错误】\n1. 用现代汉语理解古诗词语——\"停车坐爱枫林晚\"的\"坐\"是\"因为\"不是\"坐下\"。2. 混淆表现手法和修辞手法——借景抒情是表现手法，比喻拟人是修辞手法，两者分析层次不同。3. 脱离背景理解情感——鉴赏必须结合作者生平和时代背景。\n\n【出题方向】\n简答题考查诗歌意象分析和表达技巧鉴赏，选择题考查诗句理解和情感把握。名句默写题也常考这些篇目。', updated_at = NOW()
WHERE id = 3648 AND subject_id = @yy_zg_sid AND level = 3;

-- 1d. 文学常识与名句默写（14篇必背）
UPDATE knowledge_nodes SET content = '【一句话定义】\n**文学常识与名句默写**是四川省对口高考的必考内容，涵盖**14篇必背名句名篇**（先秦至唐宋诗文+现代诗歌）以及文学体裁、重要作家作品和文化常识三大知识板块。\n\n【考查范围】\n① 必背篇目14篇：静女/采薇/侍坐/寡人之于国/劝学/师说/将进酒/琵琶行/念奴娇/雨霖铃/六国论/我爱这土地/雨巷/致橡树 ② 文学体裁常识（诗歌/散文/小说/戏剧四类）③ 重要作家作品（孔子/孟子/荀子/庄子/李白/杜甫/白居易/苏轼/柳永/鲁迅/朱自清/艾青/莫泊桑等）④ 文化常识（称谓/历法/官职/科举/地理）\n\n【常见错误】\n1. 默写时写错别字——\"学而时习之，不亦说乎\"的\"说\"不写\"悦\"（通假字保留原字）。2. 作家朝代和字号记混——韩愈是唐代不是宋代，柳永原名柳三变。3. 文学作品归属张冠李戴——孔乙己是鲁迅的，骆驼祥子是老舍的。\n\n【出题方向】\n默写题4-6分（给上句写下句或给下句写上句），选择题考查文学常识正误判断。', updated_at = NOW()
WHERE id = 3649 AND subject_id = @yy_zg_sid AND level = 3;

-- ============================================================================
-- Part 2: 清理病句辨析下残留的旧修辞 L4 节点
-- 这些节点已迁移至「修辞手法辨析」L3 下（比喻/比拟/借代/夸张/对偶/排比/反问/设问/易混辨析）
-- 旧节点: 比喻与借代(3022)/拟人与夸张(3023)/排比与对偶(3024)/设问、反问与反复(3025)
-- ============================================================================

-- 先检查是否有题库引用
SELECT CONCAT('v181 清理检查: ') AS msg;

SELECT CONCAT('  残留修辞节点题库引用: ', COUNT(*)) AS result
FROM question_bank WHERE category_id IN (3022, 3023, 3024, 3025) AND status != -1;

-- 清理残留修辞L4节点（先移走关联的题库题目到修辞手法辨析L3，再删除）
-- 注意：如果题库有引用，只做标记不删除，需要人工处理
UPDATE IGNORE question_bank SET category_id = 3651 WHERE category_id IN (3022, 3023, 3024, 3025) AND status != -1;
DELETE FROM knowledge_nodes WHERE id IN (3022, 3023, 3024, 3025) AND subject_id = @yy_zg_sid AND level = 4;

-- ============================================================================
-- Part 3: 清理应用文/话题作文重复 L4 节点
-- 应用文: 便条(3057) 已更名为 便条（请假条/留言条/托事条）
--         单据(3058) 已更名为 单据（借条/收条/领条/欠条）
-- 话题作文: 审题立意(3050) → 审题与立意
--           语言表达技巧(3054) → 作文语言提升（句式变化/修辞润色）
-- ============================================================================

-- 3a. 迁移题库引用到新节点
-- 便条(3057) → 便条（请假条/留言条/托事条）
SET @new_bt = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 4 AND name = '便条（请假条/留言条/托事条）' LIMIT 1);
UPDATE IGNORE question_bank SET category_id = @new_bt WHERE category_id = 3057;

-- 单据(3058) → 单据（借条/收条/领条/欠条）
SET @new_dj = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 4 AND name = '单据（借条/收条/领条/欠条）' LIMIT 1);
UPDATE IGNORE question_bank SET category_id = @new_dj WHERE category_id = 3058;

-- 审题立意(3050) → 审题与立意
SET @new_st = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 4 AND name = '审题与立意' LIMIT 1);
UPDATE IGNORE question_bank SET category_id = @new_st WHERE category_id = 3050;

-- 语言表达技巧(3054) → 作文语言提升（句式变化/修辞润色）
SET @new_yy = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 4 AND name = '作文语言提升（句式变化/修辞润色）' LIMIT 1);
UPDATE IGNORE question_bank SET category_id = @new_yy WHERE category_id = 3054;

-- 3b. 删除重复节点
DELETE FROM knowledge_nodes WHERE id IN (3057, 3058, 3050, 3054) AND subject_id = @yy_zg_sid AND level = 4;

-- ============================================================================
-- 验证
-- ============================================================================
SELECT CONCAT('v181: 修复完成！') AS result;

-- 验证 L3 内容填充
SELECT CONCAT('L3 无内容节点: ', COUNT(*)) AS result
FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 3 AND (content IS NULL OR content = '');

-- 验证 L4 总数
SELECT CONCAT('L4 节点总数: ', COUNT(*)) AS result
FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 4;

-- 验证重复节点已清理
SELECT CONCAT('残留重复节点: ', COUNT(*)) AS result
FROM knowledge_nodes WHERE id IN (3022,3023,3024,3025,3057,3058,3050,3054);
-- ============================================================================
-- v182: 语文[职高] 知识节点 → 知识文章迁移
-- 将 knowledge_nodes.content 迁移到 knowledge_articles
-- knowledge_articles 用于前端发现页（KnowledgeDiscover.vue）
-- ============================================================================

-- 语文[职高] subject_id = 20

-- Step 1: 删除旧的语文文章（如果有）
DELETE FROM knowledge_articles WHERE subject_id = 20;

-- Step 2: 为每个 L4 节点创建文章
-- 使用自连接查询找到 L3(任务) 和 L2(章节) 祖先名称

INSERT INTO knowledge_articles (title, content_md, excerpt, subject_id, chapter, task, difficulty, status, view_count, created_at, updated_at)
SELECT
    l4.name AS title,
    l4.content AS content_md,
    CONCAT(LEFT(COALESCE(l4.content, ''), 200), CASE WHEN CHAR_LENGTH(COALESCE(l4.content, '')) > 200 THEN '...' ELSE '' END) AS excerpt,
    20 AS subject_id,
    COALESCE(l2.name, '未分类') AS chapter,
    COALESCE(l3.name, '未分类') AS task,
    1 AS difficulty,
    'PUBLISHED' AS status,
    0 AS view_count,
    NOW() AS created_at,
    NOW() AS updated_at
FROM knowledge_nodes l4
LEFT JOIN knowledge_nodes l3 ON l4.parent_id = l3.id AND l3.level = 3
LEFT JOIN knowledge_nodes l2 ON l3.parent_id = l2.id AND l2.level = 2
WHERE l4.subject_id = 20
  AND l4.level = 4
  AND l4.content IS NOT NULL
  AND l4.content != '';

-- Step 3: 验证
SELECT CONCAT('v182: 迁移完成！') AS result;
SELECT CONCAT('knowledge_articles 语文文章数: ', COUNT(*)) AS result FROM knowledge_articles WHERE subject_id = 20;
SELECT CONCAT('knowledge_nodes 语文L4节点数: ', COUNT(*)) AS result FROM knowledge_nodes WHERE subject_id = 20 AND level = 4;

-- ========================================
-- 演示数据（首次部署后请修改默认密码）
-- ========================================

-- 管理员账号（密码: admin123）
INSERT INTO `users` VALUES (1,'admin','$2a$10$evaXwHHIA/Y7sdchVHbxkOSE60XGzsZzGqZ2jOSOFhlJ6C81pFGs.','系统管理员',NULL,NULL,NULL,25,'SUPER_ADMIN',1,NULL,NULL,'2026-05-01 06:29:04','2026-05-14 10:57:47',1,4,NULL,NULL,4,0);

-- 演示班级
INSERT INTO `classes` VALUES (1,'演示班','demo','2024级','计算机','2025-2026','下',1,1,1,'2026-05-21 12:06:24','2026-05-22 07:00:13',1,4,'vocational');

-- 演示教师（用户名: demo_teacher, 密码: 123456）
INSERT INTO `teachers` VALUES (10,'Js001','演示教师',1,1,'2026-05-21 12:06:08','2026-05-22 07:00:13');
INSERT INTO `teacher_classes` VALUES (1,10,1,'信息技术应用基础','2026-05-21 12:31:41');

-- 演示学生（用户名: demo_student, 密码: 123456）
INSERT INTO `students` VALUES (199,'demo_student','演示学生',1,1,'2026-05-21 12:07:39','2026-05-22 13:56:42',NULL,NULL,1,1,4);
INSERT INTO `class_students` VALUES (1,199,1,'2026-05-21 12:07:39');


