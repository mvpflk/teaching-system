package com.school.teaching.service.impl;

import static com.school.teaching.service.impl.AiContentHelper.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.AiCallLog;
import com.school.teaching.entity.AiOutput;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.AiCallLogMapper;
import com.school.teaching.mapper.AiOutputMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.metrics.AiMetricsService;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.*;
import com.school.teaching.agent.prompt.PromptTemplateCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AiContentGeneratorServiceImpl implements AiContentGeneratorService {

    // T-04: 全局并发限流 — AI 长任务最多 10 个同时执行，超 30 秒获取不到许可则降级
    private static final java.util.concurrent.Semaphore AI_CONCURRENCY_LIMIT = new java.util.concurrent.Semaphore(10, true);

    // 共享并行线程池：用于 generateKpAware 和 generateBatched 的并行分批生成
    // 固定 8 线程，配合 Semaphore(10) 防止 API 过载
    private static final java.util.concurrent.ExecutorService AI_PARALLEL_EXECUTOR =
        java.util.concurrent.Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "ai-parallel-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });

    @jakarta.annotation.PreDestroy
    public void shutdownParallelExecutor() {
        AI_PARALLEL_EXECUTOR.shutdown();
        try {
            if (!AI_PARALLEL_EXECUTOR.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                AI_PARALLEL_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            AI_PARALLEL_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Autowired private AiOutputMapper outputMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private AiCallLogMapper aiCallLogMapper;
    @Autowired private AiServiceGateway aiGateway;
    @Autowired private AiTaskStore taskStore;
    @Autowired private KnowledgeNodeService knowledgeNodeService;
    @Autowired private AiQuestionGeneratorService aiQuestionService;
    @Autowired private ExamSyllabusService examSyllabusService;
    @Lazy @Autowired private AiContentGeneratorService self;

    @Autowired private SystemService systemService;
    @Autowired private com.school.teaching.security.ContentSafetyFilter safetyFilter;
    @Autowired private com.school.teaching.service.TaskService taskService;
    @Autowired private com.school.teaching.mapper.QuestionBankMapper questionBankMapper;
    @Autowired private com.school.teaching.mapper.TeacherMapper teacherMapper;
    @Autowired private AiMetricsService aiMetrics;
    @Autowired private TeacherReferenceQuestionService teacherReferenceQuestionService;
    @Autowired private GeomRenderService geomRenderService;
    @Autowired private QuestionBankMixer questionBankMixer;
    @Autowired(required = false) private PromptTemplateCache promptTemplateCache;

    private static final ObjectMapper om = new ObjectMapper();
    private static final java.util.regex.Pattern JSON_BLOCK =
        java.util.regex.Pattern.compile("```json\\s*(\\{[^`]+\\})\\s*```", java.util.regex.Pattern.DOTALL);

    @Override
    public String submitGeneration(Long teacherId, Map<String, Object> params) {
        String taskId = taskStore.create(com.school.teaching.common.AsyncTaskType.AI_GENERATE, 1200, teacherId); // 20分钟TTL，支持55-100题
        try {
            self.executeAsync(taskId, teacherId, params);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            taskStore.fail(taskId, "AI 服务繁忙，请稍后重试");
        }
        return taskId;
    }

    @Override
    public AiTaskStore.TaskEntry getResult(String taskId) {
        return taskStore.get(taskId);
    }

    @Override
    public Map<String, Object> generateSync(Long teacherId, Map<String, Object> params) {
        String taskId = taskStore.create(180);
        try {
            self.executeAsync(taskId, teacherId, params);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            taskStore.fail(taskId, "AI 服务繁忙，请稍后重试");
        }
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 180_000) {
            AiTaskStore.TaskEntry entry = taskStore.get(taskId);
            if ("COMPLETED".equals(entry.status)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) entry.result;
                return result;
            }
            if ("FAILED".equals(entry.status)) throw new BusinessException(500, entry.error);
            try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
        }
        throw new BusinessException(504, "AI 生成超时，请重试");
    }

    @Override
    @Async("aiExecutor")
    public void executeAsync(String taskId, Long teacherId, Map<String, Object> params) {
        com.school.teaching.common.SchoolContext.set(1L);
        // 防御：如果任务已被取消/超时，不再执行
        AiTaskStore.TaskEntry existing = taskStore.get(taskId);
        if (existing != null && !"PENDING".equals(existing.status) && !"RUNNING".equals(existing.status)) {
            com.school.teaching.common.SchoolContext.clear();
            return;
        }
        // T-04: 并发限流 — 获取许可，最多等 30 秒
        try {
            if (!AI_CONCURRENCY_LIMIT.tryAcquire(30, java.util.concurrent.TimeUnit.SECONDS)) {
                taskStore.fail(taskId, "系统繁忙，请稍后重试");
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskStore.fail(taskId, "系统繁忙，请稍后重试");
            return;
        }
        taskStore.markRunning(taskId);
        long start = 0;
        try {
            String contentType = (String) params.get("contentType");
            checkQuota(teacherId, contentType);
            // 支持多选章节(categoryIds数组) + 单选(categoryId)
            Long nodeId = params.get("categoryId") instanceof Number n ? n.longValue() : null;
            if (nodeId == null) {
                nodeId = params.get("nodeId") instanceof Number n ? n.longValue() : null;
            }
            @SuppressWarnings("unchecked")
            List<Long> categoryIds = params.get("categoryIds") instanceof List<?> list
                ? list.stream().filter(o -> o instanceof Number).map(o -> ((Number) o).longValue()).toList()
                : java.util.List.of();
            // 如果有多选章节ID且主nodeId未设置，用第一个作为anchor
            if (nodeId == null && !categoryIds.isEmpty()) {
                nodeId = categoryIds.get(0);
            }

            // sourceOutputId：从知识清单联动生成配套练习时（走历史页无级联场景），先于 injectNodePath 执行
            Object sourceOutputIdObj = params.get("sourceOutputId");
            if (sourceOutputIdObj != null && "KNOWLEDGE_PRACTICE".equals(contentType)) {
                Long sourceId;
                try {
                    sourceId = sourceOutputIdObj instanceof Number n ? n.longValue()
                        : Long.parseLong(String.valueOf(sourceOutputIdObj));
                } catch (Exception e) {
                    throw new BusinessException(400, "sourceOutputId解析失败: " + e.getMessage());
                }
                AiOutput source = outputMapper.selectById(sourceId);
                if (source == null) {
                    throw new BusinessException(400, "关联的知识清单不存在");
                }
                // @Async 线程 SecurityContext 恒 null，仅校验本人清单
                if (!source.getTeacherId().equals(teacherId)) {
                    throw new BusinessException(403, "无权使用他人的知识清单生成配套练习");
                }
                // categoryId 缺失时回退使用清单记录的 node_id（历史页触发场景）
                if (nodeId == null) {
                    Long sourceNodeId = source.getNodeId();
                    if (sourceNodeId != null && sourceNodeId > 0) {
                        nodeId = sourceNodeId;
                        params.put("categoryId", nodeId);
                        log.info("从清单记录回退nodeId: {}", nodeId);
                    }
                }
                // subject 缺失时回填
                if (params.get("subject") == null || String.valueOf(params.getOrDefault("subject", "")).isBlank()) {
                    String sourceSubject = source.getSubject();
                    if (sourceSubject != null && !sourceSubject.isBlank()) {
                        params.put("subject", sourceSubject);
                        log.info("从清单记录回填subject: {}", sourceSubject);
                    }
                }
                String checklistContent = source.getContent();
                if (checklistContent == null || checklistContent.isBlank()) {
                    throw new BusinessException(400, "知识清单内容为空，无法生成配套练习");
                }
                if (checklistContent.length() < 200) {
                    log.warn("清单内容过短(<200字符)，降级使用RAG上下文: sourceId={}", sourceId);
                } else {
                    if (checklistContent.length() > 6000) {
                        int cut = checklistContent.lastIndexOf("\n", 6000);
                        checklistContent = (cut > 0 ? checklistContent.substring(0, cut) : checklistContent.substring(0, 6000))
                            + "\n...(内容已按段落边界截断)";
                    }
                    params.put("_checklistContent", checklistContent);
                    log.info("注入清单内容作为参考资料: sourceId={}, length={}", sourceId, checklistContent.length());
                }
            }

            injectNodePath(params, nodeId);

            // 多选章节：合并所有章节的RAG上下文（总量封顶）
            String ragContext;
            if (!categoryIds.isEmpty()) {
                StringBuilder ragBuilder = new StringBuilder();
                for (Long cid : categoryIds) {
                    if (ragBuilder.length() > 15000) break; // 总量封顶
                    String partial = buildRagContext(cid);
                    if (partial != null && !partial.isBlank()) {
                        ragBuilder.append(partial).append("\n\n");
                    }
                }
                ragContext = ragBuilder.toString();
            } else {
                ragContext = buildRagContext(nodeId);
            }
            // 最终兜底：单次 AI 调用的 rag 上下文不超过 20000 字符
            if (ragContext != null && ragContext.length() > 20000) {
                ragContext = ragContext.substring(0, 20000) + "\n...(RAG内容已截断)";
            }
            log.info("AI请求 ragContext: {} 字符, nodeId={}, chapters={}",
                ragContext != null ? ragContext.length() : 0, nodeId, categoryIds.size());
            params.put("ragContext", ragContext);

            // 合并所有选中章节的子知识点列表（含内容深度，供后续按比例分配）
            List<Map<String, Object>> allKpsWithDepth = new java.util.ArrayList<>();
            if (!categoryIds.isEmpty()) {
                for (Long cid : categoryIds) {
                    collectKpsWithDepth(cid, allKpsWithDepth, new java.util.HashSet<>());
                }
            } else if (nodeId != null) {
                collectKpsWithDepth(nodeId, allKpsWithDepth, new java.util.HashSet<>());
            }
            // 知识点分级：正常(≥100字) → 权重1.0；薄知识点(<100字) → 权重0.3，保底1题
            List<Map<String, Object>> viableKps = new java.util.ArrayList<>();
            Map<Long, Double> kpWeights = new java.util.LinkedHashMap<>();
            int thinCount = 0;
            for (Map<String, Object> kp : allKpsWithDepth) {
                int len = kp.get("contentLen") instanceof Number n ? n.intValue() : 0;
                Long kpId = kp.get("id") instanceof Number n2 ? n2.longValue() : null;
                viableKps.add(kp);
                if (len < 100) {
                    if (kpId != null) kpWeights.put(kpId, 0.3);
                    thinCount++;
                    log.info("薄知识点({}, 仅{}字): 权重降至0.3, 保底1题",
                        kp.get("name") instanceof String s ? s : kpId, len);
                } else {
                    if (kpId != null) kpWeights.put(kpId, 1.0);
                }
            }
            if (!viableKps.isEmpty()) {
                params.put("_subKpList", om.writeValueAsString(viableKps));
                params.put("_viableKps", viableKps);
                params.put("_kpWeights", kpWeights);
                log.info("知识点收集: 原始{}个, 有效{}个 (含{}个薄知识点)", allKpsWithDepth.size(), viableKps.size(), thinCount);
            }

            // 注入知识老化提示
            try {
                String agingHint = buildAgingHint(nodeId);
                if (agingHint != null && !agingHint.isEmpty()) {
                    params.put("ragContext", params.getOrDefault("ragContext", "") + "\n\n" + agingHint);
                }
            } catch (Exception e) { log.warn("buildAgingHint 失败: {}", e.getMessage()); }

            // 注入考纲上下文
            try {
                Long subjectId = knowledgeNodeService.findSubjectRoot(nodeId);
                if (subjectId == null) {
                    subjectId = params.get("subjectId") instanceof Number n ? n.longValue() : null;
                }
                boolean preciseMapping = systemService.getBooleanConfig("feature.syllabus_node_mapping_enabled", false);
                String syllabusContext;
                if (preciseMapping && nodeId != null) {
                    syllabusContext = examSyllabusService.getSyllabusPromptContextByNode(nodeId);
                } else {
                    syllabusContext = examSyllabusService.getSyllabusPromptContext(subjectId);
                }
                if (syllabusContext != null && !syllabusContext.isEmpty()) {
                    params.put("syllabusContext", syllabusContext);
                }
                // 注入结构化考纲元数据 JSON（供 PromptBuilder 稳健读取，避免 Markdown 正则解析）
                try {
                    if (subjectId != null) {
                        String syllabusMeta = examSyllabusService.getSyllabusMeta(subjectId);
                        if (syllabusMeta != null && !syllabusMeta.isEmpty()) {
                            params.put("syllabusMeta", syllabusMeta);
                        }
                    }
                } catch (Exception e) { log.warn("结构化元数据读取失败（L2/L3兜底）: {}", e.getMessage()); }
            } catch (Exception e) { log.warn("考纲查询失败: {}", e.getMessage()); }

            // 安全过滤：输入侧prompt注入检测
            if (systemService.getBooleanConfig("feature.security_filter_enabled", true)) {
                safetyFilter.validateInput((String) params.getOrDefault("ragContext", ""));
                safetyFilter.validateInput((String) params.getOrDefault("syllabusContext", ""));
            }

            // 注入真题参考样式（Few-shot 风格对齐） — V055: 扩展到所有出题类型
            if (isQuestionContentType(contentType)) {
                String subject = (String) params.get("subject");
                if (subject != null && !subject.isEmpty()) {
                    try {
                        String ref = teacherReferenceQuestionService.loadAllTypes(subject);
                        if (ref != null && !ref.isEmpty()) {
                            params.put("_referenceQuestions", ref);
                        }
                    } catch (Exception e) {
                        log.warn("加载真题参考失败: subject={}", subject, e);
                    }
                }
            }

            TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build(contentType, params);

            if (promptTemplateCache != null) {
                String override = promptTemplateCache.getFinal("teaching_content_" + contentType.toLowerCase(), (String) params.get("subject"));
                if (override != null) {
                    pr = new TeachingContentPromptBuilder.PromptResult(override, pr.maxTokens(), pr.extraParams());
                }
            }

            // Prompt 全链路验证日志
            if (pr.isQuestionType() && pr.extraParams() != null) {
                String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
                log.info("AI Prompt [{}]: {}", contentType, ip.length() > 200 ? ip.substring(0, 200) : ip);
            } else if (pr.prompt() != null) {
                log.info("AI Prompt [{}]: {}", contentType, pr.prompt().length() > 200 ? pr.prompt().substring(0, 200) : pr.prompt());
            }

            start = System.currentTimeMillis();
            Map<String, Object> result = new LinkedHashMap<>();
            int tokensUsed = 0;

            // DIAGNOSIS 走独立的内容生成通道（不走出题通道）
            if ("DIAGNOSIS".equals(contentType)) {
                Map<String, Object> contentParams = new LinkedHashMap<>();
                contentParams.put("prompt", pr.prompt());
                contentParams.put("maxTokens", pr.maxTokens());
                contentParams.put("temperature", 0.3);
                String rawDiagnosis = aiGateway.generateContent(contentParams);
                Object dtk = contentParams.get("_tokensUsed");
                int dTokens = dtk instanceof Number n ? n.intValue() : 0;

                AiOutput diagOutput = new AiOutput();
                Long dTaskId = null;
                Object taskIdObj = params.get("taskId");
                if (taskIdObj instanceof Number n) {
                    dTaskId = n.longValue();
                } else if (taskIdObj instanceof String s && !s.isBlank()) {
                    try { dTaskId = Long.parseLong(s); } catch (NumberFormatException ignored) { log.debug("任务ID解析失败: {}", s); }
                }
                diagOutput.setNodeId(dTaskId != null ? dTaskId : 0L);
                diagOutput.setTeacherId(teacherId);
                diagOutput.setOutputType("DIAGNOSIS");
                diagOutput.setTitle("诊断报告 - " + (params.getOrDefault("subject", "")));
                diagOutput.setSubject((String) params.getOrDefault("subject", ""));
                diagOutput.setContent(rawDiagnosis.trim());
                diagOutput.setIsLatest(1);
                diagOutput.setVersionSeq(1);
                diagOutput.setStatus(0);
                diagOutput.setTokensUsed(dTokens);
                diagOutput.setLatencyMs((int) (System.currentTimeMillis() - start));
                diagOutput.setCreatedAt(LocalDateTime.now());
                outputMapper.insert(diagOutput);

                AiCallLog callLog = new AiCallLog();
                callLog.setSchoolId(1L);
                callLog.setUserId(teacherId);
                callLog.setCapability("DIAGNOSIS");
                callLog.setProvider(aiGateway.getProvider());
                callLog.setTokensUsed(dTokens);
                callLog.setLatencyMs((int) (System.currentTimeMillis() - start));
                callLog.setStatus("SUCCESS");
                callLog.setPromptTokens(dTokens / 3);
                callLog.setCompletionTokens(dTokens * 2 / 3);
                aiCallLogMapper.insert(callLog);
                aiMetrics.recordCall("DIAGNOSIS", "SUCCESS", dTokens, dTokens / 3, dTokens * 2 / 3, (int) (System.currentTimeMillis() - start), null);

                result.put("type", "diagnosis");
                result.put("outputId", diagOutput.getId());
                result.put("content", rawDiagnosis.trim());
                taskStore.complete(taskId, result);
                com.school.teaching.common.SchoolContext.clear();
                return;
            }

            // 巩固材料走内容生成通道
            if ("CONSOLIDATION_MATERIAL".equals(contentType)) {
                String point = (String) params.getOrDefault("knowledgePoint", "");
                String stageHint = (String) params.getOrDefault("stageHint", "");
                java.util.Map<String, Object> contentParams = new java.util.LinkedHashMap<>();
                contentParams.put("prompt", pr.prompt());
                contentParams.put("maxTokens", pr.maxTokens());
                contentParams.put("temperature", 0.7);
                contentParams.put("knowledgePoint", point);
                contentParams.put("stageHint", stageHint);
                String markdown = aiGateway.generateContent(contentParams);
                int consTokens = (contentParams.get("_tokensUsed") instanceof Number n) ? n.intValue() : 0;

                AiOutput output = new AiOutput();
                Long consNodeId = params.get("categoryId") instanceof Number n ? n.longValue() : null;
                if (consNodeId == null) consNodeId = nodeId;
                output.setNodeId(consNodeId != null ? consNodeId : 0L);
                output.setTeacherId(teacherId);
                output.setOutputType("CONSOLIDATION_MATERIAL");
                output.setTitle("巩固材料");
                output.setSubject((String) params.getOrDefault("subject", ""));
                output.setContent(markdown != null ? markdown.trim() : "");
                output.setIsLatest(1);
                output.setVersionSeq(1);
                output.setStatus(0);
                output.setTokensUsed(consTokens);
                output.setLatencyMs((int) (System.currentTimeMillis() - start));
                output.setCreatedAt(java.time.LocalDateTime.now());
                outputMapper.insert(output);

                AiCallLog callLog = new AiCallLog();
                callLog.setSchoolId(1L);
                callLog.setUserId(teacherId);
                callLog.setCapability("CONSOLIDATION_MATERIAL");
                callLog.setProvider(aiGateway.getProvider());
                callLog.setTokensUsed(consTokens);
                callLog.setLatencyMs((int) (System.currentTimeMillis() - start));
                callLog.setStatus("SUCCESS");
                callLog.setPromptTokens(consTokens / 3);
                callLog.setCompletionTokens(consTokens * 2 / 3);
                aiCallLogMapper.insert(callLog);
                aiMetrics.recordCall("CONSOLIDATION_MATERIAL", "SUCCESS", consTokens, consTokens / 3, consTokens * 2 / 3, (int) (System.currentTimeMillis() - start), null);

                java.util.Map<String, Object> tmResult = new java.util.LinkedHashMap<>();
                tmResult.put("type", "content");
                tmResult.put("outputId", output.getId());
                tmResult.put("content", markdown != null ? markdown.trim() : "");
                taskStore.complete(taskId, tmResult);
                com.school.teaching.common.SchoolContext.clear();
                return;
            }

            if (pr.isQuestionType()) {
                // V055: 提前生成 batchId（saveQuestions 内部也会用同一个ID）
                String batchId = java.util.UUID.randomUUID().toString().substring(0, 8);
                pr.extraParams().put("_batchId", batchId);
                List<Map<String, Object>> saved;
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> viableKpsRaw = (List<Map<String, Object>>) params.get("_viableKps");
                final int originalExpectedTotal = resolveExpectedCount(pr.extraParams());
                int rawTotal = originalExpectedTotal;
                if (viableKpsRaw != null && !viableKpsRaw.isEmpty() && rawTotal >= 10) {
                    saved = generateKpAware(teacherId, pr.extraParams(), viableKpsRaw, rawTotal, nodeId);
                } else if (rawTotal > 20) {
                    saved = generateBatched(teacherId, pr.extraParams(), rawTotal);
                } else {
                    saved = aiQuestionService.generateFromAiAssistant(teacherId, pr.extraParams());
                }
                // V055: 题库混搭 — 从现有题库抽取高质量题混入AI结果
                try {
                    Long studentId = params.get("studentId") instanceof Number n ? n.longValue() : null;
                    saved = questionBankMixer.mix(nodeId, categoryIds, saved, studentId);
                } catch (Exception e) {
                    log.warn("题库混搭失败, 降级使用纯AI结果: {}", e.getMessage());
                }
                // V055-fix(REWRITE): 精确匹配各题型数量，不以总数为准
                int expectedTotal = originalExpectedTotal;
                @SuppressWarnings("unchecked")
                Map<String, Integer> typeTargets = (Map<String, Integer>) pr.extraParams().get("typeCounts");
                if (expectedTotal > 0 && typeTargets != null && !typeTargets.isEmpty()
                        && saved.size() != expectedTotal) {
                    // 按题型分组
                    Map<String, List<Map<String, Object>>> byType = new LinkedHashMap<>();
                    for (Map<String, Object> q : saved) {
                        String qt = String.valueOf(q.getOrDefault("questionType", "SINGLE_CHOICE"));
                        byType.computeIfAbsent(qt, k -> new ArrayList<>()).add(q);
                    }

                    // Step 1: 硬裁剪 — 每种题型精确裁到目标值，不受总 excess 限制
                    int before = saved.size();
                    for (Map.Entry<String, Integer> target : typeTargets.entrySet()) {
                        String type = target.getKey();
                        int targetCount = target.getValue();
                        List<Map<String, Object>> typeList = byType.get(type);
                        if (typeList != null && typeList.size() > targetCount) {
                            typeList.subList(targetCount, typeList.size()).clear();
                        }
                    }
                    // 清理空组
                    byType.values().removeIf(List::isEmpty);

                    // 重建 saved
                    saved = new ArrayList<>();
                    for (List<Map<String, Object>> group : byType.values()) {
                        saved.addAll(group);
                    }
                    int trimmedCount = before - saved.size();

                    // Step 2: 计算各题型缺额，按缺额比例用 AI 补齐
                    Map<String, Integer> gaps = new LinkedHashMap<>();
                    for (Map.Entry<String, Integer> target : typeTargets.entrySet()) {
                        String type = target.getKey();
                        int targetCount = target.getValue();
                        int actual = byType.getOrDefault(type, Collections.emptyList()).size();
                        int gap = targetCount - actual;
                        if (gap > 0) gaps.put(type, gap);
                    }

                    int totalGap = gaps.values().stream().mapToInt(Integer::intValue).sum();
                    if (trimmedCount > 0 || totalGap > 0) {
                        log.info("按题型裁剪: {}题 → {}题, 缺额: {}题 ({})",
                            before, saved.size(), totalGap, gaps);
                    }

                    if (totalGap > 0) {
                        int gapPct = expectedTotal > 0 ? totalGap * 100 / expectedTotal : 100;
                        if (gapPct <= 20) {
                            log.info("缺额{}题({}%) ≤20%阈值内，跳过AI补批", totalGap, gapPct);
                        } else {
                            log.info("题目不足: 期望{}题, 实际{}题, 缺额{}({}%) — 按缺额比例补齐: {}",
                                expectedTotal, saved.size(), totalGap, gapPct, gaps);
                            try {
                                Map<String, Object> cParams = new java.util.LinkedHashMap<>(pr.extraParams());
                                cParams.put("typeCounts", gaps);
                                // 调 AI 按缺额比例补题，使用小批量生成
                                List<Map<String, Object>> cBatch = aiQuestionService.generateFromAiAssistant(teacherId, cParams);
                                if (cBatch != null && !cBatch.isEmpty()) {
                                    // 补批的题也需要写入同一个 batchId 便于后续清理
                                    String cbBatchId = pr.extraParams().get("_batchId") instanceof String bs ? bs : batchId;
                                    int added = 0;
                                    for (Map<String, Object> q : cBatch) {
                                        if (!(q.get("id") instanceof Number)) continue;
                                        if (added >= totalGap) break;
                                        saved.add(q);
                                        added++;
                                    }
                                    log.info("缺额补齐成功: 需补{}题, AI返回{}题, 实际补入{}题", totalGap, cBatch.size(), added);
                                }
                            } catch (Exception e) {
                                log.error("缺额AI补批失败: {}", e.getMessage());
                            }
                        }
                    }
                    if (saved.size() < expectedTotal) {
                        log.warn("补齐后仍不足: 期望{}题, 实际{}题", expectedTotal, saved.size());
                    }
                    // ★ 最终保险：总题数超出预期 10% 以上时，按题型比例裁减
                    int capMax = (int) Math.ceil(expectedTotal * 1.10);
                    if (saved.size() > capMax) {
                        log.warn("最终总数超出上限: {}题 > {}题, 执行终裁", saved.size(), capMax);
                        java.util.Map<String, java.util.List<Map<String, Object>>> finalByType = new java.util.LinkedHashMap<>();
                        for (Map<String, Object> q : saved) {
                            String qt = String.valueOf(q.getOrDefault("questionType", "SINGLE_CHOICE"));
                            finalByType.computeIfAbsent(qt, k -> new java.util.ArrayList<>()).add(q);
                        }
                        int excess = saved.size() - expectedTotal;
                        java.util.List<String> typeKeys = new java.util.ArrayList<>(finalByType.keySet());
                        for (int i = 0; excess > 0 && i < typeKeys.size(); i++) {
                            String type = typeKeys.get(i);
                            java.util.List<Map<String, Object>> list = finalByType.get(type);
                            if (list == null || list.size() <= 1) continue;
                            int canCut = Math.min(excess, list.size() - 1);
                            list.subList(list.size() - canCut, list.size()).clear();
                            excess -= canCut;
                        }
                        saved = new java.util.ArrayList<>();
                        for (java.util.List<Map<String, Object>> group : finalByType.values()) {
                            saved.addAll(group);
                        }
                        log.info("终裁完成: {}题", saved.size());
                    }
                }
                // 清理已入库但被混搭/裁剪移除的题目（防止预览/编辑时多出题目）
                java.util.Set<Long> keepIds = saved.stream()
                    .map(q -> q.get("id"))
                    .filter(id -> id instanceof Number)
                    .map(id -> ((Number) id).longValue())
                    .collect(java.util.stream.Collectors.toSet());
                if (!keepIds.isEmpty()) {
                    try {
                        int deleted = questionBankMapper.delete(
                            new LambdaQueryWrapper<QuestionBank>()
                                .apply("JSON_EXTRACT(content_json, '$.batchId') = {0}", batchId)
                                .notIn(QuestionBank::getId, keepIds)
                        );
                        if (deleted > 0) {
                            log.info("清理了{}道被替换/裁剪的题目(batchId={})", deleted, batchId);
                        }
                    } catch (Exception e) {
                        log.warn("清理已入库题目失败: {}", e.getMessage());
                    }
                }
                // 从 extraParams 中取出 tokensUsed（由 generateQuestions → callDeepSeek 回写）
                Object tk = pr.extraParams().get("_tokensUsed");
                if (tk instanceof Number n) tokensUsed = n.intValue();
                result.put("type", "questions");
                result.put("questions", saved);
                result.put("count", saved.size());
                // V055-fix: 透传入库过滤统计，供前端提示
                Object dupSkip = pr.extraParams().get("_dupSkipped");
                Object lowQReject = pr.extraParams().get("_lowQualityRejected");
                if (dupSkip instanceof Number n && n.intValue() > 0) result.put("dupSkipped", n.intValue());
                if (lowQReject instanceof Number n && n.intValue() > 0) result.put("lowQualityRejected", n.intValue());
                int diagCount = 0;
                for (Map<String, Object> question : saved) {
                    Object diagramObj = question.get("diagram");
                    if (diagramObj instanceof Map<?, ?> diagram) {
                        try {
                            String svgUrl = geomRenderService.renderAndUpload(
                                (Map<String, Object>) diagram, teacherId);
                            if (svgUrl != null) {
                                String origText = (String) question.getOrDefault("questionText", "");
                                String newText = origText + "\n[图片](" + svgUrl + ")";
                                question.put("questionText", newText);
                                Long qid = question.get("id") instanceof Number n ? n.longValue() : null;
                                if (qid != null) {
                                    questionBankMapper.updateQuestionText(qid, newText);
                                }
                                diagCount++;
                            }
                        } catch (Exception e) {
                            log.warn("几何图形渲染失败，降级为纯文本: questionId={}", question.get("id"), e);
                        }
                    }
                }
                result.put("diagramCount", diagCount);
                // 知识点覆盖率统计
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> inputKps = (List<Map<String, Object>>) params.get("_viableKps");
                if (inputKps != null && !inputKps.isEmpty()) {
                    Map<Long, Integer> kpCounts = new LinkedHashMap<>();
                    Map<Long, String> kpNames = new LinkedHashMap<>();
                    for (Map<String, Object> kp : inputKps) {
                        Long kpId = kp.get("id") instanceof Number n ? n.longValue() : null;
                        String kpName = kp.get("name") instanceof String s ? s : "?";
                        if (kpId != null) { kpCounts.put(kpId, 0); kpNames.put(kpId, kpName); }
                    }
                    for (Map<String, Object> q : saved) {
                        Long kpId = null;
                        Object kpObj = q.get("knowledgeNodeId");
                        if (kpObj instanceof Number nn) kpId = nn.longValue();
                        else {
                            Object catObj = q.get("categoryId");
                            if (catObj instanceof Number nn) kpId = nn.longValue();
                        }
                        if (kpId != null) kpCounts.merge(kpId, 1, Integer::sum);
                    }
                    List<Map<String, Object>> coverage = new ArrayList<>();
                    StringBuilder sb = new StringBuilder("知识点覆盖: ");
                    for (Map.Entry<Long, Integer> e : kpCounts.entrySet()) {
                        String name = kpNames.getOrDefault(e.getKey(), "?");
                        int cnt = e.getValue();
                        sb.append(name).append(":").append(cnt).append("题");
                        if (cnt == 0) sb.append("(空白!)");
                        sb.append(", ");
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("kpId", e.getKey());
                        item.put("kpName", name);
                        item.put("count", cnt);
                        coverage.add(item);
                    }
                    if (sb.length() > 500) { sb.setLength(497); sb.append("..."); }
                    log.info("{}", sb);
                    result.put("kpCoverage", coverage);
                }
                // 传递 batchId + scorePresets 回前端
                result.put("batchId", batchId);
                // 传递 scorePresets 回前端，方便组卷发布预填
                if (pr.extraParams().get("scorePresets") instanceof Map<?,?> sp) {
                    result.put("scorePresets", sp);
                }

                // 写入 ai_outputs 以便在历史记录中查看
                int questionLatency = (int) (System.currentTimeMillis() - start);
                AiOutput qOutput = new AiOutput();
                qOutput.setNodeId(nodeId != null ? nodeId : 0L);
                qOutput.setTeacherId(teacherId);
                qOutput.setOutputType(contentType);
                qOutput.setTitle((String) params.getOrDefault("knowledgePoint",
                    (String) params.getOrDefault("subject", "AI出题")));
                qOutput.setSubject((String) params.getOrDefault("subject", ""));
                // 存储完整题目 JSON（含 options/answer/explanation），而非仅题干摘要
                qOutput.setContent(om.writeValueAsString(
                    Map.of("questions", saved, "count", saved.size())));
                qOutput.setIsLatest(1);
                qOutput.setVersionSeq(1);
                qOutput.setStatus(0);
                qOutput.setTokensUsed(tokensUsed);
                qOutput.setLatencyMs(questionLatency);
                qOutput.setCreatedAt(java.time.LocalDateTime.now());
                outputMapper.insert(qOutput);
                result.put("outputId", qOutput.getId());
            } else {
                Map<String, Object> contentParams = new LinkedHashMap<>();
                contentParams.put("prompt", pr.prompt());
                contentParams.put("maxTokens", pr.maxTokens());
                contentParams.put("temperature", 0.7);
                String markdown = aiGateway.generateContent(contentParams);
                // 从 contentParams 中取出 tokensUsed（由 generateContent 回写）
                Object tk = contentParams.get("_tokensUsed");
                if (tk instanceof Number n) tokensUsed = n.intValue();

                int latency = (int) (System.currentTimeMillis() - start);

                // 版本管理：查询当前最大版本号 → 插入 version_seq = max+1
                AiOutput maxVerEntity = outputMapper.selectOne(
                        new LambdaQueryWrapper<AiOutput>()
                                .eq(AiOutput::getNodeId, nodeId)
                                .eq(AiOutput::getOutputType, contentType)
                                .orderByDesc(AiOutput::getVersionSeq)
                                .last("LIMIT 1"));
                int newVersion = (maxVerEntity != null && maxVerEntity.getVersionSeq() != null
                        ? maxVerEntity.getVersionSeq() : 0) + 1;

                AiOutput output = new AiOutput();
                output.setNodeId(nodeId != null ? nodeId : 0L);
                output.setTeacherId(teacherId);
                output.setOutputType(contentType);
                output.setTitle((String) params.getOrDefault("knowledgePoint", (String) params.getOrDefault("subject", "")));
                output.setSubject((String) params.getOrDefault("subject", ""));
                output.setContent(markdown);
                output.setIsLatest(1);
                output.setVersionSeq(newVersion);
                output.setStatus(0);
                output.setTokensUsed(tokensUsed);
                output.setLatencyMs(latency);
                // 事务性保存：更新旧版本 + 插入新版本 + 清理历史
                saveOutputWithVersion(output, nodeId, contentType);

                result.put("type", "content");
                result.put("id", output.getId());
                result.put("content", markdown);
                result.put("outputType", contentType);
                result.put("title", (String) params.getOrDefault("knowledgePoint",
                        (String) params.getOrDefault("subject", "未命名")));
            }

            // 已由 generateFromAiAssistant 内部记录 QUESTION_GEN 日志，出题路径不再重复记录
            if (!pr.isQuestionType()) {
                AiCallLog callLog = new AiCallLog();
                callLog.setSchoolId(1L);
                callLog.setUserId(teacherId);
                callLog.setCapability("CONTENT_GEN");
                callLog.setProvider(aiGateway.getProvider());
                callLog.setTokensUsed(tokensUsed);
                callLog.setLatencyMs((int) (System.currentTimeMillis() - start));
                callLog.setStatus("SUCCESS");
                callLog.setPromptTokens(tokensUsed / 3);
                callLog.setCompletionTokens(tokensUsed * 2 / 3);
                aiCallLogMapper.insert(callLog);
                aiMetrics.recordCall("CONTENT_GEN", "SUCCESS", tokensUsed, tokensUsed / 3, tokensUsed * 2 / 3, (int) (System.currentTimeMillis() - start), null);
            }

            taskStore.complete(taskId, result);
            com.school.teaching.common.SchoolContext.clear();
        } catch (Exception e) {
            log.error("AI内容生成失败: taskId={}, teacherId={}", taskId, teacherId, e);
            aiMetrics.recordFailure("CONTENT_GEN", (int) (System.currentTimeMillis() - start));
            taskStore.fail(taskId, e.getMessage());
        } finally {
            com.school.teaching.common.SchoolContext.clear();
            AI_CONCURRENCY_LIMIT.release();
        }
    }

    @Override
    public List<AiOutput> listOutputs(Long teacherId, String outputType, String keyword, Integer page, Integer pageSize) {
        LambdaQueryWrapper<AiOutput> w = new LambdaQueryWrapper<AiOutput>()
                .eq(AiOutput::getTeacherId, teacherId)
                .eq(outputType != null && !outputType.isEmpty(), AiOutput::getOutputType, outputType)
                .eq(AiOutput::getIsLatest, 1)
                .ne(AiOutput::getStatus, 2);  // 默认不显示已归档
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            // 关键词过短会导致全表 LIKE 扫描，限制最小长度
            if (kw.length() >= 2) {
                w.and(q -> q.like(AiOutput::getContent, kw).or().like(AiOutput::getTitle, kw));
            }
        }
        w.orderByDesc(AiOutput::getCreatedAt);
        int pn = page != null && page > 0 ? page : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 20;
        if (pageSize != null && pageSize == -1) {
            return outputMapper.selectList(w);
        }
        return outputMapper.selectPage(new Page<>(pn, ps), w).getRecords();
    }

    @Override
    public AiOutput getById(Long id) {
        return outputMapper.selectById(id);
    }

    @Override
    @Transactional
    public void updateOutput(Long id, AiOutput update) {
        AiOutput entity = outputMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "产出不存在");
        checkOwnership(entity);
        if (update.getContent() != null) entity.setContent(update.getContent());
        outputMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        AiOutput entity = outputMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "产出不存在");
        checkOwnership(entity);
        entity.setStatus(1);
        outputMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void archive(Long id) {
        AiOutput entity = outputMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "产出不存在");
        checkOwnership(entity);
        entity.setStatus(2);
        outputMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void rate(Long id, Integer rating, String feedback) {
        AiOutput entity = outputMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "产出不存在");
        checkOwnership(entity);
        if (rating != null) entity.setRating(rating);
        if (feedback != null) entity.setFeedback(feedback);
        outputMapper.updateById(entity);
    }

    /** 校验当前用户是否为产出所有者或管理员，防止越权操作 */
    private void checkOwnership(AiOutput entity) {
        if (SecurityUtils.isAdmin()) return;
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(entity.getTeacherId())) {
            log.warn("越权操作: userId={} 尝试操作他人产出 outputId={}, ownerId={}",
                currentUserId, entity.getId(), entity.getTeacherId());
            throw new BusinessException(403, "无权操作他人的产出");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> publishAsTask(Long id, Map<String, Object> config) {
        AiOutput entity = outputMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "产出不存在");
        if (!"PRACTICE_PLAN".equals(entity.getOutputType()))
            throw new BusinessException(400, "仅实训方案可发布为任务");
        // 幂等性检查：已发布的不能再发
        if (entity.getFeedback() != null && entity.getFeedback().startsWith("已转为实训任务"))
            throw new BusinessException(409, "该产出已发布为实训任务，请勿重复发布");

        String content = entity.getContent();
        if (content == null || content.isBlank())
            throw new BusinessException(400, "产出内容为空，无法发布");

        // 提取JSON元数据
        var matcher = JSON_BLOCK.matcher(content);
        if (!matcher.find()) throw new BusinessException(400, "未找到任务元数据JSON，请重新生成实训方案");
        String jsonStr = matcher.group(1);
        Map<String, Object> meta;
        try { meta = om.readValue(jsonStr, Map.class); }
        catch (Exception e) { throw new BusinessException(400, "任务元数据解析失败: " + e.getMessage()); }

        // 构建Task
        com.school.teaching.entity.Task task = new com.school.teaching.entity.Task();
        task.setTitle((String) meta.getOrDefault("title", "实训任务"));
        task.setDescription(cleanMarkdownForTask(content));
        task.setTaskType("PRACTICE");
        task.setScoreType("POINT_100");
        task.setTotalScore(java.math.BigDecimal.valueOf(
            ((Number) config.getOrDefault("totalScore", 100)).intValue()));
        task.setGradeId(((Number) config.get("gradeId")).longValue());
        task.setDeadline(config.get("deadline") instanceof String s
            ? java.time.LocalDateTime.parse(s) : null);
        task.setAllowCustomSteps(
            Boolean.TRUE.equals(config.get("allowCustomSteps")) ? 1 : 0);
        task.setSubject((String) config.getOrDefault("subject", ""));
        task.setTeacherId(resolveTeacherId(entity.getTeacherId()));
        task.setTargetType("CLASS");
        task.setStatus("PUBLISHED");

        // targetIds → 多班级创建（批量插入）
        @SuppressWarnings("unchecked")
        var classIds = (java.util.List<Integer>) config.get("classIds");
        Long firstTaskId = null;
        if (classIds != null && !classIds.isEmpty()) {
            List<com.school.teaching.entity.Task> batchTasks = new java.util.ArrayList<>();
            for (Integer cid : classIds) {
                com.school.teaching.entity.Task copy = copyTask(task, cid.longValue());
                copy.setStatus("PUBLISHED");
                if (copy.getSchoolId() == null) copy.setSchoolId(1L);
                if (copy.getTeacherId() == null) copy.setTeacherId(resolveTeacherId(entity.getTeacherId()));
                batchTasks.add(copy);
            }
            com.baomidou.mybatisplus.extension.toolkit.Db.saveBatch(batchTasks);
            firstTaskId = batchTasks.get(0).getId();
        } else {
            Long targetId = config.get("classId") instanceof Number n ? n.longValue() : null;
            com.school.teaching.entity.Task taskCopy = new com.school.teaching.entity.Task();
            // 复制字段
            taskCopy.setTitle(task.getTitle());
            taskCopy.setTaskType(task.getTaskType());
            taskCopy.setScoreType(task.getScoreType());
            taskCopy.setSubject(task.getSubject());
            taskCopy.setDescription(task.getDescription());
            taskCopy.setTargetType(task.getTargetType());
            taskCopy.setTargetId(targetId);
            taskCopy.setTaskConfig(task.getTaskConfig());
            taskCopy.setQuestionIds(task.getQuestionIds());
            com.school.teaching.entity.Task created = taskService.create(taskCopy);
            firstTaskId = created.getId();
        }

        // 标记产出（不覆盖用户评分）
        entity.setFeedback("已转为实训任务 #" + firstTaskId);
        outputMapper.updateById(entity);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("taskId", firstTaskId);
        result.put("title", task.getTitle());
        result.put("steps", meta.get("steps"));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> publishQuestionsAsExam(Long teacherId, List<Long> questionIds, Map<String, Object> config) {
        if (questionIds == null || questionIds.isEmpty())
            throw new BusinessException(400, "没有可发布的题目");

        // 从DB加载题目，过滤已删除(status=-1)和已驳回(status=2)的
        java.util.List<com.school.teaching.entity.QuestionBank> questionList = questionBankMapper.selectBatchIds(questionIds);

        // 统计过滤掉的题目
        java.util.List<Long> filteredOut = questionList.stream()
            .filter(q -> q.getStatus() != null && (q.getStatus() == -1 || q.getStatus() == 2))
            .map(com.school.teaching.entity.QuestionBank::getId)
            .toList();
        if (!filteredOut.isEmpty()) {
            log.warn("发布时过滤掉{}道已删除/已驳回题目: {}", filteredOut.size(), filteredOut);
        }

        // 只保留有效题目(status=0草稿 或 status=1已审核)
        boolean requireReview = systemService.getBooleanConfig("feature.require_question_review", false);
        java.util.List<Long> validQuestionIds;
        if (requireReview) {
            // 审核模式：只包含已审核通过的题目，草稿不入卷
            validQuestionIds = questionList.stream()
                .filter(q -> q.getStatus() != null && q.getStatus() == 1)
                .map(com.school.teaching.entity.QuestionBank::getId)
                .toList();
        } else {
            // 快速模式：草稿自动审核通过后入卷（默认行为）
            validQuestionIds = questionList.stream()
                .filter(q -> q.getStatus() == null || q.getStatus() == 0 || q.getStatus() == 1)
                .map(com.school.teaching.entity.QuestionBank::getId)
                .toList();

            java.util.List<Long> toApprove = questionList.stream()
                .filter(q -> q.getStatus() == null || q.getStatus() == 0)
                .map(com.school.teaching.entity.QuestionBank::getId)
                .toList();
            if (!toApprove.isEmpty()) {
                questionBankMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.school.teaching.entity.QuestionBank>()
                        .in(com.school.teaching.entity.QuestionBank::getId, toApprove)
                        .set(com.school.teaching.entity.QuestionBank::getStatus, 1));
            }
        }

        if (validQuestionIds.isEmpty())
            throw new BusinessException(400, "没有有效题目可发布（所有题目已被删除或驳回）");

        // 创建Task
        com.school.teaching.entity.Task task = new com.school.teaching.entity.Task();
        task.setTitle((String) config.getOrDefault("title", "AI组卷"));
        task.setTaskType("SUMMATIVE");
        int totalScore = ((Number) config.getOrDefault("totalScore", 100)).intValue();
        task.setScoreType(totalScore > 100 ? "POINT_150" : "POINT_100");
        task.setSubject((String) config.getOrDefault("subject", ""));
        task.setTotalScore(java.math.BigDecimal.valueOf(totalScore));
        task.setGradeId(config.get("gradeId") instanceof Number n ? n.longValue() : null);
        task.setDeadline(config.get("deadline") instanceof String s
            ? java.time.LocalDateTime.parse(s) : null);
        task.setTeacherId(resolveTeacherId(teacherId));
        task.setTargetType("CLASS");
        task.setStatus("DRAFT");  // create() 会设置 DRAFT，之后主动 publish
        task.setQuestionIds(validQuestionIds);

        // 考试配置：taskConfig（乱序/防作弊/时长等）
        Object taskConfigObj = config.get("taskConfig");
        if (taskConfigObj instanceof String s && !s.isEmpty()) {
            task.setTaskConfig(s);
        } else if (taskConfigObj instanceof java.util.Map) {
            try { task.setTaskConfig(om.writeValueAsString(taskConfigObj)); } catch (Exception e) { log.warn("taskConfig JSON 序列化失败: {}", e.getMessage()); }
        }
        // 分值预设
        Object scorePresetsObj = config.get("scorePresets");
        if (scorePresetsObj instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Integer> presets = (java.util.Map<String, Integer>) scorePresetsObj;
            task.setScorePresets(presets);
        }
        // 其他任务属性
        if (config.get("autoWrongbook") instanceof Boolean b) task.setAutoWrongbook(b ? 1 : 0);
        if (config.get("isRequired") instanceof Boolean b) task.setIsRequired(b ? 1 : 0);
        if (config.get("notifyParents") instanceof Boolean b) task.setNotifyParents(b ? 1 : 0);
        if (config.get("allowResubmit") instanceof Boolean b) task.setAllowResubmit(b ? 1 : 0);
        if (config.get("durationMinutes") instanceof Number n) {
            // E1: 将 durationMinutes 写入 taskConfig JSON，确保 ExamTaskHandler 能读取做超时校验
            try {
                String existing = task.getTaskConfig();
                java.util.Map<String, Object> cfg = (existing != null && !existing.isBlank())
                    ? om.readValue(existing, new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() {})
                    : new java.util.LinkedHashMap<>();
                cfg.put("durationMinutes", n.intValue());
                task.setTaskConfig(om.writeValueAsString(cfg));
            } catch (Exception e) { log.warn("durationMinutes 写入 taskConfig 失败: {}", e.getMessage()); }
            task.setDescription("考试时长：" + n.intValue() + "分钟");
        }

        // T-01: 统计实际难度分布并存入 taskConfig
        try {
            int easy = 0, medium = 0, hard = 0;
            for (com.school.teaching.entity.QuestionBank q : questionList) {
                if (q.getDifficultyLevel() != null) {
                    if (q.getDifficultyLevel() <= 2) easy++;
                    else if (q.getDifficultyLevel() >= 4) hard++;
                    else medium++;
                }
            }
            int total = easy + medium + hard;
            if (total > 0) {
                String existing = task.getTaskConfig();
                java.util.Map<String, Object> cfg = (existing != null && !existing.isBlank())
                    ? om.readValue(existing, new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() {})
                    : new java.util.LinkedHashMap<>();
                java.util.Map<String, Integer> ratio = new java.util.LinkedHashMap<>();
                ratio.put("EASY", easy * 100 / total);
                ratio.put("MEDIUM", medium * 100 / total);
                ratio.put("HARD", hard * 100 / total);
                cfg.put("difficultyRatio", ratio);
                task.setTaskConfig(om.writeValueAsString(cfg));
            }
        } catch (Exception e) { log.warn("difficultyRatio 统计写入失败: {}", e.getMessage()); }

        // T-03: 将学科考纲写入 taskConfig（供考试报告/学情分析后续使用）
        try {
            if (!questionList.isEmpty()) {
                com.school.teaching.entity.QuestionBank firstQ = questionList.get(0);
                if (firstQ.getCategoryId() != null) {
                    Long subjectId = knowledgeNodeService.findSubjectRoot(firstQ.getCategoryId());
                    if (subjectId != null) {
                        String syllabusMeta = examSyllabusService.getSyllabusMeta(subjectId);
                        if (syllabusMeta != null && !syllabusMeta.isEmpty()) {
                            String existing = task.getTaskConfig();
                            java.util.Map<String, Object> cfg = (existing != null && !existing.isBlank())
                                ? om.readValue(existing, new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() {})
                                : new java.util.LinkedHashMap<>();
                            cfg.put("syllabusMeta", syllabusMeta);
                            task.setTaskConfig(om.writeValueAsString(cfg));
                        }
                    }
                }
            }
        } catch (Exception e) { log.warn("考纲元数据写入 taskConfig 失败: {}", e.getMessage()); }

        @SuppressWarnings("unchecked")
        var classIds = (java.util.List<Integer>) config.get("classIds");
        Long firstTaskId = null;
        if (classIds != null && !classIds.isEmpty()) {
            for (Integer cid : classIds) {
                com.school.teaching.entity.Task taskForClass = new com.school.teaching.entity.Task();
                taskForClass.setTitle(task.getTitle());
                taskForClass.setTaskType(task.getTaskType());
                taskForClass.setScoreType(task.getScoreType());
                taskForClass.setSubject(task.getSubject());
                taskForClass.setTotalScore(task.getTotalScore());
                taskForClass.setGradeId(task.getGradeId());
                taskForClass.setDeadline(task.getDeadline());
                taskForClass.setTeacherId(task.getTeacherId());
                taskForClass.setTargetType("CLASS");
                taskForClass.setTargetId(cid.longValue());
                taskForClass.setStatus("DRAFT");
                taskForClass.setQuestionIds(validQuestionIds);
                taskForClass.setTaskConfig(task.getTaskConfig());
                taskForClass.setScorePresets(task.getScorePresets());
                taskForClass.setAutoWrongbook(task.getAutoWrongbook());
                taskForClass.setIsRequired(task.getIsRequired());
                taskForClass.setNotifyParents(task.getNotifyParents());
                taskForClass.setAllowResubmit(task.getAllowResubmit());
                taskForClass.setDescription(task.getDescription());
                com.school.teaching.entity.Task created = taskService.create(taskForClass);
                if (firstTaskId == null) firstTaskId = created.getId();
                taskService.publish(created.getId());  // DRAFT → PUBLISHED
            }
        } else {
            com.school.teaching.entity.Task created = taskService.create(task);
            firstTaskId = created.getId();
            taskService.publish(created.getId());
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("taskId", firstTaskId);
        result.put("title", task.getTitle());
        result.put("questionCount", validQuestionIds.size());
        if (!filteredOut.isEmpty()) {
            result.put("filteredCount", filteredOut.size());
            result.put("filteredReason", "已删除或已驳回的题目已被自动排除");
        }
        return result;
    }

    @Override
    public String exportMarkdown(Long id) {
        AiOutput entity = outputMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "产出不存在");
        return entity.getContent();
    }

    /** 精简RAG上下文：仅包含当前节点自身内容，用于同步练习等不需跨知识点的场景 */
    private String buildLeanRagContext(Long nodeId) {
        if (nodeId == null) return "";
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) return "";
        if (node.getContent() == null || node.getContent().isEmpty()) return "";
        return "【" + node.getName() + "】\n" + node.getContent();
    }

    /** 递归收集节点下所有子孙知识点（L3/L4），用于多选章节展开 */
    private void collectAllSubKps(Long nodeId, List<Map<String, Object>> result, Set<Long> visited) {
        if (nodeId == null || !visited.add(nodeId)) return;
        List<KnowledgeNode> children = nodeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getParentId, nodeId));
        if (children == null || children.isEmpty()) return;
        for (KnowledgeNode child : children) {
            // 叶子节点(L3/L4且有content)视为知识点
            if (child.getLevel() != null && child.getLevel() >= 3 && child.getContent() != null && !child.getContent().isBlank()) {
                result.add(Map.of("id", child.getId(), "name", child.getName() != null ? child.getName() : ""));
            }
            // 继续递归（L2→L3→L4）
            if (child.getLevel() != null && child.getLevel() <= 3) {
                collectAllSubKps(child.getId(), result, visited);
            }
        }
    }

    @Override
    public String buildRagContext(Long nodeId) {
        if (nodeId == null) return "";
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) return "";

        // 4级层级：1=学科 2=章节 3=任务 4=知识点
        Integer level = node.getLevel();
        if (level != null && level == 2) {
            return buildChapterLevelContext(node);
        }

        List<KnowledgeNode> childKps = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getParentId, nodeId));
        boolean isTaskLevel = !childKps.isEmpty();

        if (isTaskLevel) {
            return buildTaskLevelContext(node, childKps);
        }
        return buildKnowledgePointContext(node);
    }

    private static final int MAX_NODE_CONTENT = 500;  // 单节点内容上限
    private static final int MAX_CONTEXT_LENGTH = 12000; // 上下文总长度上限
    private static final int MAX_TASKS_PER_CHAPTER = 6;  // 每章最多任务数
    private static final int MAX_KPS_PER_TASK = 4;        // 每任务最多知识点数

    /** 章节级上下文：章节概述 + 任务列表 + 核心知识点（总量控制，防止 prompt 过大导致 AI 超时） */
    private String buildChapterLevelContext(KnowledgeNode chapter) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(chapter.getName()).append("】\n");
        if (chapter.getContent() != null && !chapter.getContent().isEmpty()) {
            sb.append(truncateContent(chapter.getContent())).append("\n\n");
        }

        List<KnowledgeNode> tasks = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getParentId, chapter.getId())
                        .orderByAsc(KnowledgeNode::getSortOrder)
                        .last("LIMIT " + MAX_TASKS_PER_CHAPTER));

        for (KnowledgeNode task : tasks) {
            sb.append("【任务：").append(task.getName()).append("】\n");
            if (task.getContent() != null && !task.getContent().isEmpty()) {
                sb.append(truncateContent(task.getContent())).append("\n");
            }

            List<KnowledgeNode> kps = nodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getParentId, task.getId())
                            .orderByAsc(KnowledgeNode::getSortOrder)
                            .last("LIMIT " + MAX_KPS_PER_TASK));
            int kpCount = 0;
            for (KnowledgeNode kp : kps) {
                if (kp.getContent() != null && !kp.getContent().isEmpty()) {
                    sb.append("  【").append(kp.getName()).append("】\n  ")
                      .append(truncateContent(kp.getContent())).append("\n");
                    kpCount++;
                }
            }
            if (kpCount > 0) sb.append("\n");
        }

        String result = sb.toString();
        if (result.length() > MAX_CONTEXT_LENGTH)
            result = result.substring(0, MAX_CONTEXT_LENGTH) + "\n...(共" + tasks.size() + "任务，内容已截断)";
        return result;
    }

    /** 截断单节点内容到合理长度，保留完整段落 */
    private String truncateContent(String content) {
        if (content == null || content.isEmpty()) return "";
        if (content.length() <= MAX_NODE_CONTENT) return content;
        // 尝试在段落边界截断
        int cut = content.lastIndexOf('\n', MAX_NODE_CONTENT);
        if (cut > MAX_NODE_CONTENT / 2) return content.substring(0, cut) + "\n...";
        return content.substring(0, MAX_NODE_CONTENT) + "...";
    }

    private String buildTaskLevelContext(KnowledgeNode task, List<KnowledgeNode> childKps) {
        StringBuilder sb = new StringBuilder();

        if (task.getParentId() != null) {
            KnowledgeNode chapter = nodeMapper.selectById(task.getParentId());
            if (chapter != null && chapter.getContent() != null && !chapter.getContent().isEmpty()) {
                sb.append("【").append(chapter.getName()).append("(章节概述)】\n")
                        .append(truncateContent(chapter.getContent())).append("\n\n");
            }
        }

        sb.append("【任务：").append(task.getName()).append("】\n");
        if (task.getContent() != null && !task.getContent().isEmpty()) {
            sb.append(truncateContent(task.getContent())).append("\n\n");
        }

        childKps.sort(java.util.Comparator.comparing(KnowledgeNode::getSortOrder));
        int showCount = Math.min(childKps.size(), MAX_KPS_PER_TASK);
        sb.append("--- 本任务包含以下 ").append(showCount).append(" 个核心知识点 ---\n\n");
        for (int i = 0; i < showCount; i++) {
            KnowledgeNode kp = childKps.get(i);
            if (kp.getContent() != null && !kp.getContent().isEmpty()) {
                sb.append("【").append(kp.getName()).append("】\n").append(truncateContent(kp.getContent())).append("\n\n");
            }
        }
        if (childKps.size() > showCount)
            sb.append("(还有 ").append(childKps.size() - showCount).append(" 个知识点，内容已省略)\n");
        return sb.toString();
    }

    private String buildKnowledgePointContext(KnowledgeNode kp) {
        StringBuilder sb = new StringBuilder();
        String targetName = kp.getName() != null ? kp.getName() : "";

        // 当前知识点：按段落过滤，只取最相关的5段
        if (kp.getContent() != null && !kp.getContent().isEmpty()) {
            String filtered = filterRelevantSections(kp.getContent(), targetName, 5);
            sb.append("【").append(kp.getName()).append("】\n").append(filtered).append("\n\n");
        } else {
            // 父节点内容回退：当前节点无内容时，向上查找父节点
            if (kp.getParentId() != null) {
                KnowledgeNode parent = nodeMapper.selectById(kp.getParentId());
                if (parent != null && parent.getContent() != null && !parent.getContent().isEmpty()) {
                    sb.append("【").append(kp.getName()).append("】\n")
                      .append(parent.getContent()).append("\n\n");
                }
            }
        }

        // 父级概述：按段落过滤，取最相关的3段
        if (kp.getParentId() != null) {
            KnowledgeNode parent = nodeMapper.selectById(kp.getParentId());
            if (parent != null && parent.getContent() != null && !parent.getContent().isEmpty()) {
                String label = (parent.getLevel() != null && parent.getLevel() == 3) ? "任务概述" : "章节概述";
                String filtered = filterRelevantSections(parent.getContent(), targetName, 3);
                sb.append("【").append(parent.getName()).append("(").append(label).append(")】\n")
                        .append(filtered).append("\n\n");
            } else if (parent != null && parent.getParentId() != null) {
                KnowledgeNode grandParent = nodeMapper.selectById(parent.getParentId());
                if (grandParent != null && grandParent.getContent() != null && !grandParent.getContent().isEmpty()) {
                    String filtered = filterRelevantSections(grandParent.getContent(), targetName, 3);
                    sb.append("【").append(grandParent.getName()).append("(章节概述)】\n")
                            .append(filtered).append("\n\n");
                }
            }
        }

        // 兄弟知识点：按名称+内容重叠度排序，取最相关N个，每个只保留最相关的3段
        if (kp.getParentId() != null) {
            List<KnowledgeNode> siblings = nodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getParentId, kp.getParentId())
                            .ne(KnowledgeNode::getId, kp.getId()));
            siblings.sort((a, b) -> {
                int scoreA = relevanceScore(targetName, a.getName());
                int scoreB = relevanceScore(targetName, b.getName());
                if (scoreA != scoreB) return Integer.compare(scoreB, scoreA);
                // 内容重叠度作为二级排序
                return Integer.compare(
                        relevanceScore(targetName, a.getContent()),
                        relevanceScore(targetName, b.getContent()));
            });

            int currentLen = sb.length();
            int maxSiblings;
            if (currentLen < 2000) maxSiblings = 5;
            else if (currentLen < 4000) maxSiblings = 3;
            else maxSiblings = 2;

            int added = 0;
            for (KnowledgeNode sib : siblings) {
                if (added >= maxSiblings) break;
                if (sib.getContent() != null && !sib.getContent().isEmpty()) {
                    String filtered = filterRelevantSections(sib.getContent(), targetName, 3);
                    if (!filtered.isEmpty()) {
                        sb.append("【").append(sib.getName()).append("】\n").append(filtered).append("\n\n");
                        added++;
                    }
                }
            }
        }

        String result = sb.toString();
        if (result.length() > 80000) result = result.substring(0, 80000) + "\n...(内容已截断)";
        return result;
    }

    private void injectNodePath(Map<String, Object> params, Long nodeId) {
        if (nodeId == null) return;
        List<String> pathParts = new ArrayList<>();
        Long current = nodeId;
        while (current != null) {
            KnowledgeNode node = nodeMapper.selectById(current);
            if (node == null) break;
            pathParts.add(0, node.getName());
            current = node.getParentId();
        }
        params.putIfAbsent("categoryPath", String.join(" → ", pathParts));
        params.putIfAbsent("nodeId", nodeId);
        if (!pathParts.isEmpty()) {
            String nodeName = pathParts.get(pathParts.size() - 1);
            params.putIfAbsent("knowledgePoint", nodeName);
            if (pathParts.size() >= 4) {
                params.putIfAbsent("taskName", pathParts.get(pathParts.size() - 2));
            }
        }
    }

    // 并发防护锁：selectCount + 业务判断不是原子操作，加锁防止并发超额
    // 信号量 AI_CONCURRENCY_LIMIT(10) 已限制最大并发，单锁即可，无需锁池
    private static final Object QUOTA_LOCK = new Object();

    private void checkQuota(Long teacherId, String contentType) {
        String quotaKey = "ai.daily_quota";
        String capability;
        if ("DIAGNOSIS".equals(contentType)) {
            quotaKey = "ai.diagnose.daily_quota";
            capability = "DIAGNOSIS";
        } else if ("CONSOLIDATION_MATERIAL".equals(contentType)) {
            quotaKey = "ai.consolidation.daily_quota";
            capability = "CONSOLIDATION_MATERIAL";
        } else if ("EXAM_PAPER".equals(contentType)) {
            quotaKey = "ai.exam_paper.daily_quota";
            capability = "EXAM_PAPER";
        } else if (isQuestionContentType(contentType)) {
            // 出题类型走 QUESTION_GEN 配额
            quotaKey = "ai.question.daily_quota";
            capability = "QUESTION_GEN";
        } else {
            // 内容生成类型（教学设计/知识清单/实训方案等）
            capability = "CONTENT_GEN";
        }
        int quota = systemService.getIntConfig(quotaKey, 25);
        if (quota <= 0) return;
        synchronized (QUOTA_LOCK) {
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            long count = aiCallLogMapper.selectCount(
                    new LambdaQueryWrapper<AiCallLog>()
                            .eq(AiCallLog::getUserId, teacherId)
                            .eq(AiCallLog::getCapability, capability)
                            .ge(AiCallLog::getCreatedAt, todayStart));
            if (count >= quota)
                throw new BusinessException(429, "今日AI生成已达上限(" + quota + "次)");
        }
    }

    /** 将 users.id 转换为 teachers.id（tasks.teacher_id 期望的格式） */
    private Long resolveTeacherId(Long userId) {
        if (userId == null) return null;
        com.school.teaching.entity.Teacher t = teacherMapper.selectOne(
                new LambdaQueryWrapper<com.school.teaching.entity.Teacher>()
                        .eq(com.school.teaching.entity.Teacher::getUserId, userId));
        if (t != null) return t.getId();
        return SecurityUtils.isAdmin() ? 0L : null;
    }

    /**
     * 构建知识老化提示（Phase 6）
     * 根据 knowledge_nodes.status 和 relevance_level 生成时效性指导
     */
    private String buildAgingHint(Long nodeId) {
        // 文化课知识点不会老化，直接跳过
        if (nodeId != null) {
            String subject = resolveNodeSubject(nodeId);
            if (subject != null && (subject.contains("语文") || subject.contains("数学") || subject.contains("英语"))) return "";
        }

        if (nodeId == null) return "";
        com.school.teaching.entity.KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) return "";

        String status = node.getStatus();
        Integer relevance = node.getRelevanceLevel();

        // 正常知识点，无需干预
        if (("ACTIVE".equals(status) || status == null)
            && (relevance == null || relevance >= 7)) {
            return "";
        }

        StringBuilder hint = new StringBuilder("【知识时效性提示】");

        if ("LEGACY".equals(status) || "DEPRECATED".equals(status)) {
            hint.append("「").append(node.getName())
                .append("」属于技术发展中已更新的知识点。");
            hint.append("出题时请侧重概念原理和演进脉络，");
            hint.append("避免考查已被淘汰的具体操作或技术细节。");
            if (node.getDeprecationNote() != null && !node.getDeprecationNote().isEmpty()) {
                hint.append("（补充说明：").append(node.getDeprecationNote()).append("）");
            }
        } else if ("OBSOLETE".equals(status)) {
            hint.append("「").append(node.getName())
                .append("」已完全淘汰，仅在考纲明确要求时作为历史背景简要提及。");
        }

        if (relevance != null && relevance <= 3) {
            hint.append("该知识点在考试中的权重较低(").append(relevance)
                .append("/10)，请减少该知识点的出题数量和分值占比。");
        }

        return hint.toString();
    }

    private String resolveNodeSubject(Long nodeId) {
        if (nodeId == null) return "";
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) return "";
        Long current = node.getParentId();
        int maxDepth = 10;
        while (current != null && maxDepth-- > 0) {
            KnowledgeNode parent = nodeMapper.selectById(current);
            if (parent == null) break;
            if (parent.getLevel() != null && parent.getLevel() == 1) return parent.getName();
            current = parent.getParentId();
        }
        return "";
    }

    /** 版本管理：更新旧版本 is_latest + 插入新版本 + 清理历史 — 需要事务保证原子性 */
    @Transactional
    protected AiOutput saveOutputWithVersion(AiOutput newOutput, Long nodeId, String contentType) {
        // 旧版本 is_latest 置 0
        outputMapper.update(null, new LambdaUpdateWrapper<AiOutput>()
                .eq(AiOutput::getNodeId, nodeId)
                .eq(AiOutput::getOutputType, contentType)
                .set(AiOutput::getIsLatest, 0));
        // 插入新版本
        outputMapper.insert(newOutput);
        // 清理超过5条的历史版本
        List<AiOutput> keepLatest = outputMapper.selectList(
                new LambdaQueryWrapper<AiOutput>()
                        .eq(AiOutput::getNodeId, nodeId)
                        .eq(AiOutput::getOutputType, contentType)
                        .orderByDesc(AiOutput::getVersionSeq)
                        .last("LIMIT 5"));
        if (!keepLatest.isEmpty()) {
            List<Long> keepIds = keepLatest.stream().map(AiOutput::getId).toList();
            outputMapper.delete(new LambdaQueryWrapper<AiOutput>()
                    .eq(AiOutput::getNodeId, nodeId)
                    .eq(AiOutput::getOutputType, contentType)
                    .le(AiOutput::getVersionSeq, newOutput.getVersionSeq())
                    .notIn(AiOutput::getId, keepIds));
        }
        return newOutput;
    }

    @Override
    public java.util.Map<String, Object> getDiagnosisResult(Long taskId) {
        AiOutput latest = outputMapper.selectOne(
            new LambdaQueryWrapper<AiOutput>()
                .eq(AiOutput::getOutputType, "DIAGNOSIS")
                .eq(AiOutput::getNodeId, taskId)
                .orderByDesc(AiOutput::getCreatedAt)
                .last("LIMIT 1"));
        if (latest == null) {
            return null;
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("diagnosisId", latest.getId());
        result.put("content", latest.getContent());
        result.put("createdAt", latest.getCreatedAt());
        return result;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> listQuestionBatches(
            Long teacherId, String outputType, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<com.school.teaching.entity.QuestionBank> w =
            new LambdaQueryWrapper<com.school.teaching.entity.QuestionBank>()
                .eq(com.school.teaching.entity.QuestionBank::getCreatedBy, teacherId);
        if (keyword != null && !keyword.isBlank()) {
            w.like(com.school.teaching.entity.QuestionBank::getQuestionText, keyword.trim());
        }
        w.orderByDesc(com.school.teaching.entity.QuestionBank::getCreateTime);
        Page<com.school.teaching.entity.QuestionBank> result = questionBankMapper.selectPage(
            new Page<>(page > 0 ? page : 1, pageSize > 0 ? pageSize : 20), w);

        // 按 batchId 分组聚合
        Map<String, java.util.List<com.school.teaching.entity.QuestionBank>> byBatch = new LinkedHashMap<>();
        for (com.school.teaching.entity.QuestionBank q : result.getRecords()) {
            String bid = extractBatchId(q);
            byBatch.computeIfAbsent(bid, k -> new java.util.ArrayList<>()).add(q);
        }

        java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
        for (Map.Entry<String, java.util.List<com.school.teaching.entity.QuestionBank>> e : byBatch.entrySet()) {
            java.util.List<com.school.teaching.entity.QuestionBank> batch = e.getValue();
            com.school.teaching.entity.QuestionBank first = batch.get(0);
            java.util.Map<String, Object> item = new LinkedHashMap<>();
            item.put("batchId", e.getKey());
            item.put("contentType", outputType);
            item.put("questionCount", batch.size());
            item.put("subject", first.getSubject());
            item.put("status", first.getStatus());
            item.put("createdAt", first.getCreateTime());
            String datePart = first.getCreateTime() != null
                ? first.getCreateTime().toLocalDate().toString().substring(5) : "";
            item.put("title", (first.getSubject() != null ? first.getSubject() + " " : "") + datePart + " · " + batch.size() + "题");
            item.put("questions", batch.stream().map(q -> {
                java.util.Map<String, Object> qm = new LinkedHashMap<>();
                qm.put("id", q.getId());
                qm.put("questionText", q.getQuestionText());
                qm.put("questionType", q.getQuestionType());
                qm.put("options", parseOptionsSafe(q.getOptions()));
                qm.put("correctAnswer", q.getCorrectAnswer());
                qm.put("explanation", q.getExplanation());
                qm.put("difficultyLevel", q.getDifficultyLevel());
                return qm;
            }).collect(java.util.stream.Collectors.toList()));
            items.add(item);
        }
        return items;
    }

    /**
     * 从 extraParams 中解析预期题目总数
     */
    private int resolveExpectedCount(Map<String, Object> params) {
        int total = 0;
        Object tc = params.get("typeCounts");
        if (tc instanceof Map<?, ?> m) {
            for (Object v : m.values()) {
                if (v instanceof Number n) total += n.intValue();
            }
        } else if (params.get("candidateCount") instanceof Number n) {
            total = n.intValue();
        }
        return total;
    }


    // ════════════════════ 知识点感知分批（v2） ════════════════════

    /**
     * 收集节点下所有 L3/L4 知识点，含内容深度（content 字符数）
     */
    private void collectKpsWithDepth(Long nodeId, List<Map<String, Object>> result, Set<Long> visited) {
        if (nodeId == null || !visited.add(nodeId)) return;
        List<KnowledgeNode> children = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>().eq(KnowledgeNode::getParentId, nodeId));
        if (children == null || children.isEmpty()) return;
        for (KnowledgeNode child : children) {
            if (child.getLevel() != null && child.getLevel() >= 3) {
                String content = child.getContent();
                int len = (content != null && !content.isBlank()) ? content.length() : 0;
                result.add(Map.of("id", child.getId(),
                    "name", child.getName() != null ? child.getName() : "",
                    "contentLen", len));
            }
            if (child.getLevel() != null && child.getLevel() <= 3) {
                collectKpsWithDepth(child.getId(), result, visited);
            }
        }
    }

    /**
     * 按内容深度将题目预算分配到各知识点（松耦合，不强求恰好命中总数）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> distributeQuestionsAcrossKps(
            List<Map<String, Object>> viableKps, Map<String, Integer> typeCounts,
            Map<Long, Double> kpWeights) {
        int rawTotal = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
        int kpCount = viableKps.size();
        // V055 fix: cap+1 给余量重分配留空间，避免 55→54 的截断
        int perKpCap = Math.max(3, Math.min(8, rawTotal / Math.max(kpCount, 1) + 1));

        // 计算加权长度（内容长度 × 权重因子），薄知识点降权但不归零
        double totalWeightedLen = 0;
        double[] weightedLens = new double[kpCount];
        for (int i = 0; i < kpCount; i++) {
            Map<String, Object> kp = viableKps.get(i);
            int len = kp.get("contentLen") instanceof Number n ? n.intValue() : 0;
            Long kpId = kp.get("id") instanceof Number n2 ? n2.longValue() : null;
            double weight = (kpWeights != null && kpId != null) ? kpWeights.getOrDefault(kpId, 1.0) : 1.0;
            weightedLens[i] = weight * Math.max(1, len);
            totalWeightedLen += weightedLens[i];
        }
        if (totalWeightedLen == 0) totalWeightedLen = 1;

        // 第一轮：底分 1 题 + 按加权比例分配剩余
        int[] alloc = new int[kpCount];
        int assigned = 0;
        for (int i = 0; i < kpCount; i++) { alloc[i] = 1; assigned++; }
        int remaining = rawTotal - assigned;
        for (int i = 0; i < kpCount && remaining > 0; i++) {
            int extra = (int) Math.round(weightedLens[i] / totalWeightedLen * remaining);
            alloc[i] = Math.min(alloc[i] + extra, perKpCap);
        }
        // 被上限截断的余量重分给未满 KP
        int actualSum = 0;
        for (int a : alloc) actualSum += a;
        if (actualSum < rawTotal) {
            int gap = rawTotal - actualSum;
            for (int i = 0; i < kpCount && gap > 0; i++) {
                if (alloc[i] < perKpCap) {
                    int add = Math.min(gap, perKpCap - alloc[i]);
                    alloc[i] += add; gap -= add;
                }
            }
        }

        // 按题型权重拆 typeCounts
        List<String> typeKeys = new ArrayList<>(typeCounts.keySet());
        int[] typeTotals = new int[typeKeys.size()];
        int typeSum = 0;
        for (int i = 0; i < typeKeys.size(); i++) { typeTotals[i] = typeCounts.get(typeKeys.get(i)); typeSum += typeTotals[i]; }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < kpCount; i++) {
            if (alloc[i] <= 0) continue;
            Map<String, Integer> subTypeCounts = new LinkedHashMap<>();
            int kpAssigned = 0;
            for (int j = 0; j < typeKeys.size(); j++) {
                int sub = Math.max(0, (int) Math.round((double) typeTotals[j] / typeSum * alloc[i]));
                subTypeCounts.put(typeKeys.get(j), sub);
                kpAssigned += sub;
            }
            if (kpAssigned < alloc[i]) {
                for (int j = 0; j < typeKeys.size() && kpAssigned < alloc[i]; j++) {
                    subTypeCounts.put(typeKeys.get(j), subTypeCounts.get(typeKeys.get(j)) + 1);
                    kpAssigned++;
                }
            } else if (kpAssigned > alloc[i]) {
                for (int j = typeKeys.size() - 1; j >= 0 && kpAssigned > alloc[i]; j--) {
                    int cur = subTypeCounts.get(typeKeys.get(j));
                    if (cur > 0) { subTypeCounts.put(typeKeys.get(j), cur - 1); kpAssigned--; }
                }
            }
            Map<String, Object> kpAlloc = new LinkedHashMap<>();
            kpAlloc.put("kpId", viableKps.get(i).get("id"));
            kpAlloc.put("kpName", viableKps.get(i).get("name"));
            kpAlloc.put("count", kpAssigned);
            kpAlloc.put("typeCounts", subTypeCounts);
            result.add(kpAlloc);
        }

        int finalSum = result.stream().mapToInt(m -> ((Map<String, Integer>) m.get("typeCounts"))
            .values().stream().mapToInt(Integer::intValue).sum()).sum();
        // V055: 精确修正 — 舍入误差导致的缺口补到第一个有容量的KP
        int gap = rawTotal - finalSum;
        if (gap > 0) {
            for (Map<String, Object> kpAlloc : result) {
                if (gap == 0) break;
                @SuppressWarnings("unchecked")
                Map<String, Integer> tc = (Map<String, Integer>) kpAlloc.get("typeCounts");
                // 优先补到单选题
                for (String tk : List.of("SINGLE_CHOICE", "FILL_IN", "TRUE_FALSE", "MULTI_CHOICE")) {
                    if (gap > 0 && tc.getOrDefault(tk, 0) < perKpCap) {
                        tc.put(tk, tc.getOrDefault(tk, 0) + 1);
                        kpAlloc.put("count", ((Number) kpAlloc.get("count")).intValue() + 1);
                        gap--;
                    }
                }
            }
        } else if (gap < 0) {
            // 多了就从最后一个题型减
            for (int i = result.size() - 1; i >= 0 && gap < 0; i--) {
                Map<String, Integer> tc = (Map<String, Integer>) result.get(i).get("typeCounts");
                for (String tk : List.of("MULTI_CHOICE", "TRUE_FALSE", "FILL_IN", "SINGLE_CHOICE")) {
                    if (gap < 0 && tc.getOrDefault(tk, 0) > 1) {
                        tc.put(tk, tc.get(tk) - 1);
                        result.get(i).put("count", ((Number) result.get(i).get("count")).intValue() - 1);
                        gap++;
                    }
                }
            }
        }
        finalSum = result.stream().mapToInt(m -> ((Map<String, Integer>) m.get("typeCounts"))
            .values().stream().mapToInt(Integer::intValue).sum()).sum();
        log.info("知识点分布: {}个KP, 原始{}题, cap={}, 分配{}题", kpCount, rawTotal, perKpCap, finalSum);
        return result;
    }

    /** 为一组 KP 构建 RAG 上下文 */
    private String buildKpGroupRagContext(List<Long> kpIds) {
        if (kpIds == null || kpIds.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        // 批量查询，避免 N+1
        List<KnowledgeNode> nodes = nodeMapper.selectBatchIds(kpIds);
        Map<Long, KnowledgeNode> nodeMap = nodes.stream()
            .collect(java.util.stream.Collectors.toMap(KnowledgeNode::getId, n -> n));
        for (Long kpId : kpIds) {
            KnowledgeNode node = nodeMap.get(kpId);
            if (node == null) continue;
            sb.append("【").append(node.getName()).append("】\n");
            if (node.getContent() != null && !node.getContent().isBlank()) {
                String c = node.getContent();
                if (c.length() > 600) c = c.substring(0, 600) + "...";
                sb.append(c).append("\n\n");
            }
        }
        return sb.toString();
    }

    /** KP 感知分批生成 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> generateKpAware(Long teacherId, Map<String, Object> params,
            List<Map<String, Object>> viableKps, int rawTotal, Long anchorNodeId) {
        Map<String, Integer> typeCounts = (Map<String, Integer>) params.get("typeCounts");
        if (typeCounts == null || typeCounts.isEmpty()) {
            return generateBatched(teacherId, params, rawTotal);
        }
        @SuppressWarnings("unchecked")
        Map<Long, Double> kpWeights = (Map<Long, Double>) params.get("_kpWeights");
        List<Map<String, Object>> kpAllocs = distributeQuestionsAcrossKps(viableKps, typeCounts, kpWeights);
        if (kpAllocs.isEmpty()) return generateBatched(teacherId, params, rawTotal);

        // 每 15 题打包成一组（V055: 从20降到15，避免最后一组题太少AI出不足）
        final int GROUP_SIZE = 15;
        List<List<Map<String, Object>>> groups = new ArrayList<>();
        List<Map<String, Object>> current = new ArrayList<>();
        int currentSum = 0;
        for (Map<String, Object> alloc : kpAllocs) {
            int cnt = alloc.get("count") instanceof Number n ? n.intValue() : 0;
            if (currentSum + cnt > GROUP_SIZE && !current.isEmpty()) {
                groups.add(current); current = new ArrayList<>(); currentSum = 0;
            }
            current.add(alloc); currentSum += cnt;
        }
        if (!current.isEmpty()) groups.add(current);

        int totalAllocated = kpAllocs.stream().mapToInt(m -> ((Number) m.get("count")).intValue()).sum();
        log.info("KP感知分批: {}KP, {}题 → {}组", kpAllocs.size(), totalAllocated, groups.size());

        // 并行生成所有组（复用共享线程池）
        List<java.util.concurrent.Callable<List<Map<String, Object>>>> tasks = new ArrayList<>();
        for (int g = 0; g < groups.size(); g++) {
            List<Map<String, Object>> group = groups.get(g);
            List<Long> groupKpIds = group.stream().map(m -> ((Number) m.get("kpId")).longValue()).toList();

            Map<String, Integer> groupTypeCounts = new LinkedHashMap<>();
            for (Map<String, Object> alloc : group) {
                Map<String, Integer> sub = (Map<String, Integer>) alloc.get("typeCounts");
                if (sub != null) sub.forEach((k, v) -> groupTypeCounts.merge(k, v, Integer::sum));
            }
            int groupExpected = groupTypeCounts.values().stream().mapToInt(Integer::intValue).sum();

            String groupRag = buildKpGroupRagContext(groupKpIds);
            List<Map<String, Object>> groupKpList = new ArrayList<>();
            for (Map<String, Object> m : group) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", m.get("kpId"));
                item.put("name", m.get("kpName"));
                groupKpList.add(item);
            }

            Map<String, Object> batchParams = new LinkedHashMap<>(params);
            batchParams.put("typeCounts", groupTypeCounts);
            batchParams.put("_batchMode", true);
            batchParams.put("_subKpList", om.valueToTree(groupKpList).toString());
            if (groupRag != null && !groupRag.isBlank()) {
                batchParams.put("ragContext", groupRag);
            }
            if (params.containsKey("_instructionPrompt")) {
                String correctedPrompt = params.get("_instructionPrompt").toString()
                    + "（第" + (g + 1) + "组/" + groups.size() + "组，本组仅需生成" + groupExpected + "题，请仅从以下知识点中出题）";
                correctedPrompt = correctedPrompt.replaceAll(
                    "题型分布：[^ ]+", "题型分布：" + formatTypeCountsForPrompt(groupTypeCounts));
                batchParams.put("_instructionPrompt", correctedPrompt);
            }

            int groupIdx = g;
            log.info("组 {}/{}: {}KP, {}题, rag={}字", g + 1, groups.size(),
                groupKpIds.size(), groupExpected, groupRag.length());
            tasks.add(() -> {
                try {
                    List<Map<String, Object>> batchResult = aiQuestionService.generateFromAiAssistant(teacherId, batchParams);
                    if (batchResult != null && !batchResult.isEmpty()) {
                        log.info("组 {}/{} 完成: 实得{}题", groupIdx + 1, groups.size(), batchResult.size());
                        return batchResult;
                    }
                    log.warn("组 {}/{} 返回空", groupIdx + 1, groups.size());
                    return Collections.emptyList();
                } catch (Exception e) {
                    log.error("组 {}/{} 失败: {}", groupIdx + 1, groups.size(), e.getMessage());
                    return Collections.emptyList();
                }
            });
        }

        List<Map<String, Object>> merged = new ArrayList<>();
        try {
            List<java.util.concurrent.Future<List<Map<String, Object>>>> futures = AI_PARALLEL_EXECUTOR.invokeAll(tasks);
            for (java.util.concurrent.Future<List<Map<String, Object>>> f : futures) {
                merged.addAll(f.get());
            }
        } catch (Exception e) {
            log.error("并行分批生成失败: {}", e.getMessage());
        }

        log.info("KP感知分批完成: 请求{}题, 实得{}题 (并行生成)", rawTotal, merged.size());
        return merged;
    }

    /** 简单分批生成（无 KP 感知，纯按题型均匀分配） */
    private List<Map<String, Object>> generateBatched(Long teacherId, Map<String, Object> params, int totalExpected) {
        final int BATCH_SIZE = 20;
        int numBatches = (totalExpected + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("组卷分批生成: 共{}题, {}批", totalExpected, numBatches);

        @SuppressWarnings("unchecked")
        Map<String, Integer> typeCounts = (Map<String, Integer>) params.get("typeCounts");
        List<Map<String, Integer>> batchTypeCounts = new ArrayList<>();
        if (typeCounts != null) {
            int[] batchTotals = new int[numBatches];
            int base = totalExpected / numBatches;
            int remainder = totalExpected % numBatches;
            for (int i = 0; i < numBatches; i++) batchTotals[i] = base + (i < remainder ? 1 : 0);
            Map<String, int[]> alloc = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
                int[] perBatch = new int[numBatches];
                int val = e.getValue(), b = val / numBatches, r = val % numBatches, assigned = 0;
                for (int i = 0; i < numBatches; i++) { perBatch[i] = b + (i < r ? 1 : 0); assigned += perBatch[i]; }
                if (assigned != val && numBatches > 0) perBatch[numBatches - 1] += val - assigned;
                alloc.put(e.getKey(), perBatch);
            }
            for (int i = 0; i < numBatches; i++) {
                Map<String, Integer> batch = new LinkedHashMap<>();
                for (Map.Entry<String, int[]> ae : alloc.entrySet())
                    if (ae.getValue()[i] > 0) batch.put(ae.getKey(), ae.getValue()[i]);
                if (!batch.isEmpty()) batchTypeCounts.add(batch);
            }
        } else {
            for (int i = 0; i < numBatches; i++)
                batchTypeCounts.add(Map.of("_count", Math.min(BATCH_SIZE, totalExpected - i * BATCH_SIZE)));
        }

        List<Map<String, Object>> merged = new ArrayList<>();
        if (batchTypeCounts.size() <= 1) {
            // 单批无需并行
            for (int i = 0; i < batchTypeCounts.size(); i++) {
                int batchExpected = batchTypeCounts.get(i).values().stream().mapToInt(Integer::intValue).sum();
                Map<String, Object> batchParams = new LinkedHashMap<>(params);
                batchParams.put("typeCounts", batchTypeCounts.get(i));
                batchParams.put("_batchMode", true);
                if (params.containsKey("_instructionPrompt"))
                    batchParams.put("_instructionPrompt", params.get("_instructionPrompt") + "（第" + (i + 1) + "批）");
                log.info("批次 {}/{}: 预期{}题", i + 1, numBatches, batchExpected);
                try {
                    List<Map<String, Object>> batchResult = aiQuestionService.generateFromAiAssistant(teacherId, batchParams);
                    if (batchResult != null && !batchResult.isEmpty()) {
                        merged.addAll(batchResult);
                        log.info("批次 {}/{} 完成: 实得{}题", i + 1, numBatches, batchResult.size());
                    } else log.warn("批次 {}/{} 返回空", i + 1, numBatches);
                } catch (Exception e) { log.error("批次 {}/{} 失败: {}", i + 1, numBatches, e.getMessage()); }
            }
        } else {
            // 多批并行生成（复用共享线程池）
            List<java.util.concurrent.Callable<List<Map<String, Object>>>> tasks = new ArrayList<>();
            for (int i = 0; i < batchTypeCounts.size(); i++) {
                int batchIdx = i;
                int batchExpected = batchTypeCounts.get(i).values().stream().mapToInt(Integer::intValue).sum();
                Map<String, Object> batchParams = new LinkedHashMap<>(params);
                batchParams.put("typeCounts", batchTypeCounts.get(i));
                batchParams.put("_batchMode", true);
                if (params.containsKey("_instructionPrompt"))
                    batchParams.put("_instructionPrompt", params.get("_instructionPrompt") + "（第" + (i + 1) + "批）");
                log.info("批次 {}/{}: 预期{}题", i + 1, numBatches, batchExpected);
                tasks.add(() -> {
                    try {
                        List<Map<String, Object>> batchResult = aiQuestionService.generateFromAiAssistant(teacherId, batchParams);
                        if (batchResult != null && !batchResult.isEmpty()) {
                            log.info("批次 {}/{} 完成: 实得{}题", batchIdx + 1, numBatches, batchResult.size());
                            return batchResult;
                        }
                        log.warn("批次 {}/{} 返回空", batchIdx + 1, numBatches);
                        return Collections.emptyList();
                    } catch (Exception e) {
                        log.error("批次 {}/{} 失败: {}", batchIdx + 1, numBatches, e.getMessage());
                        return Collections.emptyList();
                    }
                });
            }
            try {
                List<java.util.concurrent.Future<List<Map<String, Object>>>> futures = AI_PARALLEL_EXECUTOR.invokeAll(tasks);
                for (java.util.concurrent.Future<List<Map<String, Object>>> f : futures) {
                    merged.addAll(f.get());
                }
            } catch (Exception e) {
                log.error("并行分批生成失败: {}", e.getMessage());
            }
        }
        log.info("组卷分批完成: 请求{}题, 实得{}题", totalExpected, merged.size());
        return merged;
    }

    /** 构建题型数量描述文本（用于 prompt 中替换） */
    private static String formatTypeCountsForPrompt(Map<String, Integer> typeCounts) {
        StringBuilder sb = new StringBuilder();
        typeCounts.forEach((type, cnt) -> {
            if (cnt > 0) {
                if (!sb.isEmpty()) sb.append("、");
                sb.append(typeLabel(type)).append("×").append(cnt);
            }
        });
        if (sb.isEmpty()) sb.append("单选题×2");
        return sb.toString();
    }

    /** 题型英文→中文标签 */
    private static String typeLabel(String type) {
        return switch (type) {
            case "SINGLE_CHOICE" -> "单选题";
            case "MULTI_CHOICE" -> "多选题";
            case "TRUE_FALSE" -> "判断题";
            case "FILL_IN" -> "填空题";
            case "ESSAY", "SHORT_ANSWER" -> "简答题";
            case "CLOZE" -> "完形填空";
            case "CALCULATION" -> "计算题";
            case "READING_COMPREHENSION", "READING" -> "阅读理解";
            default -> type;
        };
    }

    /** 判断是否为出题类内容类型（需要注入参考样题） */
    private static boolean isQuestionContentType(String contentType) {
        if (contentType == null) return false;
        return switch (contentType) {
            case "EXAM_PAPER", "COMPREHENSIVE_EXERCISES", "CLASSROOM_QUESTIONS",
                 "KNOWLEDGE_PRACTICE", "REMEDIAL_EXERCISE" -> true;
            default -> false;
        };
    }

}
