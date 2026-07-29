package com.school.teaching.agent.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.AgentConversation;
import com.school.teaching.entity.AgentFeedback;
import com.school.teaching.entity.AgentUserMemory;
import com.school.teaching.mapper.AgentConversationMapper;
import com.school.teaching.mapper.AgentFeedbackMapper;
import com.school.teaching.mapper.AgentUserMemoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 记忆服务 — 从用户反馈和对话中自动提取、存储、检索用户偏好和模式。
 * 这是 Agent "越来越聪明"的核心引擎。
 */
@Slf4j
@Service
public class AgentMemoryService {

    private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.60");
    private static final int MAX_MEMORIES_IN_PROMPT = 5;
    private static final int MIN_EVIDENCE_FOR_PREFERENCE = 3;
    private static final int PREFERENCE_TTL_DAYS = 30;

    private final AgentFeedbackMapper feedbackMapper;
    private final AgentUserMemoryMapper memoryMapper;
    private final AgentConversationMapper conversationMapper;
    private final ObjectMapper om = new ObjectMapper();

    /** 记忆缓存：userId → (memories, expireTime)，避免每次请求都查 DB */
    private final java.util.concurrent.ConcurrentHashMap<Long, CacheEntry> memoryCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 分钟

    private static class CacheEntry {
        final List<AgentUserMemory> memories;
        final long expireAt;
        CacheEntry(List<AgentUserMemory> memories) {
            this.memories = memories;
            this.expireAt = System.currentTimeMillis() + CACHE_TTL_MS;
        }
        boolean expired() { return System.currentTimeMillis() > expireAt; }
    }

    /** 反馈标签 → 偏好记忆 的映射规则 */
    private static final Map<String, Map<String, String>> TAG_PREFERENCE_MAP = Map.of(
            "太啰嗦", Map.of("style", "concise", "detailLevel", "brief",
                    "instruction", "回答尽量简洁，用要点列表代替长篇段落"),
            "太简略", Map.of("style", "detailed", "detailLevel", "thorough",
                    "instruction", "回答需要更详细，给出具体步骤和示例"),
            "实用", Map.of("quality", "practical",
                    "instruction", "优先给出可直接操作的建议"),
            "有错误", Map.of("accuracy", "needs_improvement",
                    "instruction", "回答前先仔细核实数据，不确定的内容明确标注"),
            "准确", Map.of("accuracy", "reliable",
                    "instruction", "继续保持准确的数据引用，标注信息来源")
    );

    public AgentMemoryService(AgentFeedbackMapper feedbackMapper,
                              AgentUserMemoryMapper memoryMapper,
                              AgentConversationMapper conversationMapper) {
        this.feedbackMapper = feedbackMapper;
        this.memoryMapper = memoryMapper;
        this.conversationMapper = conversationMapper;
    }

    // ═══════════════ 记忆提取 ═══════════════

    /** 从用户反馈中提取偏好记忆（每次反馈后调用）。
     *  改进版：时间加权衰减 + 按场景分 context + 冲突检测衰减。 */
    public void extractFromFeedback(AgentFeedback feedback) {
        String tags = feedback.getFeedbackTags();
        if (tags == null || tags.isBlank()) return;

        // 从工具使用中推断场景上下文
        String contextTag = inferContextTag(feedback.getToolsUsed());

        // E-3: 一次性查询所有近期反馈，内存过滤替代 N+1
        LocalDateTime since = LocalDateTime.now().minusDays(PREFERENCE_TTL_DAYS);
        List<AgentFeedback> allRecent = feedbackMapper.selectList(new LambdaQueryWrapper<AgentFeedback>()
                .eq(AgentFeedback::getUserId, feedback.getUserId())
                .ge(AgentFeedback::getCreatedAt, since));

        String[] tagArr = tags.split("[,，]");
        for (String tag : tagArr) {
            String normalized = tag.trim();
            if (normalized.isEmpty()) continue;

            Map<String, String> prefRule = TAG_PREFERENCE_MAP.get(normalized);
            if (prefRule == null) continue;

            // 内存过滤：只保留包含当前标签的反馈
            double weightedEvidence = 0;
            for (AgentFeedback fb : allRecent) {
                String fbTags = fb.getFeedbackTags();
                if (fbTags == null || !fbTags.contains(normalized)) continue;
                long daysAgo = java.time.Duration.between(fb.getCreatedAt(), LocalDateTime.now()).toDays();
                double weight = Math.max(0.1, 1.0 - daysAgo / (double) PREFERENCE_TTL_DAYS);
                weightedEvidence += weight;
            }

            if (weightedEvidence >= MIN_EVIDENCE_FOR_PREFERENCE) {
                String sceneKey = "answer_preference:" + contextTag;

                upsertMemory(feedback.getUserId(), feedback.getRoleName(),
                        "PREFERENCE", sceneKey,
                        mapToJson(prefRule),
                        calcConfidenceWeighted(weightedEvidence),
                        (int) Math.round(weightedEvidence),
                        feedback.getSessionId(),
                        LocalDateTime.now().plusDays(PREFERENCE_TTL_DAYS));
                log.info("MemoryExtract: userId={}, tag={}, context={}, weightedEvidence={}",
                        feedback.getUserId(), normalized, contextTag, String.format("%.1f", weightedEvidence));

                // 冲突检测：如果存在对立偏好，衰减双方置信度
                detectAndDecayConflict(feedback.getUserId(), normalized, contextTag);
            }
        }

        // 高评分 → 提取工具组合模式
        if (feedback.getRating() != null && feedback.getRating() >= 4
                && feedback.getToolsUsed() != null && !feedback.getToolsUsed().isBlank()) {
            extractToolPattern(feedback);
        }
    }

