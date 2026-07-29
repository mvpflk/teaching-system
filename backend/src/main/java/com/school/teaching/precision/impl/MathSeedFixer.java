package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 一次性修复生产环境种子数据乱码（latin1连接导致的双重UTF-8编码）。
 * 通过JDBC正确编码重新写入/修复干净数据。修复成功后自动禁用。
 * R72+: 新增knowledge_nodes编码修复 + 逐条检查词汇种子
 */
@Slf4j
@Component
public class MathSeedFixer {

    @Autowired private QuestionBankMapper questionMapper;
    @Autowired(required = false) private com.school.teaching.mapper.PrecisionVocabularySeedMapper seedMapper;
    @Autowired(required = false) private KnowledgeNodeMapper knowledgeNodeMapper;

    private boolean mathFixed = false;
    private boolean vocabFixed = false;
    private boolean nodeFixed = false;

    private static final String[][] MATH_SEEDS = {
        // === 集合 3题 ===
        {"已知集合A={1,2,3}，B={2,3,4}，则A∩B等于：",
         "[\"A. {1,2,3,4}\",\"B. {2,3}\",\"C. {1,4}\",\"D. {1,2}\"]", "B",
         "交集是取两个集合共有的元素，A和B共有的元素是2和3。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"设全集U={1,2,3,4,5}，集合A={1,2,3}，则A的补集等于：",
         "[\"A. {1,2,3}\",\"B. {4,5}\",\"C. {1,4,5}\",\"D. U\"]", "B",
         "补集是在全集中去掉A中元素后剩下的部分：U-A={4,5}。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"下列关系中正确的是：",
         "[\"A. 0∈∅\",\"B. {0}=∅\",\"C. {0}⊆{0,1}\",\"D. 0⊆{0}\"]", "C",
         "单元素集合{0}是集合{0,1}的子集。A错，空集不含任何元素。", "数学[职高]", "SINGLE_CHOICE", "2"},

        // === 不等式 3题 ===
        {"不等式x²-4<0的解集是：",
         "[\"A. {x|x>2或x<-2}\",\"B. {x|-2<x<2}\",\"C. {x|x>2}\",\"D. {x|x<-2}\"]", "B",
         "x²-4<0即x²<4，解得-2<x<2。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"不等式x²-5x+6>0的解集是：",
         "[\"A. {x|x>3或x<2}\",\"B. {x|2<x<3}\",\"C. {x|x>3}\",\"D. {x|x<2}\"]", "A",
         "因式分解(x-2)(x-3)>0，抛物线开口向上，解集为x>3或x<2。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"若a>b，则下列不等式一定成立的是：",
         "[\"A. a²>b²\",\"B. ac>bc\",\"C. a+c>b+c\",\"D. |a|>|b|\"]", "C",
         "不等式两边同时加上同一个数，不等号方向不变。", "数学[职高]", "SINGLE_CHOICE", "1"},

        // === 函数 3题 ===
        {"函数$f(x)=\\sqrt{x-2}$的定义域是：",
         "[\"A. {x|x≥2}\",\"B. {x|x>2}\",\"C. {x|x≤2}\",\"D. R\"]", "A",
         "被开方数x-2≥0，解得x≥2。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"下列函数中为奇函数的是：",
         "[\"A. y=x²\",\"B. y=x³\",\"C. y=|x|\",\"D. y=x²+1\"]", "B",
         "奇函数满足f(-x)=-f(x)。y=x³中(-x)³=-x³，满足条件。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"二次函数$y=x²-4x+3$的顶点坐标是：",
         "[\"A. (2,-1)\",\"B. (-2,1)\",\"C. (2,1)\",\"D. (-2,-1)\"]", "A",
         "配方得y=(x-2)²-1，顶点为(2,-1)。", "数学[职高]", "SINGLE_CHOICE", "2"},

        // === 数列 3题 ===
        {"等差数列{an}中，a1=2，d=3，则a10等于：",
         "[\"A. 30\",\"B. 29\",\"C. 28\",\"D. 32\"]", "B",
         "an=a1+(n-1)d=2+9×3=29。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"等比数列{bn}中，b1=2，q=2，则b5等于：",
         "[\"A. 16\",\"B. 32\",\"C. 64\",\"D. 8\"]", "B",
         "bn=b1×q^(n-1)=2×2⁴=32。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"等差数列{an}的前10项和为S10，a1=1，a10=19，则S10等于：",
         "[\"A. 100\",\"B. 200\",\"C. 90\",\"D. 190\"]", "A",
         "Sn=n(a1+an)/2=10×(1+19)/2=100。", "数学[职高]", "SINGLE_CHOICE", "3"},

        // === 三角函数 3题 ===
        {"sin60°的值为：",
         "[\"A. 1/2\",\"B. √3/2\",\"C. √2/2\",\"D. √3\"]", "B",
         "特殊角三角函数值：sin60°=√3/2。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"已知sinα=3/5，且α为锐角，则cosα等于：",
         "[\"A. 4/5\",\"B. 3/5\",\"C. -4/5\",\"D. 5/3\"]", "A",
         "由sin²α+cos²α=1，cosα=√(1-9/25)=4/5。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"函数$y=2\\sin x$的最大值是：",
         "[\"A. 1\",\"B. 2\",\"C. 3\",\"D. 0\"]", "B",
         "sinx的最大值为1，2sinx的最大值=2×1=2。", "数学[职高]", "SINGLE_CHOICE", "1"},

        // === 平面向量 3题 ===
        {"向量a=(2,3)，b=(1,-1)，则a·b等于：",
         "[\"A. -1\",\"B. 1\",\"C. 5\",\"D. -5\"]", "A",
         "a·b=2×1+3×(-1)=2-3=-1。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"已知|a|=3，|b|=4，a·b=6，则a与b的夹角余弦值为：",
         "[\"A. 1/4\",\"B. 1/2\",\"C. 2/3\",\"D. 3/4\"]", "B",
         "cosθ=a·b/(|a||b|)=6/(3×4)=1/2。", "数学[职高]", "SINGLE_CHOICE", "3"},
        {"若向量a=(2,1)，b=(x,2)，且a∥b，则x等于：",
         "[\"A. 1\",\"B. 2\",\"C. 4\",\"D. 8\"]", "C",
         "a∥b时对应坐标成比例：2/x=1/2，x=4。", "数学[职高]", "SINGLE_CHOICE", "2"},

        // === 立体几何 3题 ===
        {"正方体的棱长为2，其体积等于：",
         "[\"A. 4\",\"B. 6\",\"C. 8\",\"D. 12\"]", "C",
         "正方体体积V=a³=2³=8。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"底面半径为3，高为4的圆柱，其侧面积为：",
         "[\"A. 12π\",\"B. 24π\",\"C. 36π\",\"D. 48π\"]", "B",
         "圆柱侧面积S=2πrh=2π×3×4=24π。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"球的半径为3，则体积为：",
         "[\"A. 9π\",\"B. 27π\",\"C. 36π\",\"D. 108π\"]", "C",
         "球体积V=(4/3)πr³=(4/3)π×27=36π。", "数学[职高]", "SINGLE_CHOICE", "2"},

        // === 平面解析几何 3题 ===
        {"圆$(x-1)²+(y+2)²=9$的半径是：",
         "[\"A. 1\",\"B. 2\",\"C. 3\",\"D. 9\"]", "C",
         "圆的标准方程(x-a)²+(y-b)²=r²中，r²=9，r=3。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"点P(2,3)到直线3x-4y+5=0的距离为：",
         "[\"A. 1/5\",\"B. 1\",\"C. 5\",\"D. 0\"]", "A",
         "d=|3×2-4×3+5|/√(9+16)=|6-12+5|/5=1/5。", "数学[职高]", "SINGLE_CHOICE", "3"},
        {"过点(1,3)且斜率为2的直线方程是：",
         "[\"A. y=2x+1\",\"B. y=2x+3\",\"C. y=2x-1\",\"D. y=x+2\"]", "A",
         "点斜式y-3=2(x-1)，整理得y=2x+1。", "数学[职高]", "SINGLE_CHOICE", "2"},

        // === 概率与统计 3题 ===
        {"同时掷两枚硬币，都出现正面的概率是：",
         "[\"A. 1/2\",\"B. 1/3\",\"C. 1/4\",\"D. 3/4\"]", "C",
         "样本空间有4种结果，都是正面只有1种，P=1/4。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"从1,2,3,4中任取2个不同的数，和为奇数的概率是：",
         "[\"A. 1/4\",\"B. 1/2\",\"C. 2/3\",\"D. 3/4\"]", "C",
         "总取法C(4,2)=6，和为奇数需一奇一偶：2×2=4种，P=4/6=2/3。", "数学[职高]", "SINGLE_CHOICE", "2"},
        {"一组数据：2,4,4,6,8的中位数是：",
         "[\"A. 2\",\"B. 4\",\"C. 5\",\"D. 6\"]", "B",
         "从小到大排列后中间位置的数（第3个）为4。", "数学[职高]", "SINGLE_CHOICE", "1"},

        // === 导数初步 3题 ===
        {"函数$f(x)=x²$的导数是：",
         "[\"A. f'(x)=x\",\"B. f'(x)=2x\",\"C. f'(x)=2\",\"D. f'(x)=x²\"]", "B",
         "幂函数求导公式：(xⁿ)'=nxⁿ⁻¹，所以(x²)'=2x。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"函数$f(x)=x³-3x$的单调递增区间是：",
         "[\"A. (-∞,-1)∪(1,+∞)\",\"B. (-1,1)\",\"C. (-∞,+∞)\",\"D. (-∞,0)\"]", "A",
         "f'(x)=3x²-3=3(x+1)(x-1)，f'(x)>0得x<-1或x>1。", "数学[职高]", "SINGLE_CHOICE", "3"},
        {"曲线$y=x²$在点(1,1)处的切线斜率是：",
         "[\"A. 0\",\"B. 1\",\"C. 2\",\"D. -1\"]", "C",
         "y'=2x，在x=1处斜率k=2×1=2。", "数学[职高]", "SINGLE_CHOICE", "2"},

        // === 初中基础补漏 3题 ===
        {"解方程：x²-5x+6=0，两根之和为：",
         "[\"A. 5\",\"B. 6\",\"C. -5\",\"D. -6\"]", "A",
         "韦达定理：x1+x2=-b/a=5/1=5。也可分解(x-2)(x-3)=0验证。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"直角三角形的两条直角边分别为3和4，则斜边长为：",
         "[\"A. 5\",\"B. 6\",\"C. 7\",\"D. 8\"]", "A",
         "勾股定理：c²=a²+b²=9+16=25，c=5。", "数学[职高]", "SINGLE_CHOICE", "1"},
        {"下列各式中，计算正确的是：",
         "[\"A. a²·a³=a⁶\",\"B. (a²)³=a⁵\",\"C. a⁶÷a²=a³\",\"D. (ab)²=a²b²\"]", "D",
         "(ab)²=a²b²正确。A应为a⁵，B应为a⁶，C应为a⁴。", "数学[职高]", "SINGLE_CHOICE", "1"},
    };

