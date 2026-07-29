-- ============================================================
-- V000: Baseline schema extracted from init.sql
-- Generated: 2026-07-07 13:50:36
-- DDL only. Seed data remains in database/ directory.
-- ============================================================

-- ============================================================================
-- 教学管理系统 数据库初始化 (Docker自动执行)
-- 生成时间: 2026-05-25 13:39:51
-- ============================================================================
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: teaching_system
-- ------------------------------------------------------
-- Server version	8.0.46


--
-- Table structure for table `achievement_definitions`
--

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

--
-- Dumping data for table `achievement_definitions`
--


--
-- Table structure for table `ai_call_log`
--

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

--
-- Dumping data for table `ai_call_log`
--


--
-- Table structure for table `ai_outputs`
--

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

--
-- Dumping data for table `ai_outputs`
--


--
-- Table structure for table `alert_last_scan`
--

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

--
-- Dumping data for table `alert_last_scan`
--


--
-- Table structure for table `alert_records`
--

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

--
-- Dumping data for table `alert_records`
--


--
-- Table structure for table `alert_rules`
--

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

--
-- Dumping data for table `alert_rules`
--


--
-- Table structure for table `audit_log`
--

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

--
-- Dumping data for table `audit_log`
--


--
-- Table structure for table `bbs_bookmarks`
--

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

--
-- Dumping data for table `bbs_bookmarks`
--


--
-- Table structure for table `bbs_categories`
--

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

--
-- Dumping data for table `bbs_categories`
--


--
-- Table structure for table `bbs_likes`
--

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

--
-- Dumping data for table `bbs_likes`
--


--
-- Table structure for table `bbs_muted_users`
--

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

--
-- Dumping data for table `bbs_muted_users`
--


--
-- Table structure for table `bbs_posts`
--

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

--
-- Dumping data for table `bbs_posts`
--


--
-- Table structure for table `bbs_replies`
--

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

--
-- Dumping data for table `bbs_replies`
--


--
-- Table structure for table `class_album`
--

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

--
-- Dumping data for table `class_album`
--


--
-- Table structure for table `class_album_comments`
--

CREATE TABLE `class_album_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `photo_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_photo` (`photo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级相册评论';

--
-- Dumping data for table `class_album_comments`
--


--
-- Table structure for table `class_album_likes`
--

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

--
-- Dumping data for table `class_album_likes`
--


--
-- Table structure for table `class_students`
--

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

--
-- Dumping data for table `class_students`
--


--
-- Table structure for table `class_teachers`
--

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

--
-- Dumping data for table `class_teachers`
--


--
-- Table structure for table `class_type_config`
--

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

--
-- Dumping data for table `class_type_config`
--


--
-- Table structure for table `classes`
--

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

--
-- Dumping data for table `classes`
--


--
-- Table structure for table `classroom_participations`
--

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

--
-- Dumping data for table `classroom_participations`
--


--
-- Table structure for table `classroom_questions`
--

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

--
-- Dumping data for table `classroom_questions`
--


--
-- Table structure for table `classroom_sessions`
--

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

--
-- Dumping data for table `classroom_sessions`
--


--
-- Table structure for table `credit_redemption_orders`
--

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

--
-- Dumping data for table `credit_redemption_orders`
--


--
-- Table structure for table `credit_rules`
--

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

--
-- Dumping data for table `credit_rules`
--


--
-- Table structure for table `credit_shop_items`
--

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

--
-- Dumping data for table `credit_shop_items`
--


--
-- Table structure for table `credit_term_snapshots`
--

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

--
-- Dumping data for table `credit_term_snapshots`
--


--
-- Table structure for table `credit_transactions`
--

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

--
-- Dumping data for table `credit_transactions`
--


--
-- Table structure for table `dict_class_status`
--

