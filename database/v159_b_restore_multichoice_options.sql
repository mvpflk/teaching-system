-- v159_b: 恢复被截断的多选题第5选项（E）+ 修正判断题T/F答案格式
-- 根因: AiQuestionGeneratorService 曾将多选题也截断为4选项，现修复为仅截断单选题

-- 恢复9道MULTI_CHOICE被截断的E选项（从v101/v103原始种子数据还原）
UPDATE question_bank SET options = '["A. 1∈A", "B. {1}∈A", "C. ∅⊆A", "D. {1,2}⊆A", "E. 4∉A"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%A={1,2,3}%结论正确%' AND id = 2636;
UPDATE question_bank SET options = '["A. x-1>0", "B. 2x>2", "C. -x<-1", "D. x²>1", "E. x+1>2"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%解集为{x|x>1}%' AND id = 2645;
UPDATE question_bank SET options = '["A. a₂+a₄=2a₃", "B. a₁+a₅=a₂+a₄", "C. S₅=5a₃", "D. Sn=n(a₁+an)/2", "E. an=a₁+(n-1)d"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%等差数列{an}%下列式子正确%' AND id = 2658;
UPDATE question_bank SET options = '["A. sin²α+cos²α=1", "B. sin60°=√3/2", "C. tanα=sinα/cosα", "D. cos(-α)=cosα", "E. sin(π-α)=sinα"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%下列等式正确的有%' AND id = 2665;
UPDATE question_bank SET options = '["A. 零向量的模为0", "B. 两个向量相等当且仅当大小相等方向相同", "C. |a+b|≤|a|+|b|", "D. a·b=|a||b|cosθ", "E. 若a·b>0则夹角为锐角"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%平面向量%下列说法正确%' AND id = 2672;
UPDATE question_bank SET options = '["A. 中心在原点", "B. 长半轴为3", "C. 短半轴为2", "D. 焦点在x轴上", "E. 离心率e<1"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%椭圆x²/9+y²/4=1%' AND id = 2684;
UPDATE question_bank SET options = '["A. 抛硬币出现正面", "B. 太阳从东边升起", "C. 明天下雨", "D. 掷骰子出现7点", "E. 买彩票中奖"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%随机事件%' AND id = 2691;
UPDATE question_bank SET options = '["A. 常数函数的导数为0", "B. (xⁿ)''=nx^(n-1)", "C. f(x)在极值点处导数为0", "D. f''(x)>0时函数递增", "E. f(x)=sinx的导数是cosx"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%导数%下列说法正确%' AND id = 2698;
UPDATE question_bank SET options = '["A. a+c>b+c", "B. ac>bc（c>0时）", "C. a²>b²", "D. 1/a>1/b", "E. -a<-b"]' WHERE subject = '数学[职高]' AND question_type = 'MULTI_CHOICE' AND question_text LIKE '%a>b>0%不等式一定成立%' AND id = 2776;

SELECT CONCAT('v159_b: 恢复了 ', ROW_COUNT(), ' 道多选题的第5选项') AS result;

-- 将判断题的 T/F 答案统一为 A/B 格式（与 options=["A. √","B. ×"] 一致）
UPDATE question_bank SET correct_answer = 'A' WHERE subject = '数学[职高]' AND question_type = 'TRUE_FALSE' AND correct_answer = 'T';
UPDATE question_bank SET correct_answer = 'B' WHERE subject = '数学[职高]' AND question_type = 'TRUE_FALSE' AND correct_answer = 'F';

SELECT CONCAT('v159_b: 标准化判断题答案格式完成') AS result;
