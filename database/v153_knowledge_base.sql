-- ============================================================================
-- v153: 知识库模块 — 基础表结构
-- 知识文章 + 记忆卡片 + 间隔重复调度 + 学生收藏
-- 幂等：CREATE TABLE IF NOT EXISTS
-- ============================================================================

-- 1. 知识文章表
CREATE TABLE IF NOT EXISTS knowledge_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    content_md MEDIUMTEXT NOT NULL COMMENT 'Markdown 原文',
    excerpt VARCHAR(500) DEFAULT NULL COMMENT '摘要',
    subject_id BIGINT NOT NULL COMMENT '学科ID → dict_subject.id',
    chapter VARCHAR(100) DEFAULT NULL COMMENT '章',
    task VARCHAR(100) DEFAULT NULL COMMENT '任务',
    node_id BIGINT DEFAULT NULL COMMENT '可选关联 knowledge_nodes.id',
    memory_tips TEXT DEFAULT NULL COMMENT '记忆口诀',
    exam_focus TEXT DEFAULT NULL COMMENT '考试重点',
    difficulty TINYINT DEFAULT 1 COMMENT '难度 1-3',
    tags JSON DEFAULT NULL COMMENT '标签数组',
    syllabus_refs JSON DEFAULT NULL COMMENT '考纲引用',
    quiz JSON DEFAULT NULL COMMENT '自测题 [{type,question,options,answer,explanation}]',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subject_id (subject_id),
    INDEX idx_chapter (chapter),
    INDEX idx_node_id (node_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文章';

-- 2. 记忆卡片表
CREATE TABLE IF NOT EXISTS knowledge_flashcards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '→ knowledge_articles.id',
    front_text VARCHAR(500) NOT NULL COMMENT '卡片正面（问题）',
    back_text TEXT NOT NULL COMMENT '卡片反面（答案/详解）',
    sort_order INT DEFAULT 0 COMMENT '卡片序号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article_id (article_id),
    FOREIGN KEY (article_id) REFERENCES knowledge_articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记忆卡片';

-- 3. 间隔重复调度表
CREATE TABLE IF NOT EXISTS knowledge_review_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '→ users.id',
    flashcard_id BIGINT NOT NULL COMMENT '→ knowledge_flashcards.id',
    article_id BIGINT NOT NULL COMMENT '→ knowledge_articles.id（冗余方便查询）',
    ease_factor DECIMAL(3,2) NOT NULL DEFAULT 2.50 COMMENT '难度系数',
    interval_days INT NOT NULL DEFAULT 0 COMMENT '当前间隔天数',
    repetitions INT NOT NULL DEFAULT 0 COMMENT '连续答对次数',
    next_review_at DATETIME NOT NULL COMMENT '下次复习时间',
    last_review_at DATETIME DEFAULT NULL COMMENT '上次复习时间',
    last_rating TINYINT DEFAULT NULL COMMENT '上次评分 1-4',
    is_mastered TINYINT DEFAULT 0 COMMENT '0未掌握/1已掌握',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_flashcard (student_id, flashcard_id),
    INDEX idx_student_next_review (student_id, next_review_at),
    INDEX idx_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='间隔重复调度';

-- 4. 功能开关
INSERT INTO system_settings (setting_key, setting_value, description, value_type) VALUES
('feature.knowledge_base', 'false', '知识库模块开关', 'BOOLEAN')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 5. 收藏表
CREATE TABLE IF NOT EXISTS student_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_article (student_id, article_id),
    INDEX idx_student_id (student_id),
    FOREIGN KEY (article_id) REFERENCES knowledge_articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生收藏';
