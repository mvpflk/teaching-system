-- V047: teacher_reference_questions 教师上传的真题参考（AI 组卷 Few-Shot 风格注入）
-- 表空不生效，插入数据后 AI 组卷 Prompt 末尾自动追加参考样题
-- 关联：feat/knowledge-optimize — AI 组卷 Few-Shot 真题参考注入方案

CREATE TABLE IF NOT EXISTS `teacher_reference_questions` (
  `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
  `subject`       varchar(50)  NOT NULL COMMENT '学科名，如 语文[职高]',
  `question_type` varchar(30)  NOT NULL COMMENT '题型代码，如 SINGLE_CHOICE',
  `content_json`  text         NOT NULL COMMENT '完整题目JSON（含questionText/options/correctAnswer/explanation）',
  `source`        varchar(20)  DEFAULT 'TEACHER_UPLOAD' COMMENT '来源：TEACHER_UPLOAD/SYLLABUS_SAMPLE/REAL_EXAM',
  `enabled`       tinyint(1)   DEFAULT 1 COMMENT '1=启用 0=禁用',
  `created_at`    datetime     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_subject_type` (`subject`, `question_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师上传真题参考——AI组卷Few-shot风格对齐';
