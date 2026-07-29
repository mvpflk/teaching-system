-- v68: 智慧大屏互动系统表
CREATE TABLE IF NOT EXISTS classroom_sessions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  session_type VARCHAR(30) NOT NULL COMMENT 'QUIZ/BUZZ/POLL',
  scene_mode VARCHAR(20) NOT NULL DEFAULT 'LAB' COMMENT 'LAB/CLASSROOM',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED',
  question_text VARCHAR(500),
  question_id BIGINT COMMENT '关联 classroom_questions.id 或 question_bank.id',
  poll_data JSON COMMENT '投票选项JSON',
  buzz_winner_id BIGINT COMMENT '抢答胜出学生ID',
  buzz_winner_time BIGINT COMMENT '抢答毫秒时间戳',
  task_id BIGINT COMMENT '绑定的任务ID（可选）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_class (class_id, created_at),
  INDEX idx_teacher (teacher_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS classroom_questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  teacher_id BIGINT NOT NULL,
  subject VARCHAR(50),
  chapter VARCHAR(100),
  tag VARCHAR(50) COMMENT '题目标签',
  content VARCHAR(1000) NOT NULL,
  reference_answer VARCHAR(1000) COMMENT '参考答案',
  difficulty TINYINT DEFAULT 2 COMMENT '难度1-5',
  source VARCHAR(20) DEFAULT 'MANUAL' COMMENT 'MANUAL/IMPORT/QUESTION_BANK',
  source_question_id BIGINT COMMENT '从question_bank选取时记录原题ID',
  usage_count INT DEFAULT 0 COMMENT '被抽问次数',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_teacher (teacher_id),
  INDEX idx_subject_chapter (subject, chapter),
  INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS classroom_participations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  participation_type VARCHAR(30) NOT NULL COMMENT 'QUIZZED/BUZZED/VOTED',
  is_correct TINYINT COMMENT '0=错 1=对 NULL=未评分(投票)',
  score_earned INT DEFAULT 0,
  response TEXT COMMENT '学生回答内容',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session (session_id),
  INDEX idx_student (student_id),
  UNIQUE KEY uk_session_student_type (session_id, student_id, participation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
