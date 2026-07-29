-- v125: 题目编辑历史 + 版本追踪 (2026-06-04)
-- R83 Phase B: AI纠错数据完整性 — 版本历史/软删除/教师间通知

CREATE TABLE IF NOT EXISTS question_edit_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id     BIGINT          NOT NULL COMMENT '关联 question_bank.id',
    version         INT             NOT NULL DEFAULT 1 COMMENT '编辑版本号',
    change_summary  VARCHAR(500)    NULL     COMMENT '修改摘要',
    before_snapshot JSON            NULL     COMMENT '修改前快照',
    after_snapshot  JSON            NULL     COMMENT '修改后快照',
    edited_by       BIGINT          NULL     COMMENT '编辑人 user_id',
    edit_type       VARCHAR(20)     NOT NULL DEFAULT 'UPDATE' COMMENT 'UPDATE/AI_SUGGEST/APPROVE/REJECT',
    school_id       BIGINT          NOT NULL DEFAULT 1,
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_question_id (question_id),
    INDEX idx_edited_by (edited_by),
    INDEX idx_edit_type (edit_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库编辑历史';
