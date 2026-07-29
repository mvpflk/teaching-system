-- v99: 补齐偏科提分模块缺失的表和修复
-- 1. precision_reading_seeds — 英语阅读短文种子表
-- ============================================================

CREATE TABLE IF NOT EXISTS `precision_reading_seeds` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title`       VARCHAR(200) NOT NULL COMMENT '短文标题',
  `content`     TEXT NOT NULL COMMENT '短文内容',
  `subject`     VARCHAR(100) DEFAULT '英语[职高]' COMMENT '学科',
  `difficulty`  TINYINT DEFAULT 1 COMMENT '难度1-5',
  `category`    VARCHAR(50) DEFAULT '趣味' COMMENT '分类',
  `source`      VARCHAR(50) DEFAULT 'seed' COMMENT '来源',
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_subject` (`subject`),
  KEY `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='英语阅读短文种子';
