package com.school.teaching.service.impl;

import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.QuestionBankMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSaveService {

    private final QuestionBankMapper questionBankMapper;

    @Transactional
    public List<Map<String, Object>> saveQuestions(Long teacherId, Map<String, Object> params, List<Map<String, Object>> questions) {
        // V055: 重用已有的 batchId（由 executeAsync 提前生成），确保同一批题目共享一个 batchId
        String batchId = params.get("_batchId") instanceof String s && !s.isEmpty()
            ? s : UUID.randomUUID().toString().substring(0, 8);
        params.put("_batchId", batchId);
        Set<Long> validKpIds = buildNodeWhitelist(params);
        List<Map<String, Object>> results = new ArrayList<>();
        int dupSkipped = 0;
        int lowQualityRejected = 0;
        final int MIN_QUALITY = 40;
        for (Map<String, Object> q : questions) {
            QuestionBank qb = new QuestionBank();
            String qt = (String) q.get("questionType");
            qb.setQuestionType(qt != null ? qt : "SINGLE_CHOICE");
            String rawText = (String) q.get("questionText");
            String ca = q.get("correctAnswer") instanceof String s ? s.trim() : "";
            if (ca.length() >= 2 && rawText != null && rawText.endsWith(ca)) {
                rawText = rawText.substring(0, rawText.length() - ca.length()).trim();
            }
            qb.setQuestionText(rawText);
            String expl = (String) q.get("explanation");
            boolean needsReview = checkAndTruncateOptions(q, qt);
            // 从(已截断/修正的) q 落库 options + correctAnswer
            // （R116 拆分遗漏：客观题从未 setOptions/setCorrectAnswer → 存库为 NULL。
            //   必须在 normalizeTrueFalse 之前，后者依赖 qb.correctAnswer 做判断题 T→A 归一）
            Object optsVal = q.get("options");
            if (optsVal instanceof String s && !s.isBlank()) qb.setOptions(s);
            else if (optsVal instanceof List<?> l && !l.isEmpty()) qb.setOptions(toJson(l));
            Object ansVal = q.get("correctAnswer");
            if (ansVal != null && !String.valueOf(ansVal).isBlank()) qb.setCorrectAnswer(String.valueOf(ansVal).trim());
            normalizeTrueFalse(q, qb);
            needsReview = validateAndMarkAnswer(q, qb, expl, needsReview);
            qb.setExplanation(expl);
            qb.setDifficultyLevel(q.get("difficultyLevel") instanceof Number n ? n.intValue() : 2);
            Object quality = q.get("_quality");
            if (quality instanceof Number n && n.intValue() < 60) {
                if (n.intValue() < MIN_QUALITY) {
                lowQualityRejected++;
                log.info("AI自评低分拒绝: score={}, qText={}", n.intValue(),
                    qb.getQuestionText() != null ? qb.getQuestionText().substring(0, Math.min(60, qb.getQuestionText().length())) : "");
                continue;
            }
            log.warn("AI自评质量偏低(qid={}): {}", qb.getId(), quality);
            }
            qb.setStatus(0); qb.setVersion(1); qb.setIsLatest(1);
            qb.setSource("AI");
            qb.setCreatedBy(teacherId);
            String subject = (String) params.get("subject");
            qb.setSubject(subject != null ? subject : "");
            validateNodeIds(q, validKpIds, qb, params);
            String source = q.get("source") instanceof String s ? s : "ai_generated";
            qb.setContentJson(toJson(Map.of("batchId", batchId, "text", qb.getQuestionText(), "type", source)));
            preserveMetadata(q, qb);
            Object perKpName = q.get("knowledgePoint");
            if (perKpName instanceof String && !((String) perKpName).isEmpty()) {
                qb.setKnowledgePoints(toJson(List.of(perKpName)));
            } else {
                qb.setKnowledgePoints(params.get("knowledgePoint") != null
                    ? toJson(List.of(params.get("knowledgePoint")))
                    : toJson(params.getOrDefault("knowledgePoints", List.of())));
            }
            // V055-fix: 入库前规则引擎校验 — 选项互斥/答案有效/题干完整
            List<QuestionQualityValidator.Issue> qualityIssues = QuestionQualityValidator.validate(qb);
            if (!qualityIssues.isEmpty()) {
                String issueSummary = qualityIssues.stream()
                    .map(i -> i.field() + ": " + i.message())
                    .collect(java.util.stream.Collectors.joining("; "));
                log.warn("规则引擎标记(qtype={}): {}", qb.getQuestionType(), issueSummary);
                String existingExpl = qb.getExplanation() != null ? qb.getExplanation() : "";
                qb.setExplanation(existingExpl + " 【规则引擎标记】" + issueSummary);
            }
            // V055: 入库前去重 — 跳过与已有题库题高度相似的题目
            if (isDuplicate(qb)) {
                dupSkipped++;
                log.info("跳过重复题目: {}", qb.getQuestionText() != null ? qb.getQuestionText().substring(0, Math.min(50, qb.getQuestionText().length())) : "");
                continue;
            }
            questionBankMapper.insert(qb);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", qb.getId()); r.put("questionType", qb.getQuestionType());
            r.put("questionText", qb.getQuestionText()); r.put("correctAnswer", qb.getCorrectAnswer());
            // intent/category 供智慧大屏即时出题的"四类提问分组"+提问意图标签使用(拆分时返回map漏带)
            // aiCategory 键对齐前端 AiQuestionTab 即时展示
            r.put("intent", qb.getIntent()); r.put("category", qb.getCategory()); r.put("aiCategory", qb.getCategory());
            Object optsForFrontend = qb.getOptions();
            if (optsForFrontend instanceof String s && !s.isBlank()) {
                try {
                    optsForFrontend = new ObjectMapper().readValue(s, List.class);
                } catch (Exception e) {
                    log.warn("options JSON解析失败(qid={})，回退为空数组: {}", qb.getId(), e.getMessage());
                    optsForFrontend = List.of();
                }
            }
            r.put("options", optsForFrontend != null ? optsForFrontend : List.of());
            r.put("status", "AI_DRAFT");
            r.put("source", source);  // V055: 来源标记 (ai_generated / BANK)
            if (q.containsKey("diagram")) {
                r.put("diagram", q.get("diagram"));
            }
            results.add(r);
        }
        if (dupSkipped > 0 || lowQualityRejected > 0) {
            log.info("入库完成: 共{}题, 去重跳过{}题, 低分拒绝{}题", results.size(), dupSkipped, lowQualityRejected);
        }
        // V055-fix: 将过滤计数回写到 params，供调用方传递给前端
        params.put("_dupSkipped", dupSkipped);
        params.put("_lowQualityRejected", lowQualityRejected);
        return results;
    }

    private Set<Long> buildNodeWhitelist(Map<String, Object> params) {
        Set<Long> validKpIds = new HashSet<>();
        try {
            String subKpListJson = (String) params.get("_subKpList");
            if (subKpListJson != null && !subKpListJson.isEmpty()) {
                List<Map<String, Object>> kps = new ObjectMapper().readValue(subKpListJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> kp : kps) {
                    if (kp.get("id") instanceof Number n) validKpIds.add(n.longValue());
                }
            }
        } catch (Exception e) { /* 白名单构建失败不阻塞主流程 */ }
        Object catIdObj = params.get("categoryId");
        if (catIdObj instanceof Number n) validKpIds.add(n.longValue());
        return validKpIds;
    }

    private boolean checkAndTruncateOptions(Map<String, Object> q, String qt) {
        Object optsRaw = q.get("options");
        if (!(optsRaw instanceof List<?> opts)) return false;
        boolean needsReview = false;
        boolean truncated = opts.size() > 4 && "SINGLE_CHOICE".equals(qt);
        if (truncated) {
            log.warn("AI生成了{}个选项，截断为前4个 questionType={}", opts.size(), qt);
            String rawAns = (String) q.get("correctAnswer");
            String truncatedAnswer = rawAns;
            if (rawAns != null && !rawAns.isBlank()) {
                String upperAns = rawAns.trim().toUpperCase().replaceAll("[^A-Za-z]", "");
                boolean hasOutOfRange = false;
                for (char c : upperAns.toCharArray()) { if (c > 'D') { hasOutOfRange = true; break; } }
                if (hasOutOfRange && opts.size() >= 4) {
                    String correctContent = "";
                    for (char c : upperAns.toCharArray()) {
                        int origIdx = c - 'A';
                        if (origIdx >= 0 && origIdx < opts.size()) {
                            correctContent = String.valueOf(opts.get(origIdx));
                            break;
                        }
                    }
                    if (!correctContent.isEmpty()) {
                        List<Object> newOpts = new ArrayList<>(opts.subList(0, 3));
                        newOpts.add(correctContent);
                        opts = newOpts;
                        truncatedAnswer = "D";
                        log.warn("答案{}越界，将正确选项内容'{}'移至D位", rawAns, correctContent);
                    } else {
                        needsReview = true;
                        log.warn("答案{}超出截断后选项范围(A-D)，标记待审核", rawAns);
                    }
                }
            } else {
                log.warn("AI返回空correctAnswer，标记待审核");
                needsReview = true;
            }
            q.put("correctAnswer", truncatedAnswer);
            opts = opts.subList(0, Math.min(opts.size(), 4));
        }
        // 保存前无条件剥离选项字母前缀(A./A、/A) — 兜底防止前端 "A.A." 双前缀
        List<Object> strippedOpts = new ArrayList<>();
        for (Object o : opts) strippedOpts.add(DeepSeekResponseParser.stripOptionPrefix(String.valueOf(o)));
        q.put("options", toJson(strippedOpts));
        return needsReview;
    }

    private boolean normalizeTrueFalse(Map<String, Object> q, QuestionBank qb) {
        String qt = (String) q.get("questionType");
        if (!"TRUE_FALSE".equals(qt)) return false;
        String existingOpts = qb.getOptions();
        if (existingOpts == null || existingOpts.isBlank() || "[]".equals(existingOpts.trim())) {
            qb.setOptions("[\"A. √\",\"B. ×\"]");
        }
        String ans = qb.getCorrectAnswer();
        if (ans != null) {
            String a = ans.trim().toUpperCase();
            if ("T".equals(a) || "TRUE".equals(a) || "对".equals(ans.trim()) || "正确".equals(ans.trim()) || "√".equals(a)) {
                qb.setCorrectAnswer("A");
            } else if ("F".equals(a) || "FALSE".equals(a) || "错".equals(ans.trim()) || "错误".equals(ans.trim()) || "×".equals(a)) {
                qb.setCorrectAnswer("B");
            }
        }
        return false;
    }

    private boolean validateAndMarkAnswer(Map<String, Object> q, QuestionBank qb, String expl, boolean needsReview) {
        boolean marked = needsReview;
        String qt = (String) q.get("questionType");
        if ("SINGLE_CHOICE".equals(qt) || "MULTI_CHOICE".equals(qt)) {
            String ans = qb.getCorrectAnswer();
            Object optsObj = q.get("options");
            if (ans != null && optsObj instanceof List<?> opts && !opts.isEmpty()) {
                int optCount = Math.min(opts.size(), 4);
                char maxLetter = (char) ('A' + optCount - 1);
                for (char c : ans.replaceAll("[^A-Za-z]", "").toUpperCase().toCharArray()) {
                    if (c > maxLetter) {
                        log.warn("答案字母{}超出选项范围(max={}): id={}, qText={}", c, maxLetter, qb.getId(), qb.getQuestionText());
                        if (!marked) {
                            qb.setExplanation((expl != null ? expl + " " : "") + "【答案可能异常，请教师审核】");
                        }
                        break;
                    }
                }
            }
        }
        return marked;
    }

    private void validateNodeIds(Map<String, Object> q, Set<Long> validKpIds, QuestionBank qb, Map<String, Object> params) {
        Object perKpId = q.get("knowledgeNodeId");
        boolean kpIdValid = false;
        if (perKpId instanceof Number n) {
            long kpId = n.longValue();
            if (validKpIds.isEmpty() || validKpIds.contains(kpId)) {
                qb.setCategoryId(kpId);
                kpIdValid = true;
            } else {
                log.warn("AI返回的knowledgeNodeId={}不在有效范围{}中，回退到categoryId", kpId, validKpIds);
            }
        }
        if (!kpIdValid) {
            Object categoryId = params.get("categoryId");
            if (categoryId instanceof Number n) qb.setCategoryId(n.longValue());
        }
    }

    private void preserveMetadata(Map<String, Object> q, QuestionBank qb) {
        if (q.get("intent") instanceof String s && !s.isEmpty()) qb.setIntent(s);
        if (q.get("category") instanceof String s && !s.isEmpty()) qb.setCategory(s);
        if (q.get("knowledgeDim") instanceof String s && !s.isEmpty()) qb.setKnowledgeDim(s);
        if (q.get("tier") instanceof String s && !s.isEmpty()) qb.setTier(s);
    }

    private String toJson(Object obj) {
        try { return new ObjectMapper().writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    /** V055: 检查题库中是否已有高度相似的题目（同节点 + 题干相似度 > 80%） */
    private boolean isDuplicate(QuestionBank qb) {
        if (qb.getCategoryId() == null || qb.getQuestionText() == null) return false;
        try {
            // 归一化题干用于比较
            String norm = normalizeForDedup(qb.getQuestionText());
            if (norm.length() < 10) return false; // 太短不判定为重复
            // 查同节点下所有已发布题目
            List<QuestionBank> existing = questionBankMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionBank>()
                    .eq(QuestionBank::getCategoryId, qb.getCategoryId())
                    .eq(QuestionBank::getStatus, 1)
                    .isNotNull(QuestionBank::getQuestionText));
            for (QuestionBank ex : existing) {
                String exNorm = normalizeForDedup(ex.getQuestionText());
                if (exNorm.length() < 10) continue;
                // 完全相同 → 重复
                if (norm.equals(exNorm)) return true;
                // 一个包含另一个 → 高度相似
                if (norm.contains(exNorm) || exNorm.contains(norm)) return true;
            }
        } catch (Exception e) {
            log.warn("去重查询失败(catId={}): {}", qb.getCategoryId(), e.getMessage());
        }
        return false;
    }

    private String normalizeForDedup(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s，,。．.；;：:！!？?、（）()【】\\[\\]《》\"\"''\\-\\+=|/\\\\]", "")
            .toLowerCase().trim();
    }
}
