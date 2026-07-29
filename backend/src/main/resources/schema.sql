CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar_url VARCHAR(255),
    role_id BIGINT NOT NULL,
    role_name VARCHAR(50) DEFAULT 'STUDENT',
    must_change_password TINYINT DEFAULT 0,
    school_id BIGINT DEFAULT 1,
    current_stage_id BIGINT DEFAULT 4,
    external_id VARCHAR(100),
    external_source VARCHAR(50),
    status TINYINT DEFAULT 1,
    last_login_time DATETIME,
    last_login_ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS teachers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    teacher_number VARCHAR(30) NOT NULL UNIQUE,
    school_id BIGINT DEFAULT 1,
    gender TINYINT,
    subject VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    student_number VARCHAR(30) NOT NULL UNIQUE,
    gender TINYINT,
    birthday DATE,
    enrollment_year INT,
    class_id BIGINT,
    school_id BIGINT DEFAULT 1,
    current_stage_id BIGINT DEFAULT 4,
    original_stage_id BIGINT DEFAULT 4,
    total_credits INT DEFAULT 0,
    title_level INT DEFAULT 1,
    current_streak INT DEFAULT 0,
    current_type VARCHAR(20) DEFAULT 'VOCATIONAL',
    enrollment_type VARCHAR(20),
    custom_title VARCHAR(20),
    custom_title_set_at DATETIME,
    status VARCHAR(20) DEFAULT 'active',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(100),
    class_code VARCHAR(30) NOT NULL UNIQUE,
    grade VARCHAR(20),
    major VARCHAR(100),
    academic_year VARCHAR(10),
    semester VARCHAR(10),
    head_teacher_id BIGINT,
    class_type VARCHAR(20) DEFAULT 'VOCATIONAL',
    school_id BIGINT DEFAULT 1,
    stage_id BIGINT DEFAULT 4,
    status TINYINT DEFAULT 1,
    student_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS backup_homework_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200),
    content TEXT,
    content_type VARCHAR(20) DEFAULT 'text',
    attachment_url VARCHAR(500),
    attachment_names VARCHAR(500),
    class_id BIGINT,
    teacher_id BIGINT,
    subject VARCHAR(50),
    school_id BIGINT DEFAULT 1,
    stage_id BIGINT DEFAULT 4,
    category VARCHAR(20) DEFAULT 'AFTER_CLASS',
    task_config JSON,
    target_type VARCHAR(20) DEFAULT 'CLASS',
    target_id BIGINT,
    start_time DATETIME,
    end_time DATETIME,
    allow_late_submit TINYINT DEFAULT 1,
    late_penalty DECIMAL(3,2) DEFAULT 1.00,
    notify_parents TINYINT DEFAULT 0,
    include_in_portfolio TINYINT DEFAULT 0,
    max_score INT DEFAULT 100,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS backup_homework_submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    homework_id BIGINT,
    student_id BIGINT,
    content TEXT,
    content_type VARCHAR(20) DEFAULT 'text',
    attachment_url VARCHAR(500),
    attachment_names VARCHAR(500),
    school_id BIGINT DEFAULT 1,
    stage_id BIGINT DEFAULT 4,
    score_type VARCHAR(20) DEFAULT 'POINT_100',
    grade_level VARCHAR(5),
    score_json JSON,
    include_in_portfolio TINYINT DEFAULT 0,
    submit_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_late TINYINT DEFAULT 0,
    late_hours INT DEFAULT 0,
    score INT,
    score_status VARCHAR(20) DEFAULT 'pending',
    teacher_comment TEXT,
    graded_time DATETIME,
    graded_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS credit_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT,
    rule_id BIGINT,
    transaction_type VARCHAR(30),
    credit_amount INT,
    balance_after INT,
    source_type VARCHAR(30),
    source_id BIGINT,
    description VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS backup_exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200),
    description TEXT,
    class_id BIGINT,
    teacher_id BIGINT,
    subject VARCHAR(50),
    school_id BIGINT DEFAULT 1,
    stage_id BIGINT DEFAULT 4,
    category VARCHAR(20) DEFAULT 'SUMMATIVE',
    task_config JSON,
    target_type VARCHAR(20) DEFAULT 'CLASS',
    target_id BIGINT,
    exam_type VARCHAR(20) DEFAULT 'exam',
    total_score INT DEFAULT 100,
    passing_score INT DEFAULT 60,
    question_count INT DEFAULT 0,
    duration_minutes INT DEFAULT 60,
    start_time DATETIME,
    end_time DATETIME,
    is_random_order TINYINT DEFAULT 0,
    is_show_answer TINYINT DEFAULT 0,
    allow_cheat_detection TINYINT DEFAULT 1,
    max_cheat_warnings INT DEFAULT 3,
    notify_parents TINYINT DEFAULT 0,
    include_in_portfolio TINYINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS backup_exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT,
    question_type VARCHAR(20) NOT NULL,
    question_text TEXT NOT NULL,
    question_image_url VARCHAR(500),
    options TEXT,
    correct_answer VARCHAR(500),
    explanation TEXT,
    score INT DEFAULT 5,
    difficulty_level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    attachment_url VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS backup_exam_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT,
    student_id BIGINT,
    school_id BIGINT DEFAULT 1,
    stage_id BIGINT DEFAULT 4,
    total_score INT,
    score_earned INT,
    correct_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    blank_count INT DEFAULT 0,
    start_time DATETIME,
    submit_time DATETIME,
    grade_time DATETIME,
    time_used_seconds INT,
    cheat_warnings INT DEFAULT 0,
    is_passed TINYINT DEFAULT 0,
    score_type VARCHAR(20) DEFAULT 'POINT_100',
    grade_level VARCHAR(5),
    score_json JSON,
    include_in_portfolio TINYINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ongoing',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT,
    submission_id BIGINT,
    question_id BIGINT,
    student_answer TEXT,
    is_correct TINYINT DEFAULT 0,
    auto_score DECIMAL(6,1),
    teacher_score DECIMAL(6,1),
    answer_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS credit_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(100) NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    credit_value INT NOT NULL,
    max_daily_count INT,
    conditions TEXT,
    description VARCHAR(255),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS title_levels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    level_number INT NOT NULL UNIQUE,
    level_name VARCHAR(50) NOT NULL,
    min_credits INT NOT NULL,
    max_credits INT,
    badge_icon VARCHAR(255),
    privileges TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sign_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT,
    sign_date DATE,
    sign_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    credit_earned INT DEFAULT 0,
    streak_day INT
);

