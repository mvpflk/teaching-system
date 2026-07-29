-- v155: 切屏事件审计日志表（防作弊审计闭环）
-- 记录每次切屏事件的时间戳、类型，便于教师端查看证据和申诉处理

CREATE TABLE IF NOT EXISTS cheat_event_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL COMMENT '任务ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  submission_id BIGINT COMMENT '提交记录ID',
  event_type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN' COMMENT '事件类型: VISIBILITY_HIDDEN / FULLSCREEN_EXIT / UNKNOWN',
  cheat_warnings INT DEFAULT 0 COMMENT '触发时的累计切屏次数',
  max_warnings INT DEFAULT 0 COMMENT '考试配置的切屏上限',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
  INDEX idx_task_student (task_id, student_id),
  INDEX idx_submission (submission_id),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='切屏事件审计日志';
