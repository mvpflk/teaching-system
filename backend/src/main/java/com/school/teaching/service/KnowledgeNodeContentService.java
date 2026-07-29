package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.ExamSyllabus;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.mapper.ExamSyllabusMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.service.impl.DeepSeekGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识节点内容填充服务 — 迭代〇核心引擎。
 * 用 DeepSeek 为空白 knowledge_nodes.content 批量生成结构化教学内容，
 * 严格对标四川职高对口升学考纲。
 *
 * <p>使用方式：
 * <pre>
 *   // 预览模式（不写库）
 *   var preview = service.generateContent(1L, true, 5);
 *
 *   // 正式生成
 *   var result = service.generateContent(1L, false, 10);
 * </pre>
 */
@Slf4j
@Service
public class KnowledgeNodeContentService {

    private static final int BATCH_SIZE = 1;           // 每批 1 个节点（保证 JSON 稳定）
    private static final int MAX_TOKENS = 4096;        // API max_tokens
    private static final double TEMPERATURE = 0.3;     // 低温度保证准确性
    private static final String MODEL = "deepseek-v4-pro";

    private final KnowledgeNodeMapper nodeMapper;
    private final ExamSyllabusMapper syllabusMapper;
    private final DeepSeekGateway gateway;
    private final ObjectMapper om = new ObjectMapper();

    public KnowledgeNodeContentService(KnowledgeNodeMapper nodeMapper,
                                       ExamSyllabusMapper syllabusMapper,
                                       @Qualifier("deepSeekGateway") DeepSeekGateway gateway) {
        this.nodeMapper = nodeMapper;
        this.syllabusMapper = syllabusMapper;
        this.gateway = gateway;
    }

    // ═══════════════ 主入口 ═══════════════

    /**
     * 为指定学科生成知识节点内容。
     * @param subjectId  学科 ID（1=数学 4=语文 5=英语）
     * @param preview    true=仅返回生成结果不写库
     * @param maxNodes   限制处理节点数（null=全部）
     * @return 生成报告
     */
    public Map<String, Object> generateContent(Long subjectId, boolean preview, Integer maxNodes) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("subjectId", subjectId);
        report.put("preview", preview);

