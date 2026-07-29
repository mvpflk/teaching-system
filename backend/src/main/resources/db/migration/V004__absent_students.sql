-- v16: 新增 classroom_absent_students 表，缺席管理持久化
CREATE TABLE IF NOT EXISTS `classroom_absent_students` (
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `session_id` BIGINT DEFAULT NULL COMMENT '标记时的课堂会话ID',
  `marked_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '标记时间',
  PRIMARY KEY (`class_id`, `student_id`),
  KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂缺席学生表';