    @EventListener(ApplicationReadyEvent.class)
    public void fixMathSeeds() {
        if (!mathFixed) try {
            long mathCount = questionMapper.selectCount(
                new LambdaQueryWrapper<QuestionBank>().eq(QuestionBank::getSubject, "数学[职高]"));
            log.info("MathSeedFixer: 数学题目总数={}", mathCount);

            // 仅修复缺失的 categoryId，不因数量不足删除已有题目
            if (mathCount < 30) {
                log.info("MathSeedFixer: 题目不足30题，检查是否需补充导入（不删除已有数据）");
                // 仅供首次或极少量题目场景导入；已有足够数据的保留不动
                doImportIfMissing();
            } else {
                // 检查编码是否正常
                List<QuestionBank> sample = questionMapper.selectList(
                    new LambdaQueryWrapper<QuestionBank>()
                        .eq(QuestionBank::getSubject, "数学[职高]")
                        .last("LIMIT 1"));
                if (!sample.isEmpty()) {
                    String text = sample.get(0).getQuestionText();
                    if (text != null && text.matches(".*[\\u4e00-\\u9fff].*")) {
                        log.info("MathSeedFixer: 数学题目中文正常，仅修复缺失categoryId。样本: {}", text.substring(0, Math.min(30, text.length())));
                    } else {
                        log.info("MathSeedFixer: 检测到乱码，删除乱码数据并重新导入。样本: {}", text.substring(0, Math.min(30, text.length())));
                        questionMapper.delete(new LambdaQueryWrapper<QuestionBank>()
                            .eq(QuestionBank::getSubject, "数学[职高]"));
                        doImport();
                    }
                }
                // 修复已有题目的 categoryId（缺失时根据知识点关键词自动匹配）
                fixMissingCategoryIds();
            }
            // LaTeX 语法校验 + 自动修复
            try {
                List<QuestionBank> mathQs = questionMapper.selectList(
                    new LambdaQueryWrapper<QuestionBank>().eq(QuestionBank::getSubject, "数学[职高]").eq(QuestionBank::getStatus, 1));
                int latexErrors = 0, latexFixed = 0;
                for (QuestionBank q : mathQs) {
                    String text = q.getQuestionText();
                    String fixed = fixLatexErrors(text, q.getId());
                    if (!fixed.equals(text)) { q.setQuestionText(fixed); questionMapper.updateById(q); latexFixed++; text = fixed; }
                    String err = validateLatex(text);
                    if (err != null) { log.warn("MathSeedFixer: LaTeX语法错误(无法自动修复) id={} text={} error={}", q.getId(), text.substring(0, Math.min(60, text.length())), err); latexErrors++; }
                }
                if (latexFixed > 0) log.info("MathSeedFixer: 自动修复了 {} 道数学题的 LaTeX 语法错误", latexFixed);
                if (latexErrors > 0) log.warn("MathSeedFixer: 仍有 {} 道数学题存在 LaTeX 语法错误（需手动修复）", latexErrors);
                else log.info("MathSeedFixer: 所有数学题 LaTeX 语法校验通过");
            } catch (Exception e) { log.warn("MathSeedFixer: LaTeX 校验扫描失败", e); }

            // v159: 启动时全量审计 — 检查所有数学题的答案/选项一致性
            try {
                List<QuestionBank> allMath = questionMapper.selectList(
                    new LambdaQueryWrapper<QuestionBank>().eq(QuestionBank::getSubject, "数学[职高]").eq(QuestionBank::getStatus, 1));
                int issues = 0;
                for (QuestionBank qb : allMath) {
                    String err = PrecisionHelper.validateQuestion(qb);
                    if (err != null) { log.warn("MathSeedFixer: 题目审计发现问题 — {}", err); issues++; }
                }
                if (issues > 0) log.warn("MathSeedFixer: 全量审计发现 {} 道题目存在数据错误（已 log.warn 详情），请执行 database/v159_fix_math_seed_errors.sql 修复", issues);
                else log.info("MathSeedFixer: 全量审计通过 — {} 道数学题均无数据错误", allMath.size());
            } catch (Exception e) { log.warn("MathSeedFixer: 全量审计扫描失败", e); }
        } catch (Exception e) {
            log.error("MathSeedFixer: 数学修复失败", e);
        } finally { mathFixed = true; }

        // 修复词汇种子编码 — 逐条检查，不再仅看第一条
        if (!vocabFixed) {
            try {
                log.info("MathSeedFixer: 开始逐条检查词汇种子, seedMapper={}", seedMapper);
                if (seedMapper == null) {
                    log.warn("MathSeedFixer: seedMapper is null, skipping vocab fix");
                } else {
                    java.util.List<com.school.teaching.entity.PrecisionVocabularySeed> seeds = seedMapper.selectList(null);
                    log.info("MathSeedFixer: 词汇种子总数={}", seeds != null ? seeds.size() : 0);
                    if (seeds != null && !seeds.isEmpty()) {
                        int fixedCount = 0, corruptedCount = 0;
                        for (com.school.teaching.entity.PrecisionVocabularySeed s : seeds) {
                            boolean changed = false;
                            String w = fixIfGarbled(s.getWord());
                            if (!w.equals(s.getWord())) { s.setWord(w); changed = true; }
                            String m = fixIfGarbled(s.getMeaning());
                            if (!m.equals(s.getMeaning())) { s.setMeaning(m); changed = true; }
                            String p = s.getPhonetic() != null ? fixIfGarbled(s.getPhonetic()) : null;
                            if (p != null && !p.equals(s.getPhonetic())) { s.setPhonetic(p); changed = true; }
                            String ex = s.getExample() != null ? fixIfGarbled(s.getExample()) : null;
                            if (ex != null && !ex.equals(s.getExample())) { s.setExample(ex); changed = true; }
                            if (changed) {
                                seedMapper.updateById(s);
                                fixedCount++;
                            }
                            // 检测不可修复的损坏（含 U+FFFD）
                            if (hasReplacementChar(s.getMeaning()) || hasReplacementChar(s.getWord())) {
                                log.warn("MathSeedFixer: 种子 id={} word={} 已含U+FFFD（不可修复），需从SQL源文件重新导入", s.getId(), s.getWord());
                                corruptedCount++;
                            }
                        }
                        log.info("MathSeedFixer: 修复了 {} 条词汇种子, {} 条已损坏需重导", fixedCount, corruptedCount);
                    }
                }
            } catch (Exception ex) {
                log.error("MathSeedFixer: 词汇修复失败: {}", ex.getMessage(), ex);
            }
            vocabFixed = true;
        }

        // 修复 knowledge_nodes 编码
        if (!nodeFixed) {
            try {
                log.info("MathSeedFixer: 开始检查知识节点编码, nodeMapper={}", knowledgeNodeMapper);
                if (knowledgeNodeMapper == null) {
                    log.warn("MathSeedFixer: knowledgeNodeMapper is null, skipping node fix");
                } else {
                    List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(null);
                    log.info("MathSeedFixer: 知识节点总数={}", nodes != null ? nodes.size() : 0);
                    if (nodes != null && !nodes.isEmpty()) {
                        int fixedCount = 0, skipCount = 0;
                        for (KnowledgeNode n : nodes) {
                            String originalName = n.getName();
                            String fixedName = fixIfGarbled(originalName);
                            if (!fixedName.equals(originalName)) {
                                try {
                                    // 使用 LambdaUpdateWrapper 避免乐观锁 @Version 问题
                                    com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<KnowledgeNode> uw
                                        = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
                                    uw.eq(KnowledgeNode::getId, n.getId()).set(KnowledgeNode::getName, fixedName);
                                    knowledgeNodeMapper.update(uw);
                                    fixedCount++;
                                    log.info("MathSeedFixer: 修复节点 id={} name: {} -> {}", n.getId(),
                                        originalName.length() > 30 ? originalName.substring(0, 30) + "..." : originalName,
                                        fixedName.length() > 30 ? fixedName.substring(0, 30) + "..." : fixedName);
                                } catch (Exception dupEx) {
                                    skipCount++;
                                    log.warn("MathSeedFixer: 跳过节点 id={} (固定名冲突): {} -> {}",
                                        n.getId(), originalName.length() > 20 ? originalName.substring(0, 20) + "..." : originalName,
                                        fixedName.length() > 20 ? fixedName.substring(0, 20) + "..." : fixedName);
                                }
                            }
                        }
                        log.info("MathSeedFixer: 修复了 {} 个知识节点名称, 跳过 {} 个(名称冲突)", fixedCount, skipCount);
                    }
                }
            } catch (Exception ex) {
                log.error("MathSeedFixer: 知识节点修复失败: {}", ex.getMessage(), ex);
            }
            nodeFixed = true;
        }
    }

