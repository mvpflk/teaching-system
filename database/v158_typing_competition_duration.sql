-- v158: 打字竞赛增加时长预设字段
-- 2026-06-15

ALTER TABLE typing_competitions
    ADD COLUMN duration_minutes INT DEFAULT NULL COMMENT '竞赛预设时长(分钟)，null表示手动控制';

-- 索引：加速自动结束到期竞赛的查询
CREATE INDEX idx_competition_status_endtime ON typing_competitions(status, end_time);
