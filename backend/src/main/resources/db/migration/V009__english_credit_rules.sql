-- v84: 英语快速通道测试题表 + 积分规则
-- Applied: 2026-06-05
SET NAMES utf8mb4;

-- 快速通道测试题表
CREATE TABLE IF NOT EXISTS precision_english_quick_test (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stage INT NOT NULL COMMENT '所属阶段 1-7',
    type VARCHAR(20) NOT NULL COMMENT 'VOCAB/GRAMMAR',
    question_id BIGINT NOT NULL COMMENT 'question_bank.id',
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stage_qid (stage, question_id),
    INDEX idx_stage_type (stage, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语快速通道测试题';

-- 积分规则 + 冰冻卡商品
INSERT IGNORE INTO credit_rules (rule_code, rule_name, action_type, credit_value, max_daily_count, description, status) VALUES
('engl_daily',       '完成每日英语练习', 'ENGLISH_DAILY',   2, 1, '每日提交全部练习题目', 1),
('engl_perfect',     '英语完美日',       'ENGLISH_PERFECT', 3, 1, '全题首次答对且未点看答案', 1),
('engl_7streak',     '英语7天连打卡',    'ENGLISH_STREAK',  5, 1, '自然周内每天有练习记录', 1),
('engl_improve',     '英语周进步之星',   'ENGLISH_IMPROVE', 8, 1, '本周正确率比上周提升≥10%', 1),
('engl_vocab50',     '英语词汇50词',     'ENGLISH_MILESTONE', 3, 1, '已掌握词汇≥50', 1),
('engl_vocab100',    '英语词汇100词',    'ENGLISH_MILESTONE', 5, 1, '已掌握词汇≥100', 1),
('engl_vocab200',    '英语词汇200词',    'ENGLISH_MILESTONE', 8, 1, '已掌握词汇≥200', 1),
('engl_vocab300',    '英语词汇300词',    'ENGLISH_MILESTONE', 10, 1, '已掌握词汇≥300', 1),
('engl_vocab400',    '英语词汇400词',    'ENGLISH_MILESTONE', 12, 1, '已掌握词汇≥400', 1),
('engl_vocab500',    '英语词汇500词',    'ENGLISH_MILESTONE', 15, 1, '已掌握词汇≥500', 1),
('engl_stage_clear', '英语阶段通关',     'ENGLISH_MILESTONE', 12, 1, '单个阶段进度达100%', 1),
('engl_allstages',   '英语全阶段通关',   'ENGLISH_MILESTONE', 50, 1, '7个阶段全部完成', 1);

INSERT IGNORE INTO credit_shop_items (item_code, item_name, item_type, credit_price, stock_count, image_url, description, status) VALUES
('english_freeze_card', '英语冰冻卡🧊', 'card', 10, -1, '/img/freeze-card.svg', '允许断签1天不灭火苗。最多持有3张。', 1);
