-- v67: 展示墙评论+审核系统
CREATE TABLE IF NOT EXISTS showcase_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    reviewer_id BIGINT COMMENT '审核人',
    review_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_work_status (work_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