CREATE TABLE `dict_class_status` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '状态ID',
  `status_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态代码',
  `status_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `status_code` (`status_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级状态字典';

--
-- Dumping data for table `dict_class_status`
--


--
-- Table structure for table `dict_grade`
--

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

--
-- Dumping data for table `dict_grade`
--


--
-- Table structure for table `dict_major`
--

CREATE TABLE `dict_major` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `major_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业名称',
  `sort_order` int DEFAULT '0' COMMENT '排序，越小越靠前',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业字典';

--
-- Dumping data for table `dict_major`
--


--
-- Table structure for table `dict_question_type`
--

CREATE TABLE `dict_question_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `type_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型代码',
  `type_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
  `is_selectable` tinyint DEFAULT '1' COMMENT '是否可选(考试时)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `type_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目类型字典';

--
-- Dumping data for table `dict_question_type`
--


--
-- Table structure for table `dict_subject`
--

CREATE TABLE `dict_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学科名称',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '0=禁用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学科字典';

--
-- Dumping data for table `dict_subject`
--


--
-- Table structure for table `dict_user_role`
--

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

--
-- Dumping data for table `dict_user_role`
--


--
-- Table structure for table `document_template`
--

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

--
-- Dumping data for table `document_template`
--


--
-- Table structure for table `drafts`
--

CREATE TABLE `drafts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `content` json DEFAULT NULL COMMENT '草稿内容JSON',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_task` (`student_id`,`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生作答草稿';

--
-- Dumping data for table `drafts`
--


--
-- Table structure for table `edu_stage_config`
--

CREATE TABLE `edu_stage_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL DEFAULT '1',
  `capability_key` varchar(50) NOT NULL,
  `enabled` tinyint DEFAULT '0',
  `config_json` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_cap` (`school_id`,`capability_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `edu_stage_config`
--


--
-- Table structure for table `exam_shares`
--

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

--
-- Dumping data for table `exam_shares`
--


--
-- Table structure for table `exam_syllabus`
--

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

--
-- Dumping data for table `exam_syllabus`
--


--
-- Table structure for table `exam_syllabus_node_relation`
--

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

--
-- Dumping data for table `exam_syllabus_node_relation`
--


--
-- Table structure for table `external_review`
--

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

--
-- Dumping data for table `external_review`
--


--
-- Table structure for table `group_member`
--

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

--
-- Dumping data for table `group_member`
--


--
-- Table structure for table `group_members`
--

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

--
-- Dumping data for table `group_members`
--


--
-- Table structure for table `jwt_blacklist`
--

CREATE TABLE `jwt_blacklist` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `jti` varchar(128) NOT NULL COMMENT 'JWT Token ID (jti)',
  `expires_at` datetime NOT NULL COMMENT '黑名单过期时间（token原exp时间）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (`id`),
  KEY `idx_jti` (`jti`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='JWT黑名单（Redis回退/降级存储）';

--
-- Dumping data for table `jwt_blacklist`
--


--
-- Table structure for table `knowledge_nodes`
--

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

--
-- Dumping data for table `knowledge_nodes`
--


--
-- Table structure for table `late_submit_requests`
--

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

--
-- Dumping data for table `late_submit_requests`
--


--
-- Table structure for table `lesson_prep_group`
--

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

--
-- Dumping data for table `lesson_prep_group`
--


--
-- Table structure for table `notifications`
--

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

--
-- Dumping data for table `notifications`
--


--
-- Table structure for table `parent_child_relations`
--

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

--
-- Dumping data for table `parent_child_relations`
--


--
-- Table structure for table `peer_review`
--

CREATE TABLE `peer_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `submission_id` bigint NOT NULL,
  `score_json` json DEFAULT NULL,
  `submitted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `peer_review`
--


--
-- Table structure for table `practice_session_items`
--

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

--
-- Dumping data for table `practice_session_items`
--


--
-- Table structure for table `practice_sessions`
--

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

--
-- Dumping data for table `practice_sessions`
--


--
-- Table structure for table `practice_step_files`
--

CREATE TABLE `practice_step_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `step_id` bigint NOT NULL COMMENT '关联步骤ID',
  `file_url` varchar(500) NOT NULL COMMENT '文件URL',
  `original_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  PRIMARY KEY (`id`),
  KEY `idx_step_id` (`step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训步骤附件';

--
-- Dumping data for table `practice_step_files`
--


--
-- Table structure for table `practice_step_grades`
--

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

--
-- Dumping data for table `practice_step_grades`
--


--
-- Table structure for table `practice_step_images`
--

CREATE TABLE `practice_step_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `step_id` bigint NOT NULL COMMENT '关联步骤ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `order_index` int NOT NULL DEFAULT '0' COMMENT '排序序号',
  PRIMARY KEY (`id`),
  KEY `idx_step_id` (`step_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训步骤配图';

--
-- Dumping data for table `practice_step_images`
--


--
-- Table structure for table `practice_steps`
--

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

--
-- Dumping data for table `practice_steps`
--


--
-- Table structure for table `practice_submissions`
--

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

--
-- Dumping data for table `practice_submissions`
--


--
-- Table structure for table `question_bank`
--

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

--
-- Dumping data for table `question_bank`
--


--
-- Table structure for table `re_review_request`
--

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

--
-- Dumping data for table `re_review_request`
--


--
-- Table structure for table `redeem_deliveries`
--

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

--
-- Dumping data for table `redeem_deliveries`
--


--
-- Table structure for table `redemption_codes`
--

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

--
-- Dumping data for table `redemption_codes`
--


--
-- Table structure for table `rubric`
--

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

--
-- Dumping data for table `rubric`
--


--
-- Table structure for table `rubric_dimension`
--

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

--
-- Dumping data for table `rubric_dimension`
--


--
-- Table structure for table `school_stages`
--

CREATE TABLE `school_stages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL,
  `stage_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_stage` (`school_id`,`stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校学段关联';

--
-- Dumping data for table `school_stages`
--


--
-- Table structure for table `school_term`
--

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

--
-- Dumping data for table `school_term`
--


--
-- Table structure for table `schools`
--

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

--
-- Dumping data for table `schools`
--


--
-- Table structure for table `shedlock`
--

CREATE TABLE `shedlock` (
  `name` varchar(64) NOT NULL,
  `lock_until` timestamp(3) NOT NULL,
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(255) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ShedLock 分布式定时任务锁';

--
-- Dumping data for table `shedlock`
--


--
-- Table structure for table `showcase_comments`
--

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

--
-- Dumping data for table `showcase_comments`
--


--
-- Table structure for table `showcase_works`
--

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

--
-- Dumping data for table `showcase_works`
--


--
-- Table structure for table `sign_records`
--

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

--
-- Dumping data for table `sign_records`
--


--
-- Table structure for table `stages`
--

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

--
-- Dumping data for table `stages`
--


--
-- Table structure for table `student_achievements`
--

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

--
-- Dumping data for table `student_achievements`
--


--
-- Table structure for table `student_answers`
--

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

--
-- Dumping data for table `student_answers`
--


--
-- Table structure for table `student_class_history`
--

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

--
-- Dumping data for table `student_class_history`
--


--
-- Table structure for table `student_groups`
--

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

--
-- Dumping data for table `student_groups`
--


--
-- Table structure for table `student_remarks`
--

CREATE TABLE `student_remarks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `semester` varchar(20) NOT NULL,
  `remark` text,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_semester` (`student_id`,`semester`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班主任寄语';

--
-- Dumping data for table `student_remarks`
--


--
-- Table structure for table `student_stage_change_log`
--

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

--
-- Dumping data for table `student_stage_change_log`
--


--
-- Table structure for table `student_timeline`
--

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

--
-- Dumping data for table `student_timeline`
--


--
-- Table structure for table `students`
--

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

--
-- Dumping data for table `students`
--


--
-- Table structure for table `subject_category`
--

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

--
-- Dumping data for table `subject_category`
--


--
-- Table structure for table `survey_questions`
--

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

--
-- Dumping data for table `survey_questions`
--


--
-- Table structure for table `system_configs`
--

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

--
-- Dumping data for table `system_configs`
--


--
-- Table structure for table `system_settings`
--

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

--
-- Dumping data for table `system_settings`
--


--
-- Table structure for table `task_group_visibility`
--

CREATE TABLE `task_group_visibility` (
  `task_id` bigint NOT NULL COMMENT '任务ID → tasks.id',
  `group_id` bigint NOT NULL COMMENT '分组ID → student_groups.id',
  PRIMARY KEY (`task_id`,`group_id`),
  KEY `idx_tgv_task_id` (`task_id`),
  KEY `idx_tgv_group_id` (`group_id`),
  CONSTRAINT `fk_tgv_group` FOREIGN KEY (`group_id`) REFERENCES `student_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tgv_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务分组可见性关联表';

--
-- Dumping data for table `task_group_visibility`
--


--
-- Table structure for table `task_questions`
--

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

--
-- Dumping data for table `task_questions`
--


--
-- Table structure for table `task_submissions`
--

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

--
-- Dumping data for table `task_submissions`
--


--
-- Table structure for table `task_templates`
--

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

--
-- Dumping data for table `task_templates`
--


--
-- Table structure for table `tasks`
--

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

--
-- Dumping data for table `tasks`
--


--
-- Table structure for table `teacher_classes`
--

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

--
-- Dumping data for table `teacher_classes`
--


--
-- Table structure for table `teacher_quick_comments`
--

CREATE TABLE `teacher_quick_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师ID→teachers.id',
  `comment_text` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '快捷评语内容',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师快捷评语';

--
-- Dumping data for table `teacher_quick_comments`
--


--
-- Table structure for table `teachers`
--

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

--
-- Dumping data for table `teachers`
--


--
-- Table structure for table `teaching_group`
--

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

--
-- Dumping data for table `teaching_group`
--


--
-- Table structure for table `title_levels`
--

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

--
-- Dumping data for table `title_levels`
--


--
-- Table structure for table `typing_competition_results`
--

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

--
-- Dumping data for table `typing_competition_results`
--


--
-- Table structure for table `typing_competitions`
--

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

--
-- Dumping data for table `typing_competitions`
--


--
-- Table structure for table `typing_levels`
--

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

--
-- Dumping data for table `typing_levels`
--


--
-- Table structure for table `typing_records`
--

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

--
-- Dumping data for table `typing_records`
--


--
-- Table structure for table `typing_texts`
--

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

--
-- Dumping data for table `typing_texts`
--


--
-- Table structure for table `users`
--

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

--
-- Dumping data for table `users`
--

-- ⚠️ 内置管理员账号 admin — 密码已自定义（非 admin123）
--    - id=1 是系统初始管理员，很多维护操作依赖此账号
--    - 密码通过 /api/profile/password 自行修改，或直接操作数据库
--    - 如修改密码，必须同时更新此 SQL 文件 + CLAUDE.md + fix_admin_pwd.sql 中的密码记录
--    - 生成时间: 2026-06-07 · 密码: BrightDawn492#

--
-- Table structure for table `wrong_questions`
--

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

--
-- Dumping data for table `wrong_questions`
--


--
-- Dumping events for database 'teaching_system'
--

--
-- Dumping routines for database 'teaching_system'
--



--
-- Create application user
--
CREATE USER IF NOT EXISTS 'teaching_app'@'%' IDENTIFIED BY 'kUwb6in6sOvW23mXY1Irk6qUngh9vPZ8';
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

-- ---------- v69_parent_feedback_feature_flag.sql ----------
-- v69: 家长反馈汇总功能开关
-- 默认关闭，第四期真数据上线后开启


-- ---------- v70_drop_parent_student_bind.sql ----------
-- v70: 清理废弃的 parent_student_bind 表
-- parent_child_relations (v55) 已替代其功能

-- 迁移 parent_student_bind 中尚不存在于 parent_child_relations 的记录
-- parent_student_bind.student_user_id → students.user_id → students.id → parent_child_relations.student_id

-- 获取刚创建的章节ID


-- ============================================================================
-- 普高英语[全国考纲] (subject_id=@en_pg, root=@en_pg_root)
-- ============================================================================



-- ============================================================================
-- 职高语文[四川省对口升学] (subject_id=@yy_zg, root=@yy_zg_root)
-- ============================================================================



-- ============================================================================
-- 职高英语[四川省对口升学] (subject_id=@en_zg, root=@en_zg_root)
-- ============================================================================



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


-- ============================================================================
-- 普高语文 — 全国高考语文大纲
-- ============================================================================

-- ============================================================================
-- 普高英语 — 全国高考英语大纲
-- ============================================================================

-- ============================================================================
-- 职高语文 — 四川省对口升学语文大纲
-- ============================================================================

-- ============================================================================
-- 职高英语 — 四川省对口升学英语大纲
-- ============================================================================

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


-- 语文职高作文


-- 英语普高写作


-- 英语职高写作


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



SELECT 'v78: 应用文写作知识点补充完成！' AS result;
SELECT CONCAT('应用文写作下共 ', COUNT(*), ' 个知识点') FROM knowledge_nodes WHERE parent_id = @app_writing_id;

-- ---------- v79_practical_writing_rubric.sql ----------
-- ============================================================================
-- v79: 职高语文应用文写作专用评分量规
-- 区别于话题作文（Rubric 101），应用文重在格式规范+语言得体+内容要素
-- 幂等：INSERT IGNORE
-- ============================================================================



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

-- ============================================================
-- 2. 批量创建教师档案（users表已有TEACHER角色，补teachers表记录）
-- ============================================================

-- ============================================================
-- 3. 教师-班级任教分配
-- ============================================================

-- ============================================================
-- 5. 将学生分配到班级（class_students表）
-- ============================================================

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


-- 获取现有 L2/L3 节点ID（用于挂载新节点）


-- ============================================================================
-- Part 1: 新增 L3 节点
-- ============================================================================

-- 1a. L3「古诗词鉴赏」→ 挂在 L2「文言文阅读」下

-- 1b. L3「文学常识与名句默写」→ 挂在 L2「文言文阅读」下

-- 1c. L3「标点符号」→ 挂在 L2「基础知识与运用」下

-- 1d. 拆分「病句辨析与修辞」→ 新增独立 L3「修辞手法辨析」
--     原 L3「病句辨析与修辞」改名为「病句辨析」
UPDATE knowledge_nodes SET name = '病句辨析' WHERE id = @l3_bjcf AND name = '病句辨析与修辞';


-- 重新获取可能变化的节点ID

-- ============================================================================
-- Part 2: 古诗词鉴赏 L4 — 7篇指定阅读篇目
-- ============================================================================

-- ============================================================================
-- Part 3: 文学常识与名句默写 L4 — 14篇默写篇目 + 文学常识
-- ============================================================================

-- ============================================================================
-- Part 4: 标点符号 L4 — 9种常用标点
-- ============================================================================

-- ============================================================================
-- Part 5: 修辞手法辨析 L4 — 8种修辞 + 易混辨析
-- ============================================================================

-- ============================================================================
-- Part 6: 应用文写作 — 补齐缺失3种 + 2种细化 + 拆分条据
-- ============================================================================

-- 6a. 补充缺失的应用文类型

-- 6b. 将「条据」拆分为「便条」和「单据」（如果存在）
--     单据内容已在上方新建，这里更新旧「条据」节点名称为「便条」
UPDATE knowledge_nodes
WHERE parent_id = @l3_yyw AND name = '条据';

-- 6c. 补充话题作文写作技巧L4

-- ============================================================================
-- Part 7: 文言文阅读 L4 补充 — 指定篇目 + 虚词
-- ============================================================================

-- ============================================================================
-- Part 8: 修正 exam_syllabus 考纲文本
-- ============================================================================
UPDATE exam_syllabus
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

-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务1 走进计算机网络（知识点1075~1078）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10101: 计算机网络的定义与功能（node=1075）
-- ============================================================

-- 卡片10101~10104: 计算机网络的定义与功能

-- ============================================================
-- 文章10102: 计算机网络的组成（node=1076）
-- ============================================================


-- ============================================================
-- 文章10103: 计算机网络的分类（node=1077）
-- ============================================================


-- ============================================================
-- 文章10104: 网络拓扑结构（node=1078）
-- ============================================================


-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务2 认识数据通信（知识点1079）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10105: 数据通信基本概念（node=1079）
-- ============================================================


-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务3 剖析计算机网络体系结构（知识点1080~1082）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10106: OSI七层模型及各层功能（node=1080）
-- ============================================================


-- ============================================================
-- 文章10107: TCP/IP四层模型（node=1081）
-- ============================================================


-- ============================================================
-- 文章10108: OSI与TCP/IP对应关系（node=1082）
-- ============================================================


-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务4 认识传输介质（知识点1083~1084）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10109: 有线传输介质（node=1083）
-- ============================================================


-- ============================================================
-- 文章10110: 无线传输介质（node=1084）
-- ============================================================


-- ═══════════════════════════════════════════════════════════════
-- 单元1 > 任务5 认识网络接口及网络设备（知识点1085~1089）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10111: 网卡与MAC地址（node=1085）
-- ============================================================


-- ============================================================
-- 文章10112: 集线器（node=1086）
-- ============================================================


-- ============================================================
-- 文章10113: 交换机（node=1087）
-- ============================================================


-- ============================================================
-- 文章10114: 路由器（node=1088）
-- ============================================================


-- ============================================================
-- 文章10115: 调制解调器（node=1089）
-- ============================================================


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


-- ============================================================
-- 文章10117: 以太网标准IEEE 802.3（node=1091）
-- ============================================================


-- ============================================================
-- 文章10118: MAC地址格式详解（node=1092）
-- ============================================================


-- ============================================================
-- 文章10119: IP地址的概念与点分十进制（node=1093）
-- ============================================================


-- ============================================================
-- 文章10120: IP地址分类与默认子网掩码（node=1094）
-- ============================================================


-- ============================================================
-- 文章10121: 私有IP地址范围（node=1095）
-- ============================================================


-- ============================================================
-- 文章10122: 子网掩码的作用（node=1096）
-- ============================================================


-- ============================================================
-- 文章10123: IPv6概念（node=1097）
-- ============================================================


-- ============================================================
-- 文章10124: TCP协议（node=1098）
-- ============================================================


-- ============================================================
-- 文章10125: UDP协议（node=1099）
-- ============================================================


-- ============================================================
-- 文章10126: HTTP/HTTPS协议与端口（node=1100）
-- ============================================================


-- ============================================================
-- 文章10127: FTP协议与端口（node=1101）
-- ============================================================


-- ============================================================
-- 文章10128: SMTP/POP3邮件协议（node=1102）
-- ============================================================


-- ============================================================
-- 文章10129: DNS域名解析（node=1103）
-- ============================================================


-- ============================================================
-- 文章10130: DHCP动态主机配置（node=1104）
-- ============================================================


-- ============================================================
-- 文章10131: VLAN的概念与作用（node=1105）
-- ============================================================


-- ============================================================
-- 文章10132: VLAN划分方式（node=1106）
-- ============================================================


-- ============================================================
-- 文章10133: 无线局域网基础（node=1107）
-- ============================================================


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


-- ============================================================
-- 文章10135: 域的基本概念（node=1109）
-- ============================================================


-- ============================================================
-- 文章10136: DNS服务器的功能（node=1110）
-- ============================================================


-- ============================================================
-- 文章10137: DHCP服务器的功能（node=1111）
-- ============================================================


-- ============================================================
-- 文章10138: Internet信息服务IIS（node=1112）
-- ============================================================


-- ============================================================
-- 文章10139: ping命令（node=1113）
-- ============================================================


-- ============================================================
-- 文章10140: ipconfig命令（node=1114）
-- ============================================================


-- ============================================================
-- 文章10141: tracert命令（node=1115）
-- ============================================================


-- ============================================================
-- 文章10142: netstat命令（node=1116）
-- ============================================================


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


-- ============================================================
-- 文章10144: Internet接入方式（node=1118）
-- ============================================================


-- ============================================================
-- 文章10145: WWW万维网概念（node=1119）
-- ============================================================


-- ============================================================
-- 文章10146: URL统一资源定位符（node=1120）
-- ============================================================


-- ============================================================
-- 文章10147: 浏览器的基本使用（node=1121）
-- ============================================================


-- ============================================================
-- 文章10148: 搜索引擎的使用（node=1122）
-- ============================================================


-- ============================================================
-- 文章10149: FTP文件传输协议（node=1123）
-- ============================================================


-- ============================================================
-- 文章10150: 电子邮件系统（node=1124）
-- ============================================================


-- ============================================================
-- 文章10151: 远程登录（node=1125）
-- ============================================================


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


-- ============================================================
-- 文章10153: 对称加密与非对称加密（node=1127）
-- ============================================================


-- ============================================================
-- 文章10154: 数字证书与数字签名（node=1128）
-- ============================================================


-- ============================================================
-- 文章10155: 计算机病毒的概念与特征（node=1129）
-- ============================================================


-- ============================================================
-- 文章10156: 杀毒软件的安装与配置（node=1130）
-- ============================================================


-- ============================================================
-- 文章10157: 防火墙的概念与类型（node=1131）
-- ============================================================


-- ============================================================
-- 文章10158: 安全上网习惯（node=1132）
-- ============================================================


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


-- ============================================================
-- 文章10160: 标题与段落标签（node=1134）
-- ============================================================


-- ============================================================
-- 文章10161: 换行与水平线（node=1135）
-- ============================================================


-- ============================================================
-- 文章10162: 文本格式标签（node=1136）
-- ============================================================


-- ============================================================
-- 文章10163: 图片标签 img（node=1137）
-- ============================================================


-- ============================================================
-- 文章10164: 表格标签 table（node=1138）
-- ============================================================


-- ============================================================
-- 文章10165: 有序列表与无序列表（node=1139）
-- ============================================================


-- ============================================================
-- 文章10166: 超链接标签 a（node=1140）
-- ============================================================


-- ============================================================
-- 文章10167: CSS引入方式（node=1141）
-- ============================================================


-- ============================================================
-- 文章10168: 常用CSS属性（node=1142）
-- ============================================================


-- ============================================================
-- 文章10169: 表单与input类型（node=1143）
-- ============================================================


-- ============================================================
-- 文章10170: textarea与select（node=1144）
-- ============================================================


-- ============================================================
-- 文章10171: 多媒体标签 audio/video（node=1145）
-- ============================================================


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

-- ═══════════════════════════════════════════════════════════════
-- 补充1：工作组网络的设置与文件共享（应会技能，原缺失）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10172: 工作组网络的设置与文件共享（node=1108 扩展）
-- ============================================================


-- ============================================================
-- 文章10173: 网页保存与浏览器预览（node=1121 扩展）
-- ============================================================


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
UPDATE IGNORE question_bank SET category_id = @new_bt WHERE category_id = 3057;

-- 单据(3058) → 单据（借条/收条/领条/欠条）
UPDATE IGNORE question_bank SET category_id = @new_dj WHERE category_id = 3058;

-- 审题立意(3050) → 审题与立意
UPDATE IGNORE question_bank SET category_id = @new_st WHERE category_id = 3050;

-- 语言表达技巧(3054) → 作文语言提升（句式变化/修辞润色）
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
