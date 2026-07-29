-- Original file: v84_english_reading_passages.sql (DDL extracted from MIXED file)
-- ============================================================

-- v84: 英语分级阅读短文池
-- Applied: 2026-06-05
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS precision_english_reading_passages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL COMMENT '短文内容',
    word_count INT DEFAULT 0,
    difficulty_level INT DEFAULT 1 COMMENT '1-5',
    min_vocab_size INT DEFAULT 0 COMMENT '阅读该文所需最低词汇量',
    grammar_tags VARCHAR(200) COMMENT '涉及的语法标签',
    new_word_list VARCHAR(500) COMMENT '目标生词列表',
    question_ids JSON COMMENT '[{"qid":101,"order":1},...]',
    source VARCHAR(20) DEFAULT 'MANUAL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_difficulty (difficulty_level),
    INDEX idx_vocab (min_vocab_size)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语分级阅读短文池';