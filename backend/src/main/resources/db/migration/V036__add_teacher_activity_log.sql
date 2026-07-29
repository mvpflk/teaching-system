 -- v161: 课题研究 — 教师使用行为日志表
 CREATE TABLE teacher_activity_log (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   teacher_id BIGINT NOT NULL COMMENT '教师ID(teachers表)',
   action VARCHAR(50) NOT NULL COMMENT 'CREATE_TASK,GRADE,AI_GENERATE,VIEW_ANALYTICS,DIAGNOSE',
   target_type VARCHAR(50) COMMENT 'TASK,QUESTION,ANALYSIS,DIAGNOSIS',
   target_id BIGINT COMMENT '操作目标ID',
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   INDEX idx_teacher_id (teacher_id),
   INDEX idx_created_at (created_at)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师使用行为日志';
