-- v154: 知识库自测结果持久化
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
