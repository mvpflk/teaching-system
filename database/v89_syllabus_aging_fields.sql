-- v89: 考纲对齐增强 + 知识老化机制 + 错题掌握升级
-- 日期: 2026-05-30
-- 说明: 为教学系统全链路优化新增字段，所有字段均可空或带安全默认值

-- ============================================
-- 1. question_bank: 考纲维度 + 题目层级
-- ============================================
ALTER TABLE question_bank
  ADD COLUMN knowledge_dim VARCHAR(20) DEFAULT NULL
    COMMENT '考纲维度: THEORY(应知)/PRACTICE(应会)',
  ADD COLUMN tier VARCHAR(20) DEFAULT NULL
    COMMENT '题目层级: BASIC/MEDIUM/ADVANCED';

-- ============================================
-- 2. wrong_questions: 连续正确计数 + 间隔复习
-- ============================================
ALTER TABLE wrong_questions
  ADD COLUMN consecutive_correct INT DEFAULT 0
    COMMENT '连续答对次数，达到阈值(默认3)标记掌握',
  ADD COLUMN next_review_at DATETIME DEFAULT NULL
    COMMENT '下次复习计划时间，基于遗忘曲线间隔',
  ADD COLUMN mastered_streak INT DEFAULT 0
    COMMENT '掌握后连续确认次数，决定复习间隔';

-- ============================================
-- 3. tasks: 关联考纲
-- ============================================
ALTER TABLE tasks
  ADD COLUMN syllabus_id BIGINT DEFAULT NULL
    COMMENT '关联考纲ID → exam_syllabus.id，用于考纲维度分析';

-- ============================================
-- 4. knowledge_nodes: 知识老化/陈旧标记
-- ============================================
ALTER TABLE knowledge_nodes
  ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE'
    COMMENT '节点状态: ACTIVE(正常)/LEGACY(陈旧)/DEPRECATED(过时)/OBSOLETE(淘汰)',
  ADD COLUMN relevance_level INT DEFAULT 5
    COMMENT '相关度 1-10，默认5，≤3需在命题中淡化，≥8加星标',
  ADD COLUMN deprecation_note VARCHAR(500) DEFAULT NULL
    COMMENT '陈旧原因说明，如"软盘已被U盘/云存储取代"',
  ADD COLUMN last_reviewed_at DATETIME DEFAULT NULL
    COMMENT '最后审核时间';