    /**
     * 从工具使用列表推断场景上下文。
     * 用于偏好按场景分组，避免"做练习时说太啰嗦"影响"查分析时说太啰嗦"。
     */
    private String inferContextTag(String toolsUsed) {
        if (toolsUsed == null || toolsUsed.isBlank()) return "general";
        Set<String> exerciseTools = Set.of("similar_questions", "teaching_similar_questions",
                "wrong_book", "teaching_student_wrong_book",
                "question_explain", "teaching_question_explain");
        Set<String> analyticsTools = Set.of("class_analytics", "teaching_class_analytics",
                "knowledge_trend", "teaching_knowledge_trend",
                "student_growth", "teaching_student_growth");
        Set<String> conceptTools = Set.of("knowledge_search", "teaching_knowledge_search",
                "syllabus_lookup", "teaching_syllabus_lookup");

        for (String t : exerciseTools) { if (toolsUsed.contains(t)) return "exercise"; }
        for (String t : analyticsTools) { if (toolsUsed.contains(t)) return "analytics"; }
        for (String t : conceptTools) { if (toolsUsed.contains(t)) return "concept"; }
        return "general";
    }

    /**
     * 冲突检测与衰减：当用户同时表达对立偏好时（如"太啰嗦"+"太简略"），
     * 两条记忆的置信度各衰减 0.2，避免偏好反复横跳。
     */
    private void detectAndDecayConflict(Long userId, String currentTag, String contextTag) {
        // 对立偏好映射
        Map<String, String> conflictMap = Map.of(
                "太啰嗦", "太简略",
                "太简略", "太啰嗦"
        );
        String opposite = conflictMap.get(currentTag);
        if (opposite == null) return;

        // 查近 7 天内是否有对立偏好
        LocalDateTime recent = LocalDateTime.now().minusDays(7);
        List<AgentFeedback> conflicts = feedbackMapper.selectList(new LambdaQueryWrapper<AgentFeedback>()
                .eq(AgentFeedback::getUserId, userId)
                .ge(AgentFeedback::getCreatedAt, recent)
                .like(AgentFeedback::getFeedbackTags, opposite));

        if (conflicts.isEmpty()) return;

        // 衰减对立偏好的记忆
        String oppositeSceneKey = "answer_preference:" + contextTag;
        AgentUserMemory oppositeMem = memoryMapper.selectOne(new LambdaQueryWrapper<AgentUserMemory>()
                .eq(AgentUserMemory::getUserId, userId)
                .eq(AgentUserMemory::getMemoryType, "PREFERENCE")
                .eq(AgentUserMemory::getMemoryKey, oppositeSceneKey)
                .like(AgentUserMemory::getMemoryValue, opposite)
                .last("LIMIT 1"));

        if (oppositeMem != null) {
            BigDecimal newConf = oppositeMem.getConfidence()
                    .subtract(new BigDecimal("0.20"));
            if (newConf.compareTo(BigDecimal.ZERO) < 0) newConf = BigDecimal.ZERO;
            oppositeMem.setConfidence(newConf);
            memoryMapper.updateById(oppositeMem);
            invalidateCache(userId);
            log.info("MemoryConflict: userId={}, decay={}, newConf={}",
                    userId, opposite, newConf);
        }
    }

