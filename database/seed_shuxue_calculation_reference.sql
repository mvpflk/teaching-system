-- 2022-2025 年四川省对口升学数学真题 — 解答题（CALCULATION / PROOF）
-- 来源：四川省中职对口升学数学真题卷 PDF，经用户截图+人工校验

-- === 2022 年 ===

-- 第22题：等差数列（CALCULATION）
INSERT INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES ('数学[职高]', 'CALCULATION', '{"questionText": "在等差数列{aₙ}中，已知a₃=5，a₅=9。（1）求数列{aₙ}的通项公式aₙ；（2）求数列{aₙ}的前10项和S₁₀。", "options": [], "correctAnswer": "（1）由a₃=a₁+2d=5，a₅=a₁+4d=9，解得a₁=1，d=2，aₙ=2n-1。（2）S₁₀=10×(1+19)/2=100。", "explanation": "等差数列通项公式aₙ=a₁+(n-1)d，前n项和Sₙ=n(a₁+aₙ)/2。先解方程组求a₁和d，再代公式。"}', 'REAL_EXAM', 1);

-- 第23题：立体几何（PROOF + CALCULATION）
INSERT INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES ('数学[职高]', 'CALCULATION', '{"questionText": "如图，将矩形ABCD沿对角线AC折成直二面角，设E为AD的中点。（1）求证：平面ABD⊥平面BCD；（2）若AB=3，BC=4，求三棱锥B-ACD的体积。", "options": [], "correctAnswer": "（1）由矩形ABCD得AB⊥BC，折后二面角B-AC-D为直二面角，故平面ABD⊥平面BCD。（2）V=1/3×S△ACD×BE=1/3×(1/2×3×4)×(3/√(3²+4²)×4)=1/3×6×12/5=24/5。", "explanation": "直二面角即两平面垂直。三棱锥体积V=1/3×底面积×高。利用面面垂直性质确定高。"}', 'REAL_EXAM', 1);

-- === 2023 年 ===

-- 第22题：三角函数（CALCULATION）
INSERT INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES ('数学[职高]', 'CALCULATION', '{"questionText": "已知函数f(x)=sin²x+√3 sinx cosx - 1/2，x∈R。（1）求f(x)的最小正周期；（2）当x∈[0, π/2]时，求f(x)的取值范围。", "options": [], "correctAnswer": "（1）f(x)=sin²x+√3 sinx cosx-1/2=(1-cos2x)/2+(√3/2)sin2x-1/2=(√3/2)sin2x-(1/2)cos2x=sin(2x-π/6)，T=2π/2=π。（2）x∈[0,π/2]时2x-π/6∈[-π/6, 5π/6]，f(x)∈[-1/2, 1]。", "explanation": "利用倍角公式sin²x=(1-cos2x)/2、2sinxcosx=sin2x化简，再用辅助角公式合成sin(2x-π/6)。"}' , 'REAL_EXAM', 1);

-- 第23题：解析几何 — 椭圆（CALCULATION + PROOF）
INSERT INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES ('数学[职高]', 'CALCULATION', '{"questionText": "已知椭圆C：x²/a² + y²/b² = 1（a>b>0）的离心率为√3/2，且过点(2, 0)。（1）求椭圆C的方程；（2）设直线l：y=kx+m与椭圆C交于A、B两点，O为坐标原点。若OA⊥OB，求证：m²为定值。", "options": [], "correctAnswer": "（1）由椭圆过(2,0)得a=2；由e=c/a=√3/2得c=√3，b²=a²-c²=1。椭圆方程为x²/4+y²=1。（2）联立得(1+4k²)x²+8kmx+4(m²-1)=0，由OA⊥OB得x₁x₂+y₁y₂=0，代入韦达定理化简得5m²=4(1+k²)，故m²=4(1+k²)/5，对任意k均为定值表达式。", "explanation": "由离心率定义e=c/a和椭圆基本关系a²=b²+c²。垂直条件OA⊥OB转化为向量内积为零。"}', 'REAL_EXAM', 1);

-- === 2024 年 ===

