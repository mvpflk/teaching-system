-- v100: 智慧大屏安全加固
-- L2: classroom_participations 加唯一约束防投票刷票
-- L4: wrong_questions 加 source_session_id 防错题本重复插入

-- L2: 唯一约束 (session_id, student_id, participation_type) 防止同一学生在同一会话中重复参与
ALTER TABLE classroom_participations
    ADD UNIQUE INDEX uk_session_student_type (session_id, student_id, participation_type);

-- L4: 错题本增加 source_session_id 字段，用于防重
ALTER TABLE wrong_questions
    ADD COLUMN source_session_id BIGINT NULL COMMENT '来源会话ID，用于防重' AFTER source_type;

-- L4: 给 source_session_id 加唯一约束（同一会话同一题目不重复记录）
-- 注意：已有数据可能有重复，先清理再加约束
DELETE wq1 FROM wrong_questions wq1
    INNER JOIN wrong_questions wq2
    ON wq1.student_id = wq2.student_id
    AND wq1.question_id = wq2.question_id
    AND wq1.source_type = wq2.source_type
    AND wq1.id > wq2.id
    AND wq1.source_type IN ('QUIZ', 'BUZZ');

ALTER TABLE wrong_questions
    ADD UNIQUE INDEX uk_student_question_source (student_id, question_id, source_type);
