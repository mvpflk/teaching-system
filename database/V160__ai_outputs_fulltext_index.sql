-- V160: ai_outputs 全文索引，提升关键词搜索性能
-- 历史搜索对 ai_outputs.content(LONGTEXT) 的 LIKE %keyword% 查询无法走索引
-- 添加 FULLTEXT 索引以支持 MATCH ... AGAINST 快速检索

ALTER TABLE ai_outputs
    ADD FULLTEXT INDEX ft_ai_outputs_content_title (content, title)
    WITH PARSER ngram;
