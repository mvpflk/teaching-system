-- ============================================================
-- v163: P0-1 答题卡批量OCR识别
-- 依赖: tasks, students 表已存在
-- 用途: 打印答题卡→拍照→Vision OCR→自动判分
-- ============================================================

-- 1. 答题卡OCR记录表
CREATE TABLE IF NOT EXISTS `answer_sheet_ocr` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`            BIGINT       NOT NULL COMMENT '关联任务ID → tasks.id',
  `student_id`         BIGINT       DEFAULT NULL COMMENT '识别到的学生ID(可为空)',
  `student_name`       VARCHAR(50)  DEFAULT NULL COMMENT '识别到的学生姓名(冗余)',
  `class_id`           BIGINT       DEFAULT NULL COMMENT '识别到的班级ID(冗余)',
  `photo_path`         VARCHAR(500) NOT NULL COMMENT '原始照片路径',
  `ocr_raw_text`       TEXT         DEFAULT NULL COMMENT 'Vision API原始返回文本',
  `parsed_answers`     JSON         DEFAULT NULL COMMENT '解析后的答案 [{"questionNo":1,"answer":"A","confidence":0.95}]',
  `overall_confidence` DECIMAL(3,2) DEFAULT 0 COMMENT '整体置信度0.00-1.00',
  `auto_grade_result`  JSON         DEFAULT NULL COMMENT '自动判分结果 {"correct":8,"total":10,"score":80}',
  `status`             VARCHAR(20)  DEFAULT 'parsed' COMMENT 'parsed/graded/manual_entry/failed/reviewed',
  `reviewer_id`        BIGINT       DEFAULT NULL COMMENT '复核教师用户ID',
  `review_note`        VARCHAR(500) DEFAULT NULL COMMENT '复核备注',
  `grader_id`          BIGINT       DEFAULT NULL COMMENT '录入员/教师用户ID(手动录入时)',
  `school_id`          BIGINT       DEFAULT 1,
  `create_time`        DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time`        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_task` (`task_id`),
  INDEX `idx_student` (`student_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_confidence` (`overall_confidence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题卡OCR识别记录';

-- 2. 系统配置项
INSERT IGNORE INTO system_settings (setting_key, setting_value, category, description) VALUES
('p01.ocr_confidence_threshold', '0.85', 'p0', 'OCR置信度阈值：低于此值标记需人工复核'),
('p01.ocr_max_photo_size_mb',    '10',    'p0', '答题卡照片最大尺寸(MB)');
