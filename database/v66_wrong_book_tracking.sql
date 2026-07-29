-- v66: 错题本练习追踪 + 掌握来源审计
ALTER TABLE wrong_questions
  ADD COLUMN last_practice_time DATETIME     DEFAULT NULL COMMENT '最近练习时间',
  ADD COLUMN practice_count     INT          DEFAULT 0    COMMENT '练习次数',
  ADD COLUMN mastered_at        DATETIME     DEFAULT NULL COMMENT '掌握时间',
  ADD COLUMN mastered_source    VARCHAR(20)  DEFAULT NULL COMMENT '掌握来源: manual/redo/derived/single_practice';
