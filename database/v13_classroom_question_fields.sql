-- v13: question_bank 新增 intent/category 字段（课堂提问意图+认知层次）
ALTER TABLE question_bank ADD COLUMN intent VARCHAR(200) DEFAULT NULL COMMENT '课堂提问意图';
ALTER TABLE question_bank ADD COLUMN category VARCHAR(50) DEFAULT NULL COMMENT '认知层次 RECALL/COMPREHEND/APPLY/EXTEND';
