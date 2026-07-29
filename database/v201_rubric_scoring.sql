-- v201: 量规逐项评分（W-5 Rubric Scoring）
CREATE TABLE IF NOT EXISTS rubric_scores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    rubric_id BIGINT NOT NULL,
    dimension_id BIGINT NOT NULL,
    level INT NOT NULL DEFAULT 0,
    score DECIMAL(10,2) NOT NULL DEFAULT 0,
    comment VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_submission (submission_id),
    INDEX idx_rubric (rubric_id),
    UNIQUE KEY uk_submission_dimension (submission_id, dimension_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量规评分明细';
-- MySQL 8.0 不支持 IF NOT EXISTS，使用存储过程安全添加
SET @sql_rts = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE task_submissions ADD COLUMN rubric_total_score DECIMAL(10,2) DEFAULT NULL',
    'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'teaching_system' AND TABLE_NAME = 'task_submissions' AND COLUMN_NAME = 'rubric_total_score');
PREPARE stmt FROM @sql_rts; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql_rsa = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE task_submissions ADD COLUMN rubric_scored_at DATETIME DEFAULT NULL',
    'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'teaching_system' AND TABLE_NAME = 'task_submissions' AND COLUMN_NAME = 'rubric_scored_at');
PREPARE stmt FROM @sql_rsa; EXECUTE stmt; DEALLOCATE PREPARE stmt;
