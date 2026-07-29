-- v151: 学习资源生成状态与审核机制
-- 在 knowledge_nodes 表新增 5 列 + 2 索引

ALTER TABLE knowledge_nodes
  ADD COLUMN resource_generated_at   DATETIME DEFAULT NULL COMMENT '最近AI生成时间',
  ADD COLUMN resource_status         VARCHAR(20) DEFAULT NULL COMMENT '审核: PENDING/APPROVED/REJECTED',
  ADD COLUMN resource_reject_reason  VARCHAR(500) DEFAULT NULL COMMENT '教师拒绝原因',
  ADD COLUMN video_checked_at        DATETIME DEFAULT NULL COMMENT '视频链接上次检查时间',
  ADD COLUMN resource_version        INT DEFAULT 0 COMMENT '资源版本号，每次修改+1';

CREATE INDEX idx_resource_status ON knowledge_nodes(resource_status);
CREATE INDEX idx_video_checked   ON knowledge_nodes(video_checked_at);
