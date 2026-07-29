-- v10: 家校消息表（教师↔家长站内信）

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL COMMENT '发送者 users.id',
    receiver_id BIGINT NOT NULL COMMENT '接收者 users.id',
    content TEXT NOT NULL COMMENT '消息内容',
    is_read TINYINT DEFAULT 0 COMMENT '0未读/1已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_conversation (sender_id, receiver_id)
) COMMENT='家校消息';