    /** 从高评分反馈中提取有效工具组合模式 */
    private void extractToolPattern(AgentFeedback feedback) {
        String tools = feedback.getToolsUsed();
        // 标准化工具名排序，确保同一组合不被顺序差异影响
        String sorted = Arrays.stream(tools.split("[,，]"))
                .map(String::trim).filter(s -> !s.isEmpty())
                .sorted().collect(Collectors.joining(","));
        if (sorted.isEmpty()) return;

        String patternKey = "tool_combo_" + Integer.toHexString(sorted.hashCode());

        // 查已有模式记录
        AgentUserMemory existing = memoryMapper.selectOne(new LambdaQueryWrapper<AgentUserMemory>()
                .eq(AgentUserMemory::getUserId, feedback.getUserId())
                .eq(AgentUserMemory::getMemoryType, "PATTERN")
                .eq(AgentUserMemory::getMemoryKey, patternKey));

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("tools", sorted);
        value.put("lastRating", feedback.getRating());

        int evidence = existing != null ? existing.getEvidenceCount() + 1 : 1;
        upsertMemory(feedback.getUserId(), feedback.getRoleName(),
                "PATTERN", patternKey,
                mapToJson(value),
                calcConfidence(evidence),
                evidence,
                feedback.getSessionId(),
                null); // PATTERN 不过期
    }

    /** 从用户消息中提取事实性记忆（异步调用，不阻塞对话）。
     *  改进版：使用更精确的上下文感知匹配，避免误提取。 */
    public void extractFactsFromMessage(Long userId, String roleName,
                                        String userMessage, String sessionId) {
        if (userMessage == null || userMessage.isBlank()) return;

        Map<String, String> facts = new LinkedHashMap<>();

        // 1. 教学学科识别：需要明确的"教+学科"上下文，而非孤立的学科词
        if (isTeachingContext(userMessage)) {
            String subject = extractTeachingSubject(userMessage);
            if (subject != null) {
                facts.put("teachingSubject", subject);
            }
        }

        // 2. 班级语境：明确提到班级
        if (userMessage.contains("我们班") || userMessage.contains("我班")
                || userMessage.contains("我的班级") || userMessage.contains("我带的班")) {
            facts.put("hasClassContext", "true");
        }

        // 3. 偏好表达：需要"偏好/喜欢/习惯"等明确信号
        if (userMessage.contains("我喜欢") || userMessage.contains("我偏好")
                || userMessage.contains("我习惯") || userMessage.contains("我更想要")) {
            facts.put("expressedPreference", "true");
        }

        // 4. 学生自我认知：学生角色的自我介绍
        if ("STUDENT".equals(roleName) && (userMessage.contains("我今年") || userMessage.contains("我上")
                || userMessage.contains("我的年级") || userMessage.contains("我的专业"))) {
            facts.put("studentSelfIntro", "true");
        }

        for (Map.Entry<String, String> fact : facts.entrySet()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("value", fact.getValue());
            value.put("source", "user_message");
            value.put("extractedAt", LocalDateTime.now().toString());

            upsertMemory(userId, roleName, "FACT", fact.getKey(),
                    mapToJson(value),
                    new BigDecimal("0.70"),
                    1, sessionId, null);
        }
    }

    /**
     * 判断用户消息是否处于教学相关上下文（自报学科、教学计划等）。
     * 仅在此类上下文中提取学科，避免把"网络太卡了"误判为"教网络"。
     */
    private boolean isTeachingContext(String msg) {
        return msg.contains("我是教") || msg.contains("我教") || msg.contains("我负责教")
                || msg.contains("我带") || msg.contains("教的学科") || msg.contains("教学科")
                || msg.contains("任教") || msg.contains("教的科目");
    }