    /** 智能修复：纯双重编码用轮转法，混合编码逐段修复 */
    private static String fixIfGarbled(String text) {
        if (text == null || text.isEmpty()) return text;
        return PrecisionHelper.fixEncoding(text);
    }

    /** 检测是否含 U+FFFD 替换字符（表示数据已不可恢复） */
    private static boolean hasReplacementChar(String text) {
        if (text == null) return false;
        return text.indexOf('�') >= 0 || text.contains("�");
    }

    private void doImport() {
        int count = 0;
        // 构建知识点名称→nodeId映射（数学[职高]下 level=2 节点）
        Map<String, Long> nodeMap = buildMathNodeMap();
        for (String[] row : MATH_SEEDS) {
            QuestionBank q = new QuestionBank();
            q.setQuestionText(row[0]);
            q.setOptions(row[1]);
            q.setCorrectAnswer(row[2]);
            q.setExplanation(row[3]);
            q.setSubject(row[4]);
            q.setQuestionType(row[5]);
            q.setDifficultyLevel(Integer.parseInt(row[6]));
            q.setStatus(1);
            q.setVersion(1);
            q.setIsLatest(1);
            q.setSchoolId(1L);
            // 根据题目文本关键词匹配 categoryId
            String text = row[0];
            Long nodeId = matchCategoryId(text, nodeMap);
            if (nodeId != null) q.setCategoryId(nodeId);
            questionMapper.insert(q);
            count++;
        }
        log.info("MathSeedFixer: 成功导入 {} 道数学题（含categoryId匹配）", count);
    }