        // 1. 查询空 content 的 L4 节点
        List<KnowledgeNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getSubjectId, subjectId)
                        .eq(KnowledgeNode::getLevel, 4)
                        .and(w -> w.isNull(KnowledgeNode::getContent)
                                 .or().eq(KnowledgeNode::getContent, ""))
                        .and(w -> w.eq(KnowledgeNode::getStatus, "ACTIVE")
                                 .or().eq(KnowledgeNode::getStatus, "1")
                                 .or().isNull(KnowledgeNode::getStatus))
                        .orderByAsc(KnowledgeNode::getSortOrder));

        if (nodes.isEmpty()) {
            report.put("message", "该学科无空内容节点");
            report.put("totalNodes", 0);
            return report;
        }

        int limit = maxNodes != null ? Math.min(maxNodes, nodes.size()) : nodes.size();
        List<KnowledgeNode> batch = nodes.subList(0, limit);
        report.put("totalNodes", nodes.size());
        report.put("processCount", batch.size());

        // 2. 查询考纲上下文
        String syllabusContext = loadSyllabusContext(subjectId);

        // 3. 分批生成
        int generated = 0, failed = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (int i = 0; i < batch.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, batch.size());
            List<KnowledgeNode> slice = batch.subList(i, end);

            try {
                List<Map<String, Object>> results = processBatch(slice, subjectId, syllabusContext, preview);
                for (Map<String, Object> r : results) {
                    boolean ok = Boolean.TRUE.equals(r.get("success"));
                    if (ok) generated++; else failed++;
                    details.add(r);
                }
                log.info("ContentGen: subjectId={}, batch {}/{} done, {} ok, {} fail",
                        subjectId, (i / BATCH_SIZE) + 1, (batch.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                        generated, failed);
            } catch (Exception e) {
                log.error("ContentGen batch failed: subjectId={}, range=[{},{}]", subjectId, i, end, e);
                for (KnowledgeNode n : slice) {
                    failed++;
                    details.add(Map.of("nodeId", n.getId(), "nodeName", n.getName(),
                            "success", false, "error", e.getMessage()));
                }
            }
        }

        report.put("generated", generated);
        report.put("failed", failed);
        report.put("details", details);
        report.put("elapsedMs", System.currentTimeMillis() - startTime);
        return report;
    }

    /**
     * 增强现有内容 — 在已有「定义+说明+常见错」基础上，追加例题、考法、教材出处。
     * 仅用于数学[职高]（subjectId=22），其他学科用 generateContent。
     */
    public Map<String, Object> enhanceContent(Long subjectId, boolean preview, Integer maxNodes) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("subjectId", subjectId);
        report.put("preview", preview);

        // 查询有内容但缺少补充的 L4 节点，或内容为空的节点
        List<KnowledgeNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getSubjectId, subjectId)
                        .eq(KnowledgeNode::getLevel, 4)
                        .and(w -> w.isNull(KnowledgeNode::getContent)
                                 .or().eq(KnowledgeNode::getContent, "")
                                 .or().notLike(KnowledgeNode::getContent, "经典例题"))
                        .and(w -> w.eq(KnowledgeNode::getStatus, "ACTIVE")
                                 .or().eq(KnowledgeNode::getStatus, "1")
                                 .or().isNull(KnowledgeNode::getStatus))
                        .orderByAsc(KnowledgeNode::getSortOrder));

        if (nodes.isEmpty()) {
            report.put("message", "该学科无待增强节点（可能已全部包含例题）");
            report.put("totalNodes", 0);
            return report;
        }

        int limit = maxNodes != null ? Math.min(maxNodes, nodes.size()) : nodes.size();
        List<KnowledgeNode> batch = nodes.subList(0, limit);
        report.put("totalNodes", nodes.size());
        report.put("processCount", batch.size());

        // 查询考纲上下文
        String syllabusContext = loadSyllabusContext(subjectId);

        // 逐批增强
        int enhanced = 0, failed = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (int i = 0; i < batch.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, batch.size());
            List<KnowledgeNode> slice = batch.subList(i, end);

            try {
                List<Map<String, Object>> results = processEnhanceBatch(slice, subjectId, syllabusContext, preview);
                for (Map<String, Object> r : results) {
                    if (Boolean.TRUE.equals(r.get("success"))) enhanced++; else failed++;
                    details.add(r);
                }
                log.info("ContentEnhance: batch {}/{} done, {} ok, {} fail",
                        (i / BATCH_SIZE) + 1, (batch.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                        enhanced, failed);
            } catch (Exception e) {
                log.error("ContentEnhance batch failed: range=[{},{}]", i, end, e);
                for (KnowledgeNode n : slice) {
                    failed++;
                    details.add(Map.of("nodeId", n.getId(), "nodeName", n.getName(),
                            "success", false, "error", e.getMessage()));
                }
            }
        }

        report.put("enhanced", enhanced);
        report.put("failed", failed);
        report.put("details", details);
        report.put("elapsedMs", System.currentTimeMillis() - startTime);
        return report;
    }

    /** 增强批次 v3：Markdown 直出，零 JSON 解析 */
    private List<Map<String, Object>> processEnhanceBatch(List<KnowledgeNode> nodes, Long subjectId,
                                                          String syllabusContext, boolean preview) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (KnowledgeNode node : nodes) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("nodeId", node.getId());
            result.put("nodeName", node.getName());
            result.put("path", resolveNodePath(node.getId()));

            try {
                String prompt = buildSingleEnhancePrompt(node, syllabusContext);
                String supplement = callDeepSeek(prompt);

                if (supplement == null || supplement.isBlank()) {
                    result.put("success", false);
                    result.put("error", "DeepSeek 返回空");
                    results.add(result);
                    continue;
                }

                // 清理：去掉可能的 markdown 代码块包裹和多余空白
                supplement = cleanMarkdownResponse(supplement);
                if (supplement.length() < 50) {
                    result.put("success", false);
                    result.put("error", "补充内容过短（" + supplement.length() + " 字）");
                    results.add(result);
                    continue;
                }

                String original = node.getContent() != null ? node.getContent() : "";
                String merged = original.isEmpty() ? supplement : original + "\n\n" + supplement;

                if (!preview) {
                    KnowledgeNode update = new KnowledgeNode();
                    update.setId(node.getId());
                    update.setContent(merged);
                    nodeMapper.updateById(update);
                }

                result.put("success", true);
                result.put("contentLength", merged.length());
                result.put("originalLength", node.getContent() != null ? node.getContent().length() : 0);
                log.info("ContentEnhance node {} ok: {} → {} 字", node.getId(),
                        result.get("originalLength"), result.get("contentLength"));
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
                log.warn("ContentEnhance node {} failed: {}", node.getId(), e.getMessage());
            }
            results.add(result);
        }

        return results;
    }

    /** 单个节点的增强 prompt——按学科分派不同模板 */
    private String buildSingleEnhancePrompt(KnowledgeNode node, String syllabus) {
        Long subjectId = node.getSubjectId();
        boolean hasContent = node.getContent() != null && !node.getContent().isBlank();
        if (subjectId != null && subjectId == 48) {
            return buildChinesePrompt(node, syllabus, hasContent);
        }
        if (subjectId != null && subjectId == 24) {
            return buildEnglishEnhancePrompt(node, syllabus);
        }
        return buildMathEnhancePrompt(node, syllabus);
    }

    /** 语文教材同步——生成结构化教学内容 */
    private String buildChinesePrompt(KnowledgeNode node, String syllabus, boolean hasContent) {
        String existingHint = hasContent
                ? "现有内容：" + (node.getContent().length() > 300 ? node.getContent().substring(0, 300) : node.getContent())
                : "（新节点，无现有内容，请生成完整教学内容）";
        String outputFormat = hasContent
                ? "补充以下缺失板块（保留已有内容不动）"
                : "生成完整教学内容，按以下结构输出";

        return String.format("""
                你是四川职高语文教研专家。为以下课文生成结构化教学内容，严格对标职高语文考纲。

                ## 课文信息
                - 名称：%s
                - 路径：%s
                - %s

                ## %s
                【教学背景】
                （写作时间、作者处境、时代背景，50-80字）

                【教学目标】
                1. 知识目标：（具体可检测）
                2. 能力目标：（具体可操作）
                3. 素养目标：（思政/人文/审美）

                【重点字词】
                - 字词1（注音+释义）
                - 字词2（注音+释义）
                - 字词3（注音+释义）
                （列出5-8个重点字词）

                【写作手法】
                1. （手法名称）：引用原文+简要分析
                2. （手法名称）：引用原文+简要分析

                【练习方向】
                1. （关联考纲能力点1）
                2. （关联考纲能力点2）

                ## 硬性约束
                - 知识必须准确，来不得半点马虎。不确定的内容宁可少写
                - 字词注音用汉语拼音，不用国际音标
                - 写作手法要引用课文原文，不可凭空编造
                - 只输出上面几个板块，不要 JSON
                """,
                node.getName(), resolveNodePath(node.getId()), existingHint,
                outputFormat);
    }

    /** 数学增强 prompt */
    private String buildMathEnhancePrompt(KnowledgeNode node, String syllabus) {
        return String.format("""
                你是四川职高对口升学数学教研专家。补充「经典例题+考试考法+教材出处」，直接输出 Markdown。

                ## 知识点：%s | 路径：%s
                现有内容：%s
                ## 考纲：%s

                ## 输出格式（严格，不要 JSON）
                【经典例题】
                **例1** （LaTeX）\\n解：①…②…③…（推到答案）
                \\n\\n**例2** （LaTeX，不同角度）\\n解：①…②…（推到答案）
                【考试考法】
                （30-60字，题型+易错）
                【教材出处】
                基础模块上/下册 第X章 §X.X，P页码

                ## 约束：所有数学符号用 $...$、例题用具体数字、只输出三板块
                """,
                node.getName(), resolveNodePath(node.getId()),
                node.getContent() != null && node.getContent().length() > 300
                        ? node.getContent().substring(0, 300) : node.getContent(),
                syllabus);
    }

    /** 英语增强 prompt——补充常见错+自测题+考法 */
    private String buildEnglishEnhancePrompt(KnowledgeNode node, String syllabus) {
        return String.format("""
                你是四川职高对口升学英语教研专家。为以下语法知识点补充缺失内容，直接输出 Markdown：

                ## 知识点
                - 名称：%s
                - 路径：%s
                - 现有内容：%s

                ## 考纲
                %s

                ## 输出格式（严格，直接 Markdown）
                【常见错误】
                1. （学生最容易犯的错误及纠正）
                2. （第二个常见错误及纠正）
                3. （第三个常见错误及纠正，如有）

                【即时自测】
                1. （选择题题干，4个选项A/B/C/D）
                答案：（正确选项+解析，20-40字）

                2. （选择题题干，4个选项A/B/C/D）
                答案：（正确选项+解析，20-40字）

                【考试考法】
                （30-60字，对口升学常考题型+注意事项）

                ## 硬性约束
                - 自测题风格对齐四川职高对口升学英语真题（词汇量≤2000）
                - 只输出上面三个板块，不要任何额外解释
                - 不要 JSON、不要代码块包裹
                """,
                node.getName(), resolveNodePath(node.getId()),
                node.getContent() != null && node.getContent().length() > 300
                        ? node.getContent().substring(0, 300) : node.getContent(),
                syllabus);
    }

    /** 清理 DeepSeek 的 Markdown 响应 */
    private String cleanMarkdownResponse(String raw) {
        String text = raw.trim();
        // 去掉 markdown 代码块
        if (text.startsWith("```")) {
            int end = text.indexOf("```", 3);
            if (end > 3) text = text.substring(3, end).trim();
            if (text.startsWith("markdown")) text = text.substring(8).trim();
        }
        // 如果以 【经典例题】 开头，说明是正确的 markdown 格式
        return text;
    }

    /** 构建节点列表文本（传完整现有内容，让 DeepSeek 理解已有结构） */
    private String buildEnhanceNodeList(List<Map<String, Object>> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> n : nodes) {
            sb.append("### [nodeId=").append(n.get("nodeId"))
              .append("] ").append(n.get("path")).append("\n");
            String existing = (String) n.get("existingContent");
            if (existing != null && !existing.isEmpty()) {
                sb.append("**现有内容**：\n").append(existing).append("\n");
            }
            sb.append("**需要补充**：2道分步例题 + 考试考法提示 + 教材出处\n\n");
        }
        return sb.toString();
    }

    /** 增强 prompt v2 — 完整现有内容 + 严格质量约束 */
    private String buildEnhancePrompt(String subjectName, String nodeList, String syllabus) {
        return String.format("""
                你是四川省职高对口升学考试的数学教研专家。以下节点已有定义和自测题，请为每个生成缺失部分。

                ## 考纲要求
                %s

                ## 待增强节点
                %s

                ## 每个节点输出一个 JSON：
                {
                  "nodeId": 数字,
                  "examples": "**例1** 题目（LaTeX）\\n解：第1步… 第2步… 第3步…（完整推到答案）\\n\\n**例2** 题目（LaTeX·与例1不同角度）\\n解：第1步… 第2步…（完整推到答案）",
                  "examTips": "常见考法和易错提醒，30-60字。如：选择题常考用通项公式求指定项，注意区分a_n与a_m的下标",
                  "textbookRef": "教材出处：基础模块上/下册 第X章 §X.X，P起-止"
                }

                ## 例题质量铁律
                1. 每道例题必须分步，至少3步，标注步骤编号①②③
                2. 用具体数字，不用字母参数。例1偏基础、例2多一步推导
                3. 计算结果必须验证，不确定的不要写
                4. 不可与已有「即时自测」题目重复

                ## LaTeX 铁律（每条必须遵守）
                1. 任何数学符号必须用 $...$ 包裹：$a_n$、$q \\\\neq 0$、$\\\\{a_n\\\\}$
                2. 独立公式用 $$...$$，如 $$a_n=a_1 \\\\cdot q^{n-1}$$
                3. 集合用 $\\\\{x \\\\mid x>0\\\\}$，区间用 $[1,+\\\\infty)$
                4. 分数用 $\\\\frac{}{}$，根号用 $\\\\sqrt{}$
                5. 希腊字母不加$：α→$\\\\alpha$、β→$\\\\beta$
                6. 度数符号：$420^\\\\circ$、$-150^\\\\circ$

                ## 硬性约束
                - 只输出 JSON 数组，不要任何解释性文字
                - 教材出处参考四川职高数学教材（高教社·基础模块上/下册）
                - 不确定的教材页码标注"教材出处待确认"
                """,
                syllabus, nodeList);
    }

    /** 将 DeepSeek 返回的 JSON 补充内容转换为 Markdown */
    private String formatSupplement(String supplementJson) {
        try {
            Map<String, Object> s = om.readValue(supplementJson, new TypeReference<>() {});
            StringBuilder md = new StringBuilder();

            String examples = (String) s.get("examples");
            if (examples != null && !examples.isBlank()) {
                md.append("【经典例题】\n").append(examples).append("\n");
            }

            String tips = (String) s.get("examTips");
            if (tips != null && !tips.isBlank()) {
                md.append("\n【考试考法】\n").append(tips).append("\n");
            }

            String ref = (String) s.get("textbookRef");
            if (ref != null && !ref.isBlank()) {
                md.append("\n【教材出处】\n").append(ref).append("\n");
            }

            return md.toString();
        } catch (Exception e) {
            log.warn("格式化补充内容失败: {}", e.getMessage());
            return null;
        }
    }

    // ═══════════════ 分批处理 ═══════════════

    private List<Map<String, Object>> processBatch(List<KnowledgeNode> nodes, Long subjectId,
                                                    String syllabusContext, boolean preview) {
        // 加载节点路径
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (KnowledgeNode n : nodes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nodeId", n.getId());
            item.put("nodeName", n.getName());
            item.put("path", resolveNodePath(n.getId()));
            enriched.add(item);
        }

        // 构建学科 prompt
        String nodeListText = buildNodeListText(enriched);
        String subjectName = resolveSubjectName(subjectId);
        String prompt = buildPrompt(subjectId, subjectName, nodeListText, syllabusContext);

        // 调用 DeepSeek
        String rawJson = callDeepSeek(prompt);
        if (rawJson == null) {
            return enriched.stream().map(m -> {
                m.put("success", false);
                m.put("error", "DeepSeek API 调用失败");
                return m;
            }).collect(Collectors.toList());
        }

        // 解析返回的 JSON 数组
        List<Map<String, Object>> results = parseResponse(rawJson, enriched);
        if (results == null) {
            return enriched.stream().map(m -> {
                m.put("success", false);
                m.put("error", "响应解析失败（非 JSON 数组）");
                m.put("rawResponse", rawJson.substring(0, Math.min(500, rawJson.length())));
                return m;
            }).collect(Collectors.toList());
        }

        // 校验 & 写入
        for (Map<String, Object> r : results) {
            Long nodeId = r.get("nodeId") instanceof Number n ? n.longValue() : null;
            String content = (String) r.get("content");
            if (nodeId == null || content == null || content.isBlank()) {
                r.put("success", false);
                r.put("error", "content 为空");
                continue;
            }

            String validation = validateContent(subjectId, content);
            if (validation != null) {
                r.put("success", false);
                r.put("error", "校验失败: " + validation);
                continue;
            }

            if (!preview) {
                KnowledgeNode node = new KnowledgeNode();
                node.setId(nodeId);
                node.setContent(content);
                nodeMapper.updateById(node);
            }
            r.put("success", true);
            r.put("contentLength", content.length());
        }

        return results;
    }

    // ═══════════════ Prompt 构建 ═══════════════

    private String buildPrompt(Long subjectId, String subjectName, String nodeList, String syllabus) {
        String subjectBlock = switch (subjectId.intValue()) {
            case 22 -> buildMathTemplate();
            case 20 -> buildChineseTemplate();
            case 24 -> buildEnglishTemplate();
            default -> buildGenericTemplate();
        };

        return String.format("""
                你是四川省职业高中对口升学考试的教研专家，精通%s的考纲和教学要求。
                请为以下知识节点生成结构化教学内容，严格对标四川职高学段水平。

                ## 考纲要求
                %s

                ## 需要生成内容的节点
                %s

                ## 输出要求
                %s

                ## 硬性约束
                - 知识必须准确，来不得半点马虎。不确定的内容宁可少写，不要编造
                - 语言简洁、适合职高学生理解水平
                - 数学公式必须用 LaTeX：行内 $...$，独立公式 $$...$$
                - 输出必须是合法的 JSON 数组，每个元素对应一个节点
                - 不要输出任何 JSON 之外的文字
                """,
                subjectName, syllabus, nodeList, subjectBlock);
    }

    private String buildMathTemplate() {
        return """
                对每个节点输出一个 JSON 对象，包含以下字段：
                {
                  "nodeId": 数字,
                  "definition": "教材标准定义，1-2句，核心公式用LaTeX",
                  "example1": {"question": "经典例题题干（LaTeX格式）", "solution": "完整解题步骤（LaTeX格式）"},
                  "example2": {"question": "变体或进阶例题（LaTeX格式）", "solution": "完整解题步骤（LaTeX格式）"},
                  "commonMistakes": ["学生最常见错误1", "学生最常见错误2"],
                  "examTips": "考试中常见的考法和注意事项，20-40字"
                }

                职高数学难度参考：
                - 集合/不等式：初中到高一衔接水平
                - 函数：理解定义域值域、二次函数图像即可
                - 三角函数：重点在正弦余弦定理的应用，不涉及复杂恒等变换
                - 数列：等差/等比数列的通项与求和
                - 解析几何：直线与圆的方程
                - 立体几何：体积表面积公式应用
                - 导数：仅多项式求导
                """;
    }

    private String buildChineseTemplate() {
        return """
                对每个节点输出一个 JSON 对象，包含以下字段：
                {
                  "nodeId": 数字,
                  "background": "课文写作背景或作者简介，50-80字",
                  "objectives": ["教学目标1", "教学目标2", "教学目标3"],
                  "keyWords": [{"word":"重点字词1","pinyin":"注音","meaning":"释义"}, ...],
                  "writingTechniques": ["写作手法1：引用原文+简要分析", "写作手法2：引用原文+简要分析"],
                  "practiceDirection": ["练习方向1（关联考纲能力点）", "练习方向2"]
                }

                四川职高语文考纲重点：
                - 现代文阅读：提取信息、推断含义、赏析语言表达
                - 文言文阅读：常见实词/虚词理解、句子翻译、内容概括
                - 写作手法：借景抒情、托物言志、对比衬托、铺垫照应
                - 教学目标按三维写法：知识/能力/素养
                """;
    }

    private String buildEnglishTemplate() {
        return """
                对每个节点输出一个 JSON 对象，包含以下字段：
                {
                  "nodeId": 数字,
                  "rule": "语法规则精炼描述，2-3句",
                  "exampleSentences": [
                    {"en":"英语例句","cn":"中文翻译"},
                    {"en":"英语例句","cn":"中文翻译"}
                  ],
                  "commonMistake": "学生最容易犯的一个错误及纠正",
                  "examTips": "考试中常见的考法，20-40字"
                }

                四川职高英语：
                - 词汇量 ≤ 2000，例句用词在此范围内
                - 语法覆盖：时态语态/非谓语/从句/主谓一致/情态动词/虚拟语气/情景交际
                - 考查形式：单选+完形+阅读+翻译+写作
                """;
    }

    private String buildGenericTemplate() {
        return """
                对每个节点输出一个 JSON 对象，包含以下字段：
                {
                  "nodeId": 数字,
                  "definition": "核心概念定义",
                  "keyPoints": ["要点1", "要点2", "要点3"],
                  "commonMistakes": ["常见错误1"],
                  "examTips": "考法提示"
                }
                """;
    }

    // ═══════════════ 考纲加载 ═══════════════

    private String loadSyllabusContext(Long subjectId) {
        try {
            List<ExamSyllabus> syllabi = syllabusMapper.selectList(
                    new LambdaQueryWrapper<ExamSyllabus>()
                            .eq(ExamSyllabus::getSubjectId, subjectId)
                            .last("LIMIT 5"));
            if (syllabi.isEmpty()) return "暂无考纲数据。";

            StringBuilder sb = new StringBuilder();
            for (ExamSyllabus s : syllabi) {
                if (s.getContent() != null && !s.getContent().isBlank()) {
                    String brief = s.getContent().length() > 600
                            ? s.getContent().substring(0, 600).replace('\n', ' ') + "…"
                            : s.getContent().replace('\n', ' ');
                    sb.append("- ").append(brief).append("\n");
                }
            }
            return sb.length() > 0 ? sb.toString() : "暂无考纲数据。";
        } catch (Exception e) {
            log.warn("加载考纲失败: subjectId={}", subjectId, e);
            return "考纲数据加载失败。";
        }
    }

    // ═══════════════ 节点路径解析 ═══════════════

    /** 构建 L1→L2→L3→L4 的完整路径 */
    private String resolveNodePath(Long nodeId) {
        List<String> names = new ArrayList<>();
        Long current = nodeId;
        int safety = 0;
        while (current != null && safety < 5) {
            KnowledgeNode node = nodeMapper.selectById(current);
            if (node == null) break;
            names.add(0, node.getName());
            current = node.getParentId();
            safety++;
        }
        return String.join(" > ", names);
    }

    /** 构建节点列表文本（路径 + 名称） */
    private String buildNodeListText(List<Map<String, Object>> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> n : nodes) {
            sb.append("- [nodeId=").append(n.get("nodeId"))
              .append("] ").append(n.get("path")).append("\n");
        }
        return sb.toString();
    }

    private String resolveSubjectName(Long subjectId) {
        return switch (subjectId.intValue()) {
            case 22 -> "数学[职高]";
            case 20 -> "语文[职高]";
            case 24 -> "英语[职高]";
            case 31 -> "农学[职高]";
            case 43 -> "幼儿卫生与保健";
            case 44 -> "幼儿照护";
            case 45 -> "幼儿早期学习支持";
            case 46 -> "幼儿发展心理基础";
            case 47 -> "保教政策法规与职业道德";
            default -> "学科(id=" + subjectId + ")";
        };
    }

    // ═══════════════ DeepSeek 调用 ═══════════════

    private String callDeepSeek(String prompt) {
        try {
            List<Map<String, Object>> messages = List.of(
                    Map.of("role", "user", "content", prompt)
            );
            Map<String, Object> result = gateway.callWithTools(
                    messages,
                    Collections.emptyList(),
                    TEMPERATURE,
                    MAX_TOKENS,
                    null, null, MODEL, null         // apiKey, baseUrl, model, userId
            );
            String body = (String) result.get("body");
            if (body == null || body.isBlank()) {
                log.warn("DeepSeek 返回空 body");
                return null;
            }
            // 从响应中提取 assistant content
            return extractContent(body);
        } catch (Exception e) {
            log.error("DeepSeek 调用失败", e);
            return null;
        }
    }

    /** 从 DeepSeek 响应 JSON 中提取 assistant 的 content 字段 */
    private String extractContent(String responseBody) {
        try {
            Map<String, Object> resp = om.readValue(responseBody, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            if (msg == null) return null;
            return (String) msg.get("content");
        } catch (Exception e) {
            log.debug("解析 DeepSeek 响应失败: {}", e.getMessage());
            return null;
        }
    }

    // ═══════════════ 响应解析 ═══════════════

    /** 解析 DeepSeek 返回的 JSON 数组 → 提取 content 字段（稳健版） */
    private List<Map<String, Object>> parseResponse(String rawText, List<Map<String, Object>> enriched) {
        String json = extractJsonArray(rawText);
        if (json == null) {
            log.warn("无法从响应中提取 JSON 数组");
            return null;
        }

        try {
            List<Map<String, Object>> parsed = om.readValue(json, new TypeReference<>() {});
            Map<Long, Map<String, Object>> enrichedMap = new LinkedHashMap<>();
            for (Map<String, Object> e : enriched) {
                Long nid = e.get("nodeId") instanceof Number n ? n.longValue() : null;
                if (nid != null) enrichedMap.put(nid, e);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> item : parsed) {
                Long nodeId = item.get("nodeId") instanceof Number n ? n.longValue() : null;
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("nodeId", nodeId);

                Map<String, Object> meta = nodeId != null ? enrichedMap.get(nodeId) : null;
                if (meta != null) {
                    result.put("nodeName", meta.get("nodeName"));
                    result.put("path", meta.get("path"));
                }

                Map<String, Object> contentMap = new LinkedHashMap<>(item);
                contentMap.remove("nodeId");
                String content = om.writeValueAsString(contentMap);
                result.put("content", content);
                results.add(result);
            }
            return results;
        } catch (Exception e) {
            log.warn("JSON 解析失败: {} | raw前200字: {}", e.getMessage(),
                    json.length() > 200 ? json.substring(0, 200) : json);
            return null;
        }
    }

    /** 从 DeepSeek 响应中提取 JSON 数组，处理 markdown 包裹、多余文字等 */
    private String extractJsonArray(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String text = raw.trim();

        // 1. 去掉 markdown 代码块 ```json ... ``` 或 ``` ... ```
        if (text.startsWith("```")) {
            int end = text.indexOf("```", 3);
            if (end > 3) {
                text = text.substring(3, end).trim();
                // 去掉语言标记 "json"
                if (text.startsWith("json\n") || text.startsWith("json\r\n")) {
                    text = text.substring(4).trim();
                }
            }
        }

        // 2. 找到第一个 [ 和最后一个 ]
        int arrStart = text.indexOf('[');
        int arrEnd = text.lastIndexOf(']');
        if (arrStart < 0 || arrEnd <= arrStart) return null;
        text = text.substring(arrStart, arrEnd + 1);

        // 3. 修复常见 JSON 问题：去除尾部逗号
        text = text.replaceAll(",\\s*]", "]");
        text = text.replaceAll(",\\s*}", "}");

        return text;
    }

    // ═══════════════ 内容校验 ═══════════════

    /** 校验生成的内容质量。返回 null 表示通过，否则返回错误描述。 */
    private String validateContent(Long subjectId, String content) {
        if (content == null || content.isBlank()) return "内容为空";

        int len = content.length();
        if (subjectId == 1) { // 数学
            if (len < 100) return "数学内容过短（" + len + " 字）";
            if (len > 2000) return "数学内容过长（" + len + " 字）";
            // LaTeX 检测：至少有一对 $...$ 或 $$...$$
            if (!content.contains("$")) return "数学内容缺少 LaTeX 公式";
        } else if (subjectId == 4 || subjectId == 6) { // 语文
            if (len < 150) return "语文内容过短（" + len + " 字）";
            if (len > 3000) return "语文内容过长（" + len + " 字）";
        } else if (subjectId == 5) { // 英语
            if (len < 80) return "英语内容过短（" + len + " 字）";
            if (len > 1500) return "英语内容过长（" + len + " 字）";
        }

        return null; // 校验通过
    }
}
