-- v152: AI调用日志增加状态列 (2026-06-10)
-- 用于 Prometheus 指标暴露 + Grafana 监控面板

ALTER TABLE ai_call_log
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'
  COMMENT '调用状态: SUCCESS/FAILED';

ALTER TABLE ai_call_log
  ADD COLUMN model VARCHAR(100) DEFAULT NULL
  COMMENT '使用的模型名称';

ALTER TABLE ai_call_log
  ADD COLUMN prompt_tokens INT DEFAULT 0
  COMMENT 'Prompt Token 数';

ALTER TABLE ai_call_log
  ADD COLUMN completion_tokens INT DEFAULT 0
  COMMENT 'Completion Token 数';

CREATE INDEX idx_ai_call_log_status ON ai_call_log (status);
CREATE INDEX idx_ai_call_log_created ON ai_call_log (created_at);
