-- Original file: v126_checkpoint_system.sql (DDL extracted from MIXED file)
-- ============================================================

-- ============================================
-- v126: 闯关学习系统
-- 5张新表 + 系统设置 + 种子数据
-- 2026-06-06
-- ============================================

-- -----------------------------------------------------------
-- 表1: checkpoint_config - 关卡配置表（学科无关）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS checkpoint_config (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  subject_id      BIGINT NOT NULL COMMENT '所属学科ID → dict_subject.id',
  chapter_name    VARCHAR(100) NOT NULL COMMENT '章名称',
  task_name       VARCHAR(100) NOT NULL COMMENT '任务名称',
  task_node_id    BIGINT DEFAULT NULL COMMENT '关联 knowledge_nodes.id',
  seq             INT NOT NULL DEFAULT 1 COMMENT '关卡序号（跨章全局递增）',

  key_points_json JSON NOT NULL COMMENT '考点知识包（detailHtml+keywords+practiceQuestions）',

  question_source VARCHAR(20) NOT NULL DEFAULT 'BANK_FIRST' COMMENT 'BANK_FIRST/AI_FALLBACK',
  question_count  INT NOT NULL DEFAULT 1 COMMENT '验证闯关抽题数量',
  practice_count  INT NOT NULL DEFAULT 1 COMMENT '应会文字题数量',

  checkpoint_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/BOSS/MIXED',
  parent_config_id VARCHAR(500) DEFAULT NULL COMMENT 'Boss/Mixed关联的关卡configId(JSON array)',

  review_status   VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/REVIEWED/REJECTED',
  reviewed_by     BIGINT DEFAULT NULL COMMENT '审核教师ID',
  reviewed_at     DATETIME DEFAULT NULL,

  status          TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  created_by      BIGINT DEFAULT NULL COMMENT '创建者ID',
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_subject_seq (subject_id, seq),
  INDEX idx_task_node (task_node_id),
  INDEX idx_type (checkpoint_type, review_status),
  INDEX idx_review (review_status, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闯关配置表';

-- -----------------------------------------------------------
-- 表2: checkpoint_progress - 闯关进度表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS checkpoint_progress (
  id                BIGINT NOT NULL AUTO_INCREMENT,
  student_id        BIGINT NOT NULL COMMENT '学生ID',
  config_id         BIGINT NOT NULL COMMENT 'checkpoint_config.id',
  subject_id        BIGINT NOT NULL COMMENT '冗余，加速按学科查询',

  keywords_passed   TINYINT NOT NULL DEFAULT 0 COMMENT '关键词确认是否通过: 0/1',
  keywords_skipped  INT NOT NULL DEFAULT 0 COMMENT '跳过的关键词数量',
  keywords_attempts INT NOT NULL DEFAULT 0 COMMENT '关键词确认尝试次数',

  checkpoint_passed TINYINT NOT NULL DEFAULT 0 COMMENT '验证闯关是否通过: 0/1',
  passed_at         DATETIME DEFAULT NULL COMMENT '首次通过时间',
  attempts          INT NOT NULL DEFAULT 0 COMMENT '验证闯关尝试次数',
  correct_count     INT NOT NULL DEFAULT 0 COMMENT '最佳答对数',

  credit_granted    TINYINT NOT NULL DEFAULT 0 COMMENT '0=未发放 1=已发放',
  credit_granted_at DATETIME DEFAULT NULL,
  credit_amount     INT NOT NULL DEFAULT 0,

  wrong_question_ids JSON DEFAULT NULL COMMENT '错题ID集合',
  question_ids      JSON DEFAULT NULL COMMENT '本场抽到的题目ID集合',

  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_config (student_id, config_id),
  INDEX idx_student_subject (student_id, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闯关进度表';

-- -----------------------------------------------------------
-- 表3: checkpoint_keyword_log - 关键词确认日志
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS checkpoint_keyword_log (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  student_id      BIGINT NOT NULL,
  config_id       BIGINT NOT NULL,
  keyword_index   INT NOT NULL COMMENT '对应 key_points_json.keywords 数组索引',
  attempt_no      INT NOT NULL DEFAULT 1 COMMENT '第几次尝试',
  student_input   VARCHAR(200) DEFAULT NULL COMMENT '学生填入的内容',
  is_correct      TINYINT NOT NULL DEFAULT 0,
  followup_correct TINYINT DEFAULT NULL COMMENT '追问是否正确: 1正确/0错误/NULL无追问',
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_student_config (student_id, config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关键词确认日志（含追问记录）';

-- -----------------------------------------------------------
-- 表4: checkpoint_memory_card - 考点记忆卡
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS checkpoint_memory_card (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  student_id  BIGINT NOT NULL COMMENT '学生ID',
  config_id   BIGINT NOT NULL COMMENT 'checkpoint_config.id',
  card_json   JSON NOT NULL COMMENT '记忆卡内容（关键词+释义+考点摘要）',
  last_reviewed_at DATETIME DEFAULT NULL COMMENT '上次复习时间',
  review_count     INT NOT NULL DEFAULT 0 COMMENT '复习次数',
  last_push_at     DATETIME DEFAULT NULL COMMENT '上次推送提醒时间',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_config (student_id, config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考点记忆卡';

-- -----------------------------------------------------------
-- 表5: dict_major_subject - 专业-学科映射表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS dict_major_subject (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  major_id    BIGINT NOT NULL COMMENT 'dict_major.id',
  subject_id  BIGINT NOT NULL COMMENT 'dict_subject.id',
  sort_order  INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_major_subject (major_id, subject_id),
  INDEX idx_major (major_id),
  INDEX idx_subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业-学科映射表';