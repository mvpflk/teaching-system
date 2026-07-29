package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.KnowledgeFlashcard;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.mapper.KnowledgeFlashcardMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.KnowledgeArticleMapper;
import com.school.teaching.entity.KnowledgeArticle;
import com.school.teaching.service.ExamSyllabusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 卡片质量评估器 — 异步调用 DeepSeek 对知识卡片进行多维度质量评分。
 * 触发时机：文章生成卡片后 / 教师手动触发 / 定时任务扫描 PENDING 卡片。
 */
@Slf4j
@Component
public class CardQualityEvaluator {

    @Autowired private DeepSeekGateway deepSeekGateway;
    @Autowired private KnowledgeFlashcardMapper flashcardMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private KnowledgeArticleMapper articleMapper;
    @Autowired(required = false) private ExamSyllabusService examSyllabusService;

    private static final ObjectMapper OM = new ObjectMapper();

    /** 批量 AI 评估，卡间间隔 500ms 防止打爆 API 触发熔断 */
    @Async
    public void batchEvaluate(List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) return;
        int total = 0, skipped = 0, evaluated = 0, circuitOpen = 0;
        for (Long cardId : cardIds) {
            try {
                KnowledgeFlashcard card = flashcardMapper.selectById(cardId);
                if (card == null) continue;
                total++;
                if (card.getQualityScore() != null && card.getQualityScore().compareTo(new BigDecimal("80")) >= 0) {
                    skipped++;
                    continue;
                }
                evaluateOne(card);
                evaluated++;
                // 卡间休息 500ms，避免触发 API 限流
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("熔断") || msg.contains("circuit") || msg.contains("不可用")) {
                    circuitOpen++;
                    log.warn("AI熔断，停止批量评估 (已评估{}张, 跳过{}张, 剩余{}张)", evaluated, skipped, cardIds.size() - total);
                    break;  // 熔断了就不再继续
                }
                log.warn("AI评估单张卡片失败 cardId={}", cardId, e);
            }
        }
        log.info("AI批量评估完成: total={} evaluated={} skipped={} circuitOpen={}", total, evaluated, skipped, circuitOpen);
    }

    /** 评估单张卡片 */
    public void evaluateOne(KnowledgeFlashcard card) {
        // v169: 一次性加载关联文章，避免 buildNodePath/getSyllabusContext/getNodeContent 各查一次
        KnowledgeArticle article = articleMapper.selectById(card.getArticleId());

        // 1. 获取知识点上下文（共用 article 避免 N+1）
        String nodePath = buildNodePath(article);
        String syllabusContext = getSyllabusContext(article);
        String nodeContent = getNodeContent(article);  // v169: 防超纲评估基准

        // 2. 构建 prompt
        String prompt = buildEvaluationPrompt(card, nodePath, syllabusContext, nodeContent);

        // 3. 调用 AI
        String response = deepSeekGateway.generateContentQuiet(Map.of(
            "prompt", prompt,
            "maxTokens", 800,
            "temperature", 0.3   // 低温度保证评估一致性
        ));

        // 4. 解析结果
        Map<String, Object> result = parseAiResult(response);
        if (result.isEmpty()) return;

        // 5. 写入卡片
        int totalScore = toInt(result.get("totalScore"), 0);
        card.setQualityScore(BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP));

        // 提取各维度分数（一次性计算，后续复用）
        int accuracyScore = toInt(result.get("accuracyScore"), 0);
        int completenessScore = toInt(result.get("completenessScore"), 0);
        int teachingScore = toInt(result.get("teachingScore"), 0);
        int alignmentScore = toInt(result.get("alignmentScore"), 0);
        int fidelityScore = toInt(result.get("fidelityScore"), 0); // v169: 内容保真度

        // 序列化 AI 评语为 JSON（保留分项分数字段）
        Map<String, Object> comment = new LinkedHashMap<>();
        comment.put("totalScore", totalScore);
        comment.put("accuracyScore", accuracyScore);
        comment.put("completenessScore", completenessScore);
        comment.put("teachingScore", teachingScore);
        comment.put("alignmentScore", alignmentScore);
        comment.put("fidelityScore", fidelityScore);
        comment.put("comment", Objects.toString(result.get("comment"), ""));
        comment.put("suggestion", Objects.toString(result.get("suggestion"), ""));
        comment.put("improvedVersion", Objects.toString(result.get("improvedVersion"), ""));
        try {
            card.setAiComment(OM.writeValueAsString(comment));
        } catch (Exception e) {
            card.setAiComment("{\"comment\":\"AI评语序列化失败\"}");
        }

        // v169: 审核状态 + 保真度门控
        // fidelityScore < 12 → 可能存在编造，强制PENDING待教师确认
        // totalScore >= 80 且 fidelity >= 12 → 自动通过
        if (totalScore >= 80 && fidelityScore >= 12) {
            card.setReviewStatus("APPROVED");
        } else {
            card.setReviewStatus("PENDING");
        }

        if (card.getContextPath() == null && !nodePath.isEmpty()) {
            card.setContextPath(nodePath);
        }

        flashcardMapper.updateById(card);
    }

    /** v169: 获取知识节点 content 作为评估基准（防编造） */
    private String getNodeContent(KnowledgeArticle article) {
        try {
            if (article == null || article.getNodeId() == null) return "";
            KnowledgeNode node = nodeMapper.selectById(article.getNodeId());
            return node != null && node.getContent() != null ? node.getContent() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** 构建知识点完整路径: 学科 > L2模块 > L3主题 > L4知识点 */
    private String buildNodePath(KnowledgeArticle article) {
        try {
            if (article == null || article.getNodeId() == null) return "";
            KnowledgeNode node = nodeMapper.selectById(article.getNodeId());
            if (node == null) return "";

            // 批量加载整棵树的所有节点，通过 parentId 构建路径（避免 N+1）
            Long subjectId = node.getSubjectId();
            if (subjectId == null) return node.getName();
            List<KnowledgeNode> allNodes = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getSubjectId, subjectId));
            Map<Long, KnowledgeNode> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, n -> n, (a, b) -> a));

            List<String> path = new ArrayList<>();
            KnowledgeNode current = node;
            for (int i = 0; i < 6 && current != null; i++) {
                path.add(0, current.getName());
                current = nodeMap.get(current.getParentId());
            }
            return String.join(" > ", path);
        } catch (Exception e) {
            return "";
        }
    }

    /** 获取考纲上下文描述 */
    private String getSyllabusContext(KnowledgeArticle article) {
        try {
            if (examSyllabusService == null) return "暂无考纲数据";
            if (article == null || article.getNodeId() == null) return "暂无考纲数据";
            // 找到该节点所属学科的 L1 根节点，exam_syllabus 按 subject_id 关联
            KnowledgeNode node = nodeMapper.selectById(article.getNodeId());
            if (node == null) return "暂无考纲数据";
            Long rootNodeId = findRootNodeId(node);
            if (rootNodeId == null) return "暂无考纲数据";
            var syllabi = examSyllabusService.getSyllabiByNodeId(rootNodeId);
            if (syllabi != null && !syllabi.isEmpty()) {
                var syllabus = syllabi.get(0);
                String text = syllabus.getSyllabusMeta();  // JSON 格式考纲元数据优先
                if (text == null || text.isBlank()) text = syllabus.getContent();  // 回退到 Markdown 全文
                return text != null ? text.substring(0, Math.min(300, text.length())) : "暂无考纲详情";
            }
            return "暂无考纲数据";
        } catch (Exception e) {
            return "暂无考纲数据";
        }
    }

    /** 向上递归查找学科的 L1 根节点 */
    private Long findRootNodeId(KnowledgeNode node) {
        KnowledgeNode current = node;
        for (int i = 0; i < 5 && current != null; i++) {
            if (current.getLevel() != null && current.getLevel() == 1) return current.getId();
            if (current.getParentId() == null) break;
            current = nodeMapper.selectById(current.getParentId());
        }
        return null;
    }

    /** v169: 构建 AI 评估 prompt — 5维度评估 + 节点content为防编造基准 */
    private String buildEvaluationPrompt(KnowledgeFlashcard card, String nodePath,
                                          String syllabusContext, String nodeContent) {
        return String.format("""
            你是一位教学质量管理专家。请评估以下知识卡片的质量。

            知识点路径：%s
            考纲要求：%s
            知识节点内容（准确的基准答案，用于检测卡片是否编造了超出此范围的内容）：%s
            卡片类型：%s
            卡片正面：%s
            卡片背面：%s

            请从以下维度评分（各20分，满分100）：
            1. 准确性(20分)：答案是否与知识节点内容一致，无知识性错误
            2. 完整性(20分)：是否覆盖了知识节点内容中的核心要点
            3. 教学性(20分)：表述是否适合中职学生理解，避免"差""不及格"等否定词
            4. 对齐性(20分)：内容是否在考纲范围内
            5. 内容保真度(20分)：卡片内容是否严格来自知识节点内容，未自行编造、扩展或超纲

            返回纯JSON（不要markdown代码块）：
            {"totalScore":85,"accuracyScore":17,"completenessScore":16,"teachingScore":17,"alignmentScore":17,"fidelityScore":18,"comment":"评语","suggestion":"改进建议","improvedVersion":"改写后的背面内容"}
            """,
            nodePath.isEmpty() ? "未知" : nodePath,
            syllabusContext,
            nodeContent.isEmpty() ? "暂无基准内容" : nodeContent,
            card.getCardType() != null ? card.getCardType() : "DEFINITION",
            card.getFrontText() != null ? card.getFrontText() : "",
            card.getBackText() != null ? card.getBackText() : "");
    }

    /** 解析 AI 返回的 JSON */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAiResult(String response) {
        if (response == null || response.isBlank()) return Map.of();
        try {
            int start = response.indexOf('{'), end = response.lastIndexOf('}');
            if (start >= 0 && end > start) response = response.substring(start, end + 1);
            return OM.readValue(response, Map.class);
        } catch (Exception e) {
            log.warn("AI评估结果解析失败: {}", response.substring(0, Math.min(100, response.length())), e);
            return Map.of();
        }
    }

    private static int toInt(Object val, int def) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) { try { return Integer.parseInt(s); } catch (Exception e) {} }
        return def;
    }
}
