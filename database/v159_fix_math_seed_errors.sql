-- v159: 修复数学种子题库中的2个数据错误
-- 错误1: 集合填空题 a=0 → a=0或1
-- 错误2: 函数定义域题 选项A和C重复 → C改为不同值

-- 修复1: 集合 A={1,a}, B={1,a²}, A=B → a=0或1
UPDATE question_bank
SET correct_answer = '0或1',
    explanation = 'A=B则元素相同。由a=a²得a(a-1)=0，a=0或a=1。a=0时A={1,0}=B={1,0}；a=1时A={1,1}={1}=B={1,1}={1}。两种均满足A=B。'
WHERE subject = '数学[职高]'
  AND question_type = 'FILL_IN'
  AND question_text LIKE '%A={1,a}%B={1,a²}%A=B%'
  AND correct_answer = '0'
LIMIT 1;

-- 修复2: f(x)=√(x-2)+1/(x-1) 定义域 — 选项C与A重复，改为不同值
UPDATE question_bank
SET options = '["A. [2,+∞)","B. (1,+∞)","C. (-∞,2]","D. (2,+∞)"]'
WHERE subject = '数学[职高]'
  AND question_type = 'SINGLE_CHOICE'
  AND question_text LIKE '%√(x-2)+1/(x-1)%定义域%'
  AND options LIKE '%C. [2,+∞)%'
LIMIT 1;

SELECT 'v159: 数学种子题库错误修复完成' AS result;