    /** 只导入缺失的题目（幂等：已存在的不重复导入） */
    private void doImportIfMissing() {
        Set<String> existingTexts = questionMapper.selectList(
            new LambdaQueryWrapper<QuestionBank>().eq(QuestionBank::getSubject, "数学[职高]"))
            .stream().map(q -> q.getQuestionText() != null ? q.getQuestionText().trim() : "")
            .collect(java.util.stream.Collectors.toSet());
        int count = 0;
        Map<String, Long> nodeMap = buildMathNodeMap();
        for (String[] row : MATH_SEEDS) {
            if (existingTexts.contains(row[0].trim())) continue;
            QuestionBank q = new QuestionBank();
            q.setQuestionText(row[0]);
            q.setOptions(row[1]);
            q.setCorrectAnswer(row[2]);
            q.setExplanation(row[3]);
            q.setSubject(row[4]);
            q.setQuestionType(row[5]);
            q.setDifficultyLevel(Integer.parseInt(row[6]));
            q.setStatus(1);
            q.setVersion(1);
            q.setIsLatest(1);
            q.setSchoolId(1L);
            Long nodeId = matchCategoryId(row[0], nodeMap);
            if (nodeId != null) q.setCategoryId(nodeId);
            questionMapper.insert(q);
            count++;
        }
        log.info("MathSeedFixer: 补充导入 {} 道数学题", count);
    }

