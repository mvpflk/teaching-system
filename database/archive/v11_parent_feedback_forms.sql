-- v11: 家长反馈问卷系统

CREATE TABLE IF NOT EXISTS parent_feedback_forms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    period VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    sent_at DATETIME,
    closed_at DATETIME,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_class_period (class_id, period)
);

CREATE TABLE IF NOT EXISTS parent_feedback_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    form_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    satisfaction INT,
    teaching_quality INT,
    homework_load INT,
    communication INT,
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_form_parent (form_id, parent_id),
    INDEX idx_form (form_id)
);