-- ============================================
-- v3~v6 迁移新增表
-- ============================================
CREATE TABLE IF NOT EXISTS schools (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE,
    region_id BIGINT,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0,
    grade_years INT DEFAULT 3,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_class_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    stage_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    change_reason VARCHAR(50),
    operator_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_stage_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    from_stage_id BIGINT,
    to_stage_id BIGINT NOT NULL,
    from_class_id BIGINT,
    to_class_id BIGINT,
    change_date DATE NOT NULL,
    reason VARCHAR(100),
    operator_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    type VARCHAR(50),
    title VARCHAR(200),
    content TEXT,
    related_id BIGINT,
    is_read TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wrong_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT,
    question_id BIGINT,
    wrong_count INT DEFAULT 1,
    last_wrong_time DATETIME,
    is_mastered TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- v6: 统一任务表
-- ============================================
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL DEFAULT 1,
    stage_id BIGINT NOT NULL DEFAULT 4,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    subject VARCHAR(50),
    grade_id BIGINT,
    teacher_id BIGINT NOT NULL,
    total_score DECIMAL(6,1) DEFAULT 100.0,
    score_type VARCHAR(20) DEFAULT 'POINT_100',
    task_type VARCHAR(20) DEFAULT 'AFTER_CLASS',
    target_type VARCHAR(20) DEFAULT 'CLASS',
    target_id BIGINT,
    view_scope VARCHAR(20) DEFAULT 'CLASS',
    deadline DATETIME,
    status VARCHAR(20) DEFAULT 'DRAFT',
    review_status VARCHAR(20) DEFAULT 'PENDING',
    task_config VARCHAR(4000),
    notify_parents TINYINT DEFAULT 0,
    allow_resubmit TINYINT DEFAULT 0,
    is_required TINYINT DEFAULT 1,
    wuyu_tag VARCHAR(100),
    auto_wrongbook TINYINT DEFAULT 1,
    is_forced TINYINT DEFAULT 0,
    survey_schema TEXT,
    is_competition_mode TINYINT(1) DEFAULT 0,
    allow_custom_steps TINYINT DEFAULT 1,
    reference_images TEXT,
    term_id BIGINT,
    rubric_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_type VARCHAR(20),
    sort_order INT DEFAULT 0,
    score DECIMAL(6,1) DEFAULT 1.0
);

CREATE TABLE IF NOT EXISTS task_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    school_id BIGINT DEFAULT 1,
    stage_id BIGINT DEFAULT 4,
    content TEXT,
    attachments VARCHAR(2000),
    score DECIMAL(6,1),
    grade_level VARCHAR(5),
    score_json VARCHAR(2000),
    status VARCHAR(20) DEFAULT 'PENDING',
    cheat_warnings INT DEFAULT 0,
    cheat_terminated TINYINT DEFAULT 0,
    graded_by BIGINT,
    grade_type VARCHAR(20),
    extra_submit_allowed INT DEFAULT 0,
    resubmission_of BIGINT,
    include_in_portfolio TINYINT DEFAULT 0,
    peer_score DECIMAL(10,2),
    is_exemplar TINYINT(1) DEFAULT 0,
    submitted_at DATETIME,
    graded_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);