START TRANSACTION;

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='实数的概念与分类' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '下列数中是有理数的是', '[{"key":"A","text":"$\\\\sqrt{3}$"},{"key":"B","text":"$\\\\pi$"},{"key":"C","text":"$0.333...$"},{"key":"D","text":"$\\\\sqrt{5}$"}]', 'C', '0.333...=1/3可表示为分数是有理数。√3,π,√5均无法表示为分数。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='整式加减乘除运算' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '$(x-1)(x+5)$ 展开为', '[{"key":"A","text":"$x^2+4x-5$"},{"key":"B","text":"$x^2-4x-5$"},{"key":"C","text":"$x^2+6x-5$"},{"key":"D","text":"$x^2+4x+5$"}]', 'A', '(x-1)(x+5)=x^2+5x-x-5=x^2+4x-5。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='解一元一次方程' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '方程 $5x+12=3x+20$ 的解为', '[{"key":"A","text":"$x=16$"},{"key":"B","text":"$x=8$"},{"key":"C","text":"$x=4$"},{"key":"D","text":"$x=2$"}]', 'C', '移项:5x-3x=20-12,2x=8,x=4。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='代入消元法与加减消元法' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '解方程组 $\\begin{cases} 2x+y=7 \\\\ x-y=2 \\end{cases}$ 得', '[{"key":"A","text":"$(3,1)$"},{"key":"B","text":"$(1,3)$"},{"key":"C","text":"$(2,3)$"},{"key":"D","text":"$(3,-1)$"}]', 'A', '两式相加得3x=9,x=3。代入2x+y=7得y=1。解(3,1)。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='一元二次方程公式法求解' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '方程 $2x^2+x-1=0$ 的解为', '[{"key":"A","text":"$x=0.5$ 或 $x=-1$"},{"key":"B","text":"$x=-0.5$ 或 $x=1$"},{"key":"C","text":"$x=1$ 或 $x=-1$"},{"key":"D","text":"$x=2$ 或 $x=-0.5$"}]', 'A', '判别式=1+8=9。x=(-1±3)/4。x1=0.5,x2=-1。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='根的判别式与韦达定理' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '若方程 $x^2+mx+4=0$ 有等根，则 $m=$', '[{"key":"A","text":"$\\\\pm 2$"},{"key":"B","text":"$\\\\pm 4$"},{"key":"C","text":"$4$"},{"key":"D","text":"$-4$"}]', 'B', '有等根即判别式=0:m^2-16=0,m=±4。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='一次函数与方程不等式的关系' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '不等式 $2x-3>5$ 的解集为', '[{"key":"A","text":"$x>1$"},{"key":"B","text":"$x>4$"},{"key":"C","text":"$x<4$"},{"key":"D","text":"$x<-1$"}]', 'B', '2x-3>5,2x>8,x>4。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='勾股定理与简单应用' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '直角三角形的两条直角边分别为6和8，则斜边上的高为', '[{"key":"A","text":"$10$"},{"key":"B","text":"$4.8$"},{"key":"C","text":"$5$"},{"key":"D","text":"$6$"}]', 'B', '斜边c=√(36+64)=10。斜边上高h=ab/c=48/10=4.8。', 3, 1);

COMMIT;