-- 第22题：函数与导数（CALCULATION）
INSERT INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES ('数学[职高]', 'CALCULATION', '{"questionText": "已知函数f(x)=x³-3x²-9x+5，x∈R。（1）求函数f(x)的单调区间和极值；（2）求函数f(x)在区间[-2, 4]上的最大值和最小值。", "options": [], "correctAnswer": "（1）f''(x)=3x²-6x-9=3(x-3)(x+1)，令f''(x)=0得x=-1或x=3。x<-1时f''>0递增，-1<x<3时f''<0递减，x>3时f''>0递增。极大值f(-1)=10，极小值f(3)=-22。（2）f(-2)=3，f(-1)=10，f(3)=-22，f(4)=-15。最大值10，最小值-22。", "explanation": "求导→找驻点→判断单调性→比较端点和极值点函数值。注意闭区间最值需比较所有候选点。"}', 'REAL_EXAM', 1);

-- 第23题：解析几何 — 抛物线（CALCULATION）
INSERT INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES ('数学[职高]', 'CALCULATION', '{"questionText": "已知抛物线C：y²=2px（p>0）的焦点为F，点A(2, 2√2)在抛物线C上。（1）求抛物线C的方程及准线方程；（2）设直线l过焦点F且与抛物线C交于M、N两点，若|MN|=8，求直线l的方程。", "options": [], "correctAnswer": "（1）代入A(2,2√2)得(2√2)²=2p×2，8=4p，p=2。抛物线方程为y²=4x，准线x=-1。（2）焦点F(1,0)，设l: y=k(x-1)，联立y²=4x得k²(x-1)²=4x→k²x²-(2k²+4)x+k²=0，由弦长公式|MN|=√(1+1/k²)×|[−(2k²+4)/k²]²-4|^(1/2)=8，解得k=±1，l: y=±(x-1)。", "explanation": "点在抛物线上→代入求p。焦点弦问题→设直线方程→联立→用弦长公式解k。"}', 'REAL_EXAM', 1);

-- === 2025 年 ===

-- 第22题：函数与导数（CALCULATION）
INSERT INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES ('数学[职高]', 'CALCULATION', '{"questionText": "已知函数f(x)=lnx - ax + 1（a∈R）。（1）当a=1时，求曲线y=f(x)在点(1, f(1))处的切线方程；（2）讨论函数f(x)的单调性；（3）若f(x)≤0恒成立，求实数a的取值范围。", "options": [], "correctAnswer": "（1）a=1时f(x)=lnx-x+1，f''(x)=1/x-1，f(1)=0，f''(1)=0，切线y=0。（2）f''(x)=1/x-a，x>0。a≤0时f''>0，f(x)在(0,+∞)递增。a>0时，0<x<1/a递增，x>1/a递减。（3）f(x)≤0⇔lnx+1≤ax⇔a≥(lnx+1)/x对∀x>0恒成立。令g(x)=(lnx+1)/x，g''(x)=-lnx/x²，g(x)max=g(1)=1，故a≥1。", "explanation": "2025年真题。导数应用三连问：切线→单调性讨论→恒成立求参数范围，是职高数学导数部分的标准考查模式。"}', 'REAL_EXAM', 1);

-- 第23题：概率统计（CALCULATION）
INSERT INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES ('数学[职高]', 'CALCULATION', '{"questionText": "某校从参加高二年级学业水平测试的600名学生中，随机抽取了60名学生的数学成绩（均为整数），整理后分成六组，画出如图所示的频率分布直方图。（1）求图中a的值；（2）估计该校高二年级学生数学成绩的众数和中位数（保留一位小数）；（3）若成绩不低于80分为优秀，估计该校高二学生中数学成绩优秀的人数。", "options": [], "correctAnswer": "（1）由(0.005+0.010+0.020+a+0.025+0.010)×10=1，解得a=0.030。（2）众数在[70,80)区间，取75。累计频率=0.5时在[70,80)内，中位数=70+(0.5-0.35)/0.030×10÷10=70+5.0=75.0。（3）不低于80分频率=(0.025+0.010)×10=0.35，优秀人数=600×0.35=210人。", "explanation": "频率分布直方图面积和为1求未知参数。众数取最高矩形中点。中位数用累计频率插值。"}', 'REAL_EXAM', 1);
