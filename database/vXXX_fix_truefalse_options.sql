-- ============================================================
-- vXXX: 修复 TRUE_FALSE 判断题空选项问题
-- 问题: v101/v103 等批量导入的判断题 options='[]'，前端无选项渲染
-- 修复: 统一注入 ["A. √","B. ×"] 为默认选项
-- ============================================================

UPDATE question_bank
SET options = '["A. √","B. ×"]'
WHERE question_type = 'TRUE_FALSE'
  AND (options IS NULL OR options = '' OR options = '[]' OR options = '[\"\"]');

SELECT CONCAT('TRUE_FALSE options fix: ', ROW_COUNT(), ' rows updated') AS result;
