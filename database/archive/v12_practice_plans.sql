-- v12: 实训方案 + 评分细则 + 实训报告

CREATE TABLE IF NOT EXISTS practice_plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT COMMENT '关联任务ID',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    prerequisites JSON COMMENT '前置知识要求 [{name}]',
    environment JSON COMMENT '环境要求 [{tool,version,purpose}]',
    safety_notes TEXT COMMENT '安全注意事项',
    troubleshooting TEXT COMMENT '常见问题与排故',
    team_roles JSON COMMENT '团队角色（可选）[{role,count,duty}]',
    scoring_model VARCHAR(30) NOT NULL DEFAULT 'DUAL_DIMENSION' COMMENT 'DUAL_DIMENSION/COMPETITION',
    created_by BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_plan_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实训方案';

CREATE TABLE IF NOT EXISTS practice_rubrics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    dimension VARCHAR(50) NOT NULL COMMENT '维度代码',
    dimension_label VARCHAR(100) NOT NULL COMMENT '维度中文名',
    weight DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '权重 0~1',
    criteria JSON COMMENT '评分标准 [{level:0-5,label,description}]',
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_rubric_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实训评分细则';

CREATE TABLE IF NOT EXISTS practice_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    content TEXT COMMENT 'Markdown报告正文',
    attachments JSON COMMENT '附件 [{name,url,size}]',
    result_files JSON COMMENT '成果文件 [{name,url,size}]',
    self_evaluation TEXT COMMENT '学生自评',
    peer_evaluation JSON COMMENT '互评',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_report_submission (submission_id),
    INDEX idx_report_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实训报告';