    /**
     * 从教学上下文中提取学科名。
     * 按实际知识树中的学科名匹配，避免"网络""办公"等通用词误提取。
     */
    private String extractTeachingSubject(String msg) {
        // 按优先级匹配：先长后短，避免"信息"匹配到"信息技术"的子串
        List<String> knownSubjects = List.of(
                "信息技术应用基础", "信息技术", "计算机网络", "办公软件",
                "数学", "英语", "语文", "物理", "化学", "生物",
                "政治", "历史", "地理", "美术", "音乐", "体育");
        for (String subj : knownSubjects) {
            if (msg.contains(subj)) return subj;
        }
        // 若非已知学科，尝试提取"教XX"后面的2-4字作为学科
        for (String prefix : List.of("我是教", "我教", "任教", "教的是")) {
            int idx = msg.indexOf(prefix);
            if (idx >= 0) {
                int start = idx + prefix.length();
                int end = Math.min(start + 4, msg.length());
                String candidate = msg.substring(start, end).replaceAll("[，。！？,.!?\\s].*", "").trim();
                if (candidate.length() >= 1 && candidate.length() <= 4) return candidate;
            }
        }
        return null;
    }

    // ═══════════════ 记忆检索 ═══════════════

    /** 获取用户的高置信度记忆，用于注入 system prompt（带缓存） */
    public List<AgentUserMemory> getMemoriesForPrompt(Long userId) {
        CacheEntry cached = memoryCache.get(userId);
        if (cached != null && !cached.expired()) {
            return cached.memories;
        }

        LocalDateTime now = LocalDateTime.now();
        List<AgentUserMemory> memories = memoryMapper.selectList(new LambdaQueryWrapper<AgentUserMemory>()
                .eq(AgentUserMemory::getUserId, userId)
                .eq(AgentUserMemory::getStatus, "ACTIVE")
                .ge(AgentUserMemory::getConfidence, CONFIDENCE_THRESHOLD)
                .and(w -> w.isNull(AgentUserMemory::getExpiresAt)
                         .or().gt(AgentUserMemory::getExpiresAt, now))
                .orderByDesc(AgentUserMemory::getConfidence)
                .last("LIMIT " + MAX_MEMORIES_IN_PROMPT));

        memoryCache.put(userId, new CacheEntry(memories));
        return memories;
    }

    /** 清除指定用户的记忆缓存（记忆更新后调用） */
    public void invalidateCache(Long userId) {
        memoryCache.remove(userId);
    }

    /** 每小时清理过期缓存条目，防止内存泄漏 */
    @Scheduled(fixedRateString = "${agent.cache-cleanup-ms:3600000}")
    public void cleanupExpiredCache() {
        int before = memoryCache.size();
        memoryCache.values().removeIf(CacheEntry::expired);
        int after = memoryCache.size();
        if (before != after) {
            log.debug("MemoryCache 清理: {} → {} (移除 {} 条)", before, after, before - after);
        }
    }

