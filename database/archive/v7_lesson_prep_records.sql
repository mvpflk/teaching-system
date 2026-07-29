CREATE TABLE IF NOT EXISTS lesson_prep_records (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_prep_group_id BIGINT NOT NULL COMMENT '备课组ID',
    title               VARCHAR(200) NOT NULL COMMENT '备课主题',
    record_date         DATE NOT NULL COMMENT '备课日期',
    participant_ids     JSON COMMENT '参与教师ID列表',
    participant_count   INT DEFAULT 0 COMMENT '参与人数(冗余便于查询)',
    content             TEXT COMMENT '备课内容/纪要',
    output_urls         JSON COMMENT '产出链接(课件/教案URL)',
    recorded_by         BIGINT COMMENT '记录人(教师ID)',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_group_date (lesson_prep_group_id, record_date),
    INDEX idx_recorded_by (recorded_by)
) COMMENT='备课组活动记录';