    /** 修复已有题目缺失的 categoryId */
    private void fixMissingCategoryIds() {
        List<QuestionBank> nullCategoryQs = questionMapper.selectList(
            new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getSubject, "数学[职高]")
                .and(w -> w.isNull(QuestionBank::getCategoryId).or().eq(QuestionBank::getCategoryId, 0L))
                .last("LIMIT 500"));
        if (nullCategoryQs.isEmpty()) {
            log.info("MathSeedFixer: 所有数学题已有有效categoryId");
            return;
        }
        Map<String, Long> nodeMap = buildMathNodeMap();
        int fixed = 0;
        for (QuestionBank q : nullCategoryQs) {
            String text = q.getQuestionText();
            if (text == null || text.isEmpty()) continue;
            Long nodeId = matchCategoryId(text, nodeMap);
            if (nodeId != null) {
                q.setCategoryId(nodeId);
                questionMapper.updateById(q);
                fixed++;
            }
        }
        log.info("MathSeedFixer: 修复了 {} 道数学题的 categoryId（共 {} 道缺失）", fixed, nullCategoryQs.size());
    }

    /** 构建数学知识点 name → nodeId 映射 */
    private Map<String, Long> buildMathNodeMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        if (knowledgeNodeMapper == null) return map;
        Long mathSubjectId = getMathSubjectIdFromNodes();
        if (mathSubjectId == null) return map;
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getSubjectId, mathSubjectId));
        for (KnowledgeNode n : nodes) {
            if (n.getName() != null) {
                map.put(n.getName(), n.getId());
            }
        }
        return map;
    }

    /** 从 knowledge_nodes 表动态查找数学[职高]的 subjectId（避免硬编码 22L） */
    private Long getMathSubjectIdFromNodes() {
        if (knowledgeNodeMapper == null) return null;
        // 查找 level=1 且 name 含"数学"的节点
        List<KnowledgeNode> roots = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getLevel, 1)
                .like(KnowledgeNode::getName, "数学")
                .last("LIMIT 1"));
        if (!roots.isEmpty()) return roots.get(0).getSubjectId();
        // 兜底：硬编码 22L
        return 22L;
    }

    /** 获取数学[职高]的 L1 根节点 ID（动态查询，避免硬编码 10L） */
    private Long getMathRootNodeId() {
        Long mathSubjectId = getMathSubjectIdFromNodes();
        if (mathSubjectId == null || knowledgeNodeMapper == null) return 10L;
        List<KnowledgeNode> roots = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getSubjectId, mathSubjectId)
                .eq(KnowledgeNode::getLevel, 1)
                .last("LIMIT 1"));
        if (!roots.isEmpty()) return roots.get(0).getId();
        return 10L; // 终极兜底
    }

    /** 根据题目文本中的关键词匹配知识点 nodeId */
    private Long matchCategoryId(String questionText, Map<String, Long> nodeMap) {
        if (questionText == null || nodeMap.isEmpty()) return null;
        // 关键词 → 模块匹配
        String[][] patterns = {
            {"集合", "空集", "补集", "交集", "并集", "子集"},    // 集合
            {"不等式", "解集", "区间"},                          // 不等式
            {"函数", "定义域", "奇函数", "偶函数", "增函数", "二次函数", "顶点", "值域", "单调", "f("}, // 函数
            {"三角", "sin", "cos", "tan", "正弦", "余弦"},       // 三角函数
            {"数列", "等差", "等比", "公比", "公差", "通项"},     // 数列
            {"向量", "数量积", "共线", "|a|"},                   // 平面向量
            {"立体", "正方体", "圆柱", "圆锥", "球的", "球体", "体积", "表面积"}, // 立体几何
            {"解析", "直线", "圆", "点到直线", "距离公式", "斜率", "点斜式"}, // 平面解析几何
            {"概率", "统计", "平均数", "中位数", "必然事件"},     // 概率与统计
            {"导数", "f'(", "极值", "切线斜率", "单调递增区间"},  // 导数
            {"方程", "勾股", "x²", "计算正确"},                   // 初中补漏
        };
        for (int i = 0; i < patterns.length; i++) {
            for (String kw : patterns[i]) {
                if (questionText.contains(kw)) {
                    // 匹配到关键词，查找对应的 nodeId
                    for (var entry : nodeMap.entrySet()) {
                        if (entry.getKey().contains(kw) || kw.contains(entry.getKey())) {
                            return entry.getValue();
                        }
                    }
                }
            }
        }
        // 未匹配到：设为数学[职高] L1 根节点（动态查询）
        return getMathRootNodeId();
    }

    static String validateLatex(String text) {
        if (text == null || text.isEmpty()) return null;
        int dollarCount = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '$') {
                if (i > 0 && text.charAt(i - 1) == '\\') continue;
                dollarCount++;
            }
        }
        if (dollarCount % 2 != 0) return "$ 符号不配对（共 " + dollarCount + " 个，应为偶数）";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\$([^$]+)\\$").matcher(text);
        while (m.find()) {
            String latex = m.group(1);
            int depth = 0;
            for (char c : latex.toCharArray()) {
                if (c == '{') depth++;
                if (c == '}') depth--;
                if (depth < 0) return "LaTeX 中 } 多余 {：" + latex;
            }
            if (depth != 0) return "LaTeX 中 {} 不配对（差 " + depth + " 个）：" + latex;
        }
        if (text.contains("\\sqr") && !text.contains("\\sqrt")) return "疑似拼写错误：\\sqr 应为 \\sqrt";
        if (text.contains("\\fra") && !text.contains("\\frac")) return "疑似拼写错误：\\fra 应为 \\frac";
        return null;
    }

    static String fixLatexErrors(String text, Long questionId) {
        if (text == null || text.isEmpty()) return text;
        String original = text;
        // R112修复：处理数学模式外的 LaTeX 命令，避免 \qquad/\quad 等显示为原始文本
        // 将非 $ 内的 \ 命令包装为内联数学模式
        text = text.replaceAll("(?<!\\$)(\\\\qquad|\\\\quad|\\\\;|\\\\,|\\\\:|\\\\!)(?!\\$|[a-zA-Z])", "\\$$1\\$");
        text = text.replaceAll("\\\\sqr(?![a-zA-Z])", "\\\\sqrt");
        text = text.replaceAll("\\\\fra(?![a-zA-Z])", "\\\\frac");
        text = text.replaceAll("\\\\int_(?!\\{)", "\\\\int_{");
        text = text.replaceAll("\\\\sum_(?!\\{)", "\\\\sum_{");
        text = text.replaceAll("\\\\frac(?!\\{)", "\\\\frac{");
        int dollarCount = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '$' && (i == 0 || text.charAt(i - 1) != '\\')) dollarCount++;
        }
        if (dollarCount % 2 != 0) { text = text + "$"; log.warn("MathSeedFixer: 自动补全 $ 符号 qid={}", questionId); }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\$([^$]+)\\$").matcher(text);
        StringBuffer sb = new StringBuffer();
        boolean changed = false;
        while (m.find()) {
            String latex = m.group(1);
            int depth = 0;
            for (char c : latex.toCharArray()) {
                if (c == '{') depth++;
                if (c == '}') depth--;
            }
            if (depth > 0) {
                latex = latex + "}".repeat(depth);
                changed = true;
                log.warn("MathSeedFixer: 自动补全 {}  braces qid={} latex={}", depth, questionId, latex);
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("$" + latex + "$"));
        }
        if (changed) { m.appendTail(sb); text = sb.toString(); }
        if (!text.equals(original)) log.info("MathSeedFixer: 修复 LaTeX qid={}", questionId);
        return text;
    }
}