    /** 存储 LLM 提取的事实记忆（由 AgentLoopService 异步调用） */
    public void storeLLMFact(Long userId, String roleName, String factKey,
                             String factValue, String sessionId) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("value", factValue);
        value.put("source", "llm_extraction");
        value.put("extractedAt", LocalDateTime.now().toString());
        value.put("sessionId", sessionId);
        upsertMemory(userId, roleName, "FACT", "llm_" + factKey,
                mapToJson(value), new BigDecimal("0.80"), 1, sessionId, null);
    }

    /** 将记忆列表格式化为 prompt 片段 */
    public String formatMemoriesForPrompt(List<AgentUserMemory> memories) {
        if (memories == null || memories.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("📝 关于你与用户的过往交互（系统自动学习）：\n");

        int count = 0;
        for (AgentUserMemory m : memories) {
            if (count >= MAX_MEMORIES_IN_PROMPT) break;
            String line = formatSingleMemory(m);
            if (line != null) {
                sb.append(line).append("\n");
                count++;
            }
        }
        return sb.toString();
    }

    private String formatSingleMemory(AgentUserMemory m) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> value = om.readValue(m.getMemoryValue(), Map.class);
            String type = m.getMemoryType();
            int pct = m.getConfidence().multiply(new BigDecimal("100")).intValue();

            if ("PREFERENCE".equals(type)) {
                String instr = (String) value.get("instruction");
                if (instr != null) {
                    return "- " + instr + "（置信度 " + pct + "%，来自 "
                            + m.getEvidenceCount() + " 次反馈）";
                }
            } else if ("PATTERN".equals(type)) {
                String tools = (String) value.get("tools");
                if (tools != null) {
                    return "- 工具组合「" + tools + "」在该用户场景下效果良好"
                            + "（来自 " + m.getEvidenceCount() + " 次好评）";
                }
            } else if ("FACT".equals(type)) {
                String v = (String) value.get("value");
                return "- " + m.getMemoryKey() + "：" + v;
            }
        } catch (JsonProcessingException e) {
            log.warn("解析记忆值失败（数据可能损坏）: memoryId={}, key={}", m.getId(), m.getMemoryKey());
        }
        return null;
    }

    // ═══════════════ 辅助方法 ═══════════════

    private void upsertMemory(Long userId, String roleName, String type,
                              String key, String valueJson, BigDecimal confidence,
                              int evidence, String sessionId, LocalDateTime expiresAt) {
        AgentUserMemory existing = memoryMapper.selectOne(new LambdaQueryWrapper<AgentUserMemory>()
                .eq(AgentUserMemory::getUserId, userId)
                .eq(AgentUserMemory::getMemoryType, type)
                .eq(AgentUserMemory::getMemoryKey, key));

        if (existing != null) {
            // 更新：提高置信度（新旧加权平均，新证据权重 0.3）
            BigDecimal newConf = existing.getConfidence()
                    .multiply(new BigDecimal("0.7"))
                    .add(confidence.multiply(new BigDecimal("0.3")));
            if (newConf.compareTo(BigDecimal.ONE) > 0) newConf = BigDecimal.ONE;

            existing.setMemoryValue(valueJson); // 覆盖更新旧值（如事实变化）
            existing.setConfidence(newConf);
            existing.setEvidenceCount(existing.getEvidenceCount() + 1);
            existing.setLastEvidenceAt(LocalDateTime.now());
            existing.setSourceSessionId(sessionId);
            if (expiresAt != null) existing.setExpiresAt(expiresAt);
            existing.setStatus("ACTIVE");
            memoryMapper.updateById(existing);
        } else {
            AgentUserMemory mem = new AgentUserMemory();
            mem.setUserId(userId);
            mem.setRoleName(roleName);
            mem.setMemoryType(type);
            mem.setMemoryKey(key);
            mem.setMemoryValue(valueJson);
            mem.setConfidence(confidence);
            mem.setEvidenceCount(evidence);
            mem.setLastEvidenceAt(LocalDateTime.now());
            mem.setSourceSessionId(sessionId);
            mem.setExpiresAt(expiresAt);
            mem.setStatus("ACTIVE");
            mem.setSchoolId(1L);
            try {
                memoryMapper.insert(mem);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发竞态：另一线程已插入，降级为更新
                log.debug("upsertMemory 并发冲突，降级更新: userId={}, type={}, key={}", userId, type, key);
                AgentUserMemory dup = memoryMapper.selectOne(new LambdaQueryWrapper<AgentUserMemory>()
                        .eq(AgentUserMemory::getUserId, userId)
                        .eq(AgentUserMemory::getMemoryType, type)
                        .eq(AgentUserMemory::getMemoryKey, key));
                if (dup != null) {
                    dup.setMemoryValue(valueJson);
                    dup.setConfidence(confidence);
                    dup.setEvidenceCount(dup.getEvidenceCount() + 1);
                    dup.setLastEvidenceAt(LocalDateTime.now());
                    dup.setSourceSessionId(sessionId);
                    if (expiresAt != null) dup.setExpiresAt(expiresAt);
                    dup.setStatus("ACTIVE");
                    memoryMapper.updateById(dup);
                }
            }
        }
        invalidateCache(userId);
    }

    private BigDecimal calcConfidence(long evidenceCount) {
        // 置信度 = min(0.95, 0.50 + evidenceCount * 0.10)
        double c = 0.50 + evidenceCount * 0.10;
        return new BigDecimal(Math.min(0.95, c)).setScale(2, RoundingMode.HALF_UP);
    }

    /** 针对加权证据计数的置信度计算（支持小数权重） */
    private BigDecimal calcConfidenceWeighted(double weightedEvidence) {
        double c = 0.50 + weightedEvidence * 0.10;
        return new BigDecimal(Math.min(0.95, c)).setScale(2, RoundingMode.HALF_UP);
    }

    private String mapToJson(Map<String, ?> map) {
        try {
            return om.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /** 按 followUpTaskId 查询关联的 Agent 会话 */
    public AgentConversation getConversationByTaskId(Long taskId) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<AgentConversation>()
                .eq(AgentConversation::getFollowUpTaskId, taskId)
                .last("LIMIT 1"));
    }

    /** 保存用户反馈 */
    public void saveFeedback(AgentFeedback feedback) {
        feedbackMapper.insert(feedback);
    }
}
