-- 闯关练习：checkpoint_progress 新增 last_node_index 断点续学字段
ALTER TABLE checkpoint_progress
  ADD COLUMN last_node_index INT DEFAULT NULL COMMENT '上次阅读到的知识点序号（断点续学）';
