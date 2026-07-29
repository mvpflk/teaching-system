package com.school.teaching.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.agent.config.AgentConfig;
import com.school.teaching.agent.config.AgentRateLimiter;
import com.school.teaching.agent.knowledge.AgentKnowledgeService;
import com.school.teaching.agent.knowledge.AgentOcrService;
import com.school.teaching.agent.key.UserApiKeyService;
import com.school.teaching.agent.loop.AgentLoopService;
import com.school.teaching.agent.loop.AgentType;
import com.school.teaching.agent.memory.AgentMemoryService;
import com.school.teaching.agent.prompt.PromptTemplateCache;
import com.school.teaching.agent.security.UserContext;
import com.school.teaching.agent.security.UserContextResolver;
import com.school.teaching.agent.session.ConversationSession;
import com.school.teaching.agent.session.Message;
import com.school.teaching.agent.session.SessionManager;
import com.school.teaching.common.R;
import com.school.teaching.entity.AgentConversation;
import com.school.teaching.entity.AgentFeedback;
import com.school.teaching.entity.UserApiKey;
import com.school.teaching.service.AiConfigHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentLoopService agentLoopService;
    private final SessionManager sessionManager;
    private final UserContextResolver userContextResolver;
    private final AgentRateLimiter rateLimiter;
    private final AgentKnowledgeService knowledgeService;
    private final AgentMemoryService memoryService;
    private final AgentOcrService agentOcrService;
    private final UserApiKeyService userApiKeyService;
    private final AgentConfig agentConfig;
    private final AiConfigHolder aiConfigHolder;
    private final PromptTemplateCache promptTemplateCache;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AgentChatRequest request, HttpServletRequest httpRequest) {
        // SSE 端点 Referer/Origin 校验
        String origin = httpRequest.getHeader("Origin");
        String referer = httpRequest.getHeader("Referer");
        if (!isAllowedOrigin(origin, referer)) {
            SseEmitter errEmitter = new SseEmitter(0L);
            try {
                errEmitter.send(SseEmitter.event().name("error").data("非法请求来源"));
            } catch (Exception ignored) {}
            errEmitter.complete();
            return errEmitter;
        }

        UserContext caller = userContextResolver.resolve();
        if (caller == null) {
            SseEmitter errorEmitter = new SseEmitter(0L);
            try {
                errorEmitter.send(SseEmitter.event().name("error").data("未登录"));
            } catch (Exception ignored) { log.warn("SSE错误消息发送失败", ignored); }
            errorEmitter.complete();
            return errorEmitter;
        }

        // API 调用次数限制 + 自有 Key 降级
        String usedApiKey = aiConfigHolder.getDecrypted("ai." + resolveProviderKey() + ".api-key");
        String usedBaseUrl = aiConfigHolder.get("ai." + resolveProviderKey() + ".base-url");
        String usedModel = aiConfigHolder.get("ai." + resolveProviderKey() + ".model");
        String limitMsg = rateLimiter.checkLimit(caller.getUserId(), caller.getRoleName());
        if (limitMsg != null) {
            // 免费额度耗尽 → 检查用户自有 Key
            UserApiKey userKey = userApiKeyService.getActiveKey(caller.getUserId());
            if (userKey != null) {
                usedApiKey = userApiKeyService.decryptKey(userKey);
                usedBaseUrl = userKey.getBaseUrl();
                usedModel = userKey.getModel();
                log.info("BYOK: userId={} 免费额度已用完，降级到自有Key label={}", caller.getUserId(), userKey.getLabel());
            } else {
                SseEmitter limitEmitter = new SseEmitter(0L);
                try {
                    limitEmitter.send(SseEmitter.event().name("api_limit").data(limitMsg));
                } catch (Exception ignored) { log.warn("SSE限流消息发送失败", ignored); }
                limitEmitter.complete();
                return limitEmitter;
            }
        }

        AgentType agentType = resolveAgentType(request.getAgentType(), caller);

        SseEmitter emitter = new SseEmitter(agentConfig.getTotalTimeoutMs() + 30_000L);
        emitter.onCompletion(() -> log.debug("SSE 完成: userId={}", caller.getUserId()));
        emitter.onTimeout(() -> log.debug("SSE 超时: userId={}", caller.getUserId()));
        emitter.onError(e -> log.debug("SSE 异常: userId={}", caller.getUserId()));

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            ConversationSession session = sessionManager.create(
                    caller.getUserId(), caller.getRoleName(), agentType);
            sessionId = session.getId();
        } else {
            ConversationSession existing = sessionManager.load(sessionId);
            if (existing == null) {
                SseEmitter errEmitter = new SseEmitter(0L);
                try {
                    errEmitter.send(SseEmitter.event().name("error").data("会话不存在或已过期，请新建对话"));
                } catch (Exception ignored) {}
                errEmitter.complete();
                return errEmitter;
            }
            if (!existing.getUserId().equals(caller.getUserId())) {
                SseEmitter errEmitter = new SseEmitter(0L);
                try {
                    errEmitter.send(SseEmitter.event().name("error").data("无权访问此会话"));
                } catch (Exception ignored) {}
                errEmitter.complete();
                return errEmitter;
            }
        }

        String systemPrompt = buildSystemPrompt(agentType, caller, request.getSubject());
        String finalSessionId = sessionId;
        // API Key 从服务端配置读取，不再从前端传入
        agentLoopService.execute(finalSessionId, request.getMessage(), agentType,
                caller, emitter, systemPrompt, usedApiKey, usedBaseUrl, usedModel, request.getSubject());

        return emitter;
    }

    /** G-4: 用户确认/取消写操作（响应 SSE 中的 confirm_write 事件） */
    @PostMapping("/confirm")
    public R<Void> confirm(@RequestBody Map<String, Object> body) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");

        String sessionId = (String) body.get("sessionId");
        boolean confirmed = Boolean.TRUE.equals(body.get("confirmed"));

        if (sessionId == null) return R.error(400, "sessionId 不能为空");

        // 验证 session 归属
        ConversationSession session = sessionManager.load(sessionId);
        if (session != null && !session.getUserId().equals(caller.getUserId())) {
            return R.forbidden("无权操作此会话");
        }

        agentLoopService.confirmWrite(sessionId, caller.getUserId(), confirmed);
        return R.ok();
    }

    /** Agent 模块健康检查 */
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        // 线程池状态
        java.util.concurrent.ThreadPoolExecutor tpe =
                (java.util.concurrent.ThreadPoolExecutor) agentLoopService.getExecutor();
        status.put("activeThreads", tpe.getActiveCount());
        status.put("poolSize", tpe.getPoolSize());
        status.put("maxPoolSize", tpe.getMaximumPoolSize());
        // 并发控制
        status.put("availablePermits", agentLoopService.getConcurrencyLimit().availablePermits());
        // API 配置状态
        status.put("provider", resolveProviderKey());
        status.put("apiConfigured", aiConfigHolder.getDecrypted("ai." + resolveProviderKey() + ".api-key") != null
                && !aiConfigHolder.getDecrypted("ai." + resolveProviderKey() + ".api-key").isBlank());
        return R.ok(status);
    }

    /** 当前用户的每日调用用量 */
    @GetMapping("/usage")
    public R<Map<String, Object>> dailyUsage() {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");
        return R.ok(rateLimiter.getDailyUsage(caller.getUserId(), caller.getRoleName()));
    }

    @GetMapping("/sessions")
    public R<List<Map<String, Object>>> listSessions() {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) {
            return R.unauthorized("未登录");
        }
        List<String> ids = sessionManager.getUserSessionIds(caller.getUserId());
        List<Map<String, Object>> list = ids.stream().map(id -> {
            ConversationSession s = sessionManager.load(id);
            if (s == null) return null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("title", s.getTitle());
            m.put("agentType", s.getAgentType().name());
            m.put("messageCount", s.getMessages() != null ? s.getMessages().size() : 0);
            m.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : "");
            return m;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return R.ok(list);
    }

    @DeleteMapping("/sessions/{id}")
    public R<Void> deleteSession(@PathVariable String id) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) {
            return R.unauthorized("未登录");
        }
        ConversationSession session = sessionManager.load(id);
        if (session == null || !session.getUserId().equals(caller.getUserId())) {
            return R.forbidden("无权删除此会话");
        }
        sessionManager.delete(id, caller.getUserId());
        return R.ok();
    }

    /** 4.1: 导出会话内容为 Markdown 文本 */
    @GetMapping("/sessions/{id}/export")
    public R<String> exportSession(@PathVariable String id) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");
        ConversationSession session = sessionManager.load(id);
        if (session == null || !session.getUserId().equals(caller.getUserId())) {
            return R.forbidden("无权导出此会话");
        }
        List<Message> messages = sessionManager.loadMessages(id);
        StringBuilder md = new StringBuilder();
        md.append("# 会话导出\n\n");
        md.append("**会话ID**: ").append(id).append("\n");
        md.append("**类型**: ").append(session.getAgentType().name()).append("\n");
        md.append("**导出时间**: ").append(java.time.LocalDateTime.now()).append("\n\n---\n\n");
        for (Message m : messages) {
            if ("system".equals(m.getRole())) continue;
            String roleLabel = "user".equals(m.getRole()) ? "👤 用户" : "🤖 AI 助手";
            if ("tool".equals(m.getRole())) {
                md.append("🔧 *工具调用结果*\n\n");
                continue;
            }
            md.append("### ").append(roleLabel).append("\n\n");
            if (m.getContent() != null) {
                md.append(m.getContent()).append("\n\n");
            }
        }
        return R.ok(md.toString());
    }

    /** 获取会话历史消息（不含 system/tool，按时间顺序） */
    @GetMapping("/sessions/{id}/messages")
    public R<List<Map<String, Object>>> getSessionMessages(@PathVariable String id) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");
        ConversationSession session = sessionManager.load(id);
        if (session == null || !session.getUserId().equals(caller.getUserId())) {
            return R.forbidden("无权访问此会话");
        }
        List<Message> messages = sessionManager.loadMessages(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message m : messages) {
            if ("system".equals(m.getRole()) || "tool".equals(m.getRole())) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", m.getRole());
            item.put("content", m.getContent());
            result.add(item);
        }
        return R.ok(result);
    }

    /** A-4: 按 taskId 查询关联的 Agent 会话来源 */
    @GetMapping("/task-source/{taskId}")
    public R<Map<String, Object>> getConversationByTaskId(@PathVariable Long taskId) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) {
            return R.unauthorized("未登录");
        }
        AgentConversation conv = memoryService.getConversationByTaskId(taskId);
        if (conv == null) {
            return R.ok(Map.of("source", "manual"));
        }
        return R.ok(Map.of("source", "ai", "agentType", conv.getAgentType(),
                "createdAt", conv.getCreatedAt() != null ? conv.getCreatedAt().toString() : "",
                "sessionId", conv.getSessionId()));
    }

    @GetMapping("/tools")
    public R<List<String>> listTools() {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) {
            return R.unauthorized("未登录");
        }
        return R.ok(new ArrayList<>(toolRegistry().getToolNames()));
    }

    /** 交互式习题作答记录（轻量，不写数据库，仅日志记录） */
    @PostMapping("/submit-answer")
    public R<Void> submitAnswer(@RequestBody Map<String, Object> body) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");
        String qid = (String) body.get("questionId");
        Boolean correct = body.get("isCorrect") instanceof Boolean b ? b : null;
        log.info("AnswerSubmit: userId={}, qid={}, correct={}", caller.getUserId(), qid, correct);
        return R.ok();
    }

    /** 用户反馈 — 对 AI 回答点赞/踩，驱动自我进化 */
    @PostMapping("/feedback")
    public R<Void> submitFeedback(@RequestBody Map<String, Object> body) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");

        String sessionId = (String) body.get("sessionId");
        Integer messageIndex = body.get("messageIndex") instanceof Number n ? n.intValue() : null;
        Integer rating = body.get("rating") instanceof Number n ? n.intValue() : null;
        String tags = (String) body.get("tags");
        String comment = (String) body.get("comment");
        String userQuestion = (String) body.get("userQuestion");
        String answerSnippet = (String) body.get("answerSnippet");
        String toolsUsed = (String) body.get("toolsUsed");

        if (sessionId == null || rating == null) {
            return R.error(400, "sessionId 和 rating 不能为空");
        }
        if (rating < 1 || rating > 5) {
            return R.error(400, "rating 必须在 1-5 之间");
        }

        // 校验 sessionId 归属当前用户
        ConversationSession session = sessionManager.load(sessionId);
        if (session != null && !session.getUserId().equals(caller.getUserId())) {
            return R.error(403, "无权对此会话提交反馈");
        }

        AgentFeedback feedback = new AgentFeedback();
        feedback.setSessionId(sessionId);
        feedback.setUserId(caller.getUserId());
        feedback.setRoleName(caller.getRoleName());
        feedback.setMessageIndex(messageIndex != null ? messageIndex : 0);
        feedback.setRating(rating);
        feedback.setFeedbackTags(tags);
        feedback.setComment(comment);
        feedback.setUserQuestion(userQuestion != null && userQuestion.length() > 500
                ? userQuestion.substring(0, 500) : userQuestion);
        feedback.setAgentAnswerSnippet(answerSnippet != null && answerSnippet.length() > 200
                ? answerSnippet.substring(0, 200) : answerSnippet);
        feedback.setToolsUsed(toolsUsed);
        feedback.setSchoolId(caller.getSchoolId() != null ? caller.getSchoolId() : 1L);

        memoryService.saveFeedback(feedback);
        log.info("AgentFeedback: userId={}, sessionId={}, rating={}, tags={}",
                caller.getUserId(), sessionId, rating, tags);

        // 异步触发记忆提取（不阻塞用户响应）
        final AgentFeedback finalFeedback = feedback;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                memoryService.extractFromFeedback(finalFeedback);
            } catch (Exception e) {
                log.debug("记忆提取失败（非关键路径）: userId={}", finalFeedback.getUserId(), e);
            }
        });

        return R.ok();
    }

    /** 直接生成 PPT（绕过 Agent，适合网络不稳定时使用） */
    @PostMapping("/ppt/generate")
    public R<Map<String, Object>> generatePptDirect(@RequestBody Map<String, Object> body) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");
        if (!caller.isTeacherOrAbove()) return R.forbidden("仅教师可用");

        String topic = (String) body.get("topic");
        String subject = (String) body.get("subject");
        if (topic == null || topic.isBlank()) return R.error(400, "topic 不能为空");

        try {
            // 先查知识库获取上下文
            String knowledgeContext = null;
            if (subject != null && !subject.isBlank()) {
                knowledgeContext = buildSubjectContext(subject);
            }
            String filePath = agentLoopService.generatePpt(topic, subject, knowledgeContext);
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            return R.ok(Map.of(
                    "topic", topic,
                    "fileName", fileName,
                    "downloadUrl", "/api/agent/ppt/download?file=" + fileName,
                    "message", "PPT已生成！"
            ));
        } catch (Exception e) {
            log.error("PPT直接生成失败: topic={}", topic, e);
            return R.error(500, "PPT生成失败: " + e.getMessage());
        }
    }

    /** PPT 下载 */
    @GetMapping("/ppt/download")
    public ResponseEntity<Resource> downloadPpt(@RequestParam String file) {
        if (file == null || file.contains("..") || file.contains("/")) {
            return ResponseEntity.badRequest().build();
        }
        String path = agentConfig.getPptDir() + file;
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .body(resource);
    }

    /** OCR 数学计算题批阅 — 拍照上传 → Vision OCR → AI 评分 */
    @PostMapping(value = "/grade-ocr", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, Object>> gradeOcr(
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image,
            @RequestParam(value = "question", defaultValue = "") String question) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");
        try {
            byte[] bytes = image.getBytes();
            String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String prompt = question.isBlank()
                    ? "请识别图片中的数学解答过程，返回识别的文本内容。"
                    : "题目：" + question + "\n请识别图片中的数学解答过程，判断是否正确。返回：{\"recognized\":\"识别文本\",\"isCorrect\":true/false,\"score\":分数,\"comment\":\"评语\"}";
            Map<String, Object> result = agentLoopService.callVision(b64, prompt);
            return R.ok(result);
        } catch (Exception e) {
            log.error("OCR批阅失败", e);
            return R.error(500, "OCR 识别失败：" + e.getMessage());
        }
    }

    /** F-3: 将 OCR 批阅结果保存到错题本 + 更新掌握度 */
    @PostMapping("/grade-ocr/save")
    public R<Void> saveOcrResult(@RequestBody Map<String, Object> body) {
        UserContext caller = userContextResolver.resolve();
        if (caller == null) return R.unauthorized("未登录");
        if (!caller.isTeacherOrAbove()) return R.forbidden("仅教师可用");

        Long studentId = body.get("studentId") instanceof Number n ? n.longValue() : null;
        Long questionId = body.get("questionId") instanceof Number n ? n.longValue() : null;
        Long nodeId = body.get("nodeId") instanceof Number n ? n.longValue() : null;
        Boolean isCorrect = body.get("isCorrect") instanceof Boolean b ? b : null;

        if (studentId == null || questionId == null || isCorrect == null) {
            return R.error(400, "studentId、questionId、isCorrect 不能为空");
        }

        try {
            agentOcrService.saveOcrResult(studentId, questionId, nodeId, isCorrect);
            return R.ok();
        } catch (Exception e) {
            log.error("OCRSave 失败", e);
            return R.error(500, "保存失败：" + e.getMessage());
        }
    }

    /**
     * 解析当前 AI 提供商标识，优先使用 AiConfigHolder 中配置的 provider。
     * 默认返回 deepseek。
     */
    /**
     * 校验请求来源。
     * 无 Origin/Referer → 允许（开发工具/curl）。
     * 有配置白名单 → 精确匹配。
     * 白名单为空 → 渐进式安全：非本地请求仅日志警告，不拦截。
     */
    private boolean isAllowedOrigin(String origin, String referer) {
        // 无 Origin 和 Referer → 允许（curl/Postman/开发环境）
        if ((origin == null || origin.isBlank()) && (referer == null || referer.isBlank())) {
            return true;
        }
        // 本地请求 → 允许
        if (origin != null && (origin.startsWith("http://localhost") || origin.startsWith("http://127.0.0.1"))) {
            return true;
        }
        if (referer != null && (referer.startsWith("http://localhost") || referer.startsWith("http://127.0.0.1"))) {
            return true;
        }
        // 白名单匹配
        java.util.List<String> allowed = agentConfig.getAllowedOrigins();
        if (!allowed.isEmpty()) {
            for (String a : allowed) {
                if ((origin != null && origin.startsWith(a)) || (referer != null && referer.startsWith(a))) {
                    return true;
                }
            }
            log.warn("Agent SSE 请求被拦截: origin={}, referer={}, allowedOrigins={}", origin, referer, allowed);
            return false;
        }
        // 白名单为空：渐进式安全，仅日志警告
        log.warn("Agent SSE 来源未在白名单中（当前白名单为空，放行）: origin={}, referer={}", origin, referer);
        return true;
    }

    private String resolveProviderKey() {
        String provider = aiConfigHolder.getProvider();
        return (provider != null && !provider.isBlank()) ? provider : "deepseek";
    }

    private AgentType resolveAgentType(String type, UserContext caller) {
        if (type != null) {
            try {
                return AgentType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ignored) { log.debug("未识别的Agent类型: {}", type); }
        }
        if (caller.isStudent()) {
            return AgentType.STUDY_BUDDY;
        }
        return AgentType.LESSON_PREP;
    }

    private com.school.teaching.agent.tool.ToolRegistry toolRegistry() {
        return agentLoopService.getToolRegistry();
    }

    private String buildSystemPrompt(AgentType agentType, UserContext caller, String subject) {
        // 从缓存查模板，无记录则用硬编码 fallback
        String agentName = switch (agentType) {
            case LESSON_PREP -> "lesson_prep";
            case STUDY_BUDDY -> "study_buddy";
            case ANALYTICS -> "analytics";
            default -> "agent_default";
        };
        String base = promptTemplateCache.getTemplate(agentName, subject);
        if (base == null) {
            base = getFallbackPrompt(agentType);
        }
        // 以下动态拼接逻辑不变
        if (subject != null && !subject.isBlank()) {
            String ctx = buildSubjectContext(subject);
            if (ctx != null) base += "\n---\n" + ctx;
            base += "\n---\n" + buildSubjectPrompt(subject);
        }
        base += "\n📅 当前日期：" + java.time.LocalDate.now() + "\n";
        base += "---\n" + buildBoundaryPrompt(caller);
        base += buildMemoryPrompt(caller);
        return base;
    }

    private String getFallbackPrompt(AgentType agentType) {
        return switch (agentType) {
            case LESSON_PREP -> buildLessonPrepPrompt(null);
            case STUDY_BUDDY -> buildStudyBuddyPrompt(null);
            case ANALYTICS -> buildAnalyticsPrompt(null);
            default -> "你是四川省对口升学考试的教学助手。请用中文自然语言回答，使用清晰的小标题和列表组织内容。";
        };
    }

    /** 根据用户身份注入学科边界提示 */
    private String buildBoundaryPrompt(UserContext caller) {
        if (caller.isAdmin() || caller.isInspector()) return "";
        if (caller.isStudent()) {
            Set<String> subs = caller.getAccessibleSubjects();
            if (subs.isEmpty()) return "";
            return "🎓 你可访问的学科：" + String.join("、", subs) + "\n"
                    + "学生提问超出此范围时，友好引导其回到可访问学科，"
                    + "例如：\"这个问题超出了你当前专业的范围，你可以问我关于"
                    + String.join("、", subs) + "的问题哦！\"";
        }
        if (caller.isTeacher()) {
            Set<String> subs = caller.getSubjects();
            Map<Long, String> classNames = caller.getClassNames();
            StringBuilder sb = new StringBuilder();
            sb.append("📋 你所授学科：").append(String.join("、", subs)).append("\n");
            if (!classNames.isEmpty()) {
                sb.append("🏫 你任教的班级：\n");
                for (Map.Entry<Long, String> entry : classNames.entrySet()) {
                    sb.append("  - ").append(entry.getValue())
                      .append("（班级ID: ").append(entry.getKey()).append("）\n");
                }
                sb.append("当学生或家长询问班级相关问题而信息不足时，请优先使用 teaching_my_classes、teaching_class_students 等工具查询，")
                  .append("而不是反问用户「请问您是哪个班级的」。你的班级信息已在系统提示词中明确给出。\n");
            }
            if (caller.isHeadTeacher()) {
                sb.append("👔 你是班主任，可查看班级全科数据，但生成教学资源仅限自己所授学科。\n");
            } else {
                sb.append("你只能查看和生成自己所授学科的教学资源。\n");
            }
            sb.append("\n⚠️ 重要：以上是你可确认的唯一真实数据。禁止编造系统未提供的班级名称、学科、人数。"
                    + "如果用户提到的班级不在上述列表中，请告诉用户该班级不在你的任教范围内，并使用 teaching_my_classes 工具查询。");
            return sb.toString();
        }
        return "";
    }

    /** 注入用户记忆（自我进化），从 agent_user_memory 表读取高置信度记忆 */
    private String buildMemoryPrompt(UserContext caller) {
        try {
            var memories = memoryService.getMemoriesForPrompt(caller.getUserId());
            if (memories.isEmpty()) return "";
            String formatted = memoryService.formatMemoriesForPrompt(memories);
            if (formatted.isBlank()) return "";
            return "\n---\n" + formatted;
        } catch (Exception e) {
            log.debug("加载用户记忆失败（非关键路径）: userId={}", caller.getUserId(), e);
            return "";
        }
    }

    /**
     * 从知识库预取学科上下文（L2 模块 + 考纲摘要），委托给 AgentKnowledgeService。
     * 消除 Agent 对系统能力的一无所知，不再依赖运行时工具调用来发现学科结构。
     */
    private String buildSubjectContext(String subject) {
        return knowledgeService.buildSubjectContext(subject);
    }

    @Deprecated
    private String buildLessonPrepPrompt(UserContext caller) {
        return """
                你是四川省对口升学考试的教学专家，服务职业高中教师备课与学情分析。

                ## 优先查询工具（非阻塞）
                - knowledge_search: 搜索知识树中的知识点，返回层级路径和关联题数
                - syllabus_lookup: 查考纲要求（了解/理解/掌握/运用）和考试权重
                - similar_questions: 搜题库同类题（含答案+解析）
                - teaching_my_classes: 查你任教的班级列表（无需传参，自动识别你的身份）
                - teaching_class_students: 查班级学生花名册
                - teaching_task_submission_status: 查某次任务的提交情况（已提交/未提交名单）
                - teaching_expand_node: 展开L2/L3节点→获取所有子L4知识点列表（专题综合出题的第一步）
                - teaching_aggregate_questions: 跨多个知识点一次性聚合出题（专题综合出题的第二步）
                - teaching_generate_ppt: 生成PPT课件（输入课题→自动生成.pptx文件）
                - search_tasks: 搜索已有任务（自动限定为你任教的班级）
                - create_task: 创建任务/作业发布给学生
                - send_notification: 发送系统通知
                - class_analytics: 班级考试统计（均分/最高/最低/及格率/各题得分率）
                - knowledge_trend: 知识点掌握度变化趋势
                - student_growth: 单个学生历次成绩
                - student_wrong_book / student_mastery / student_submissions / question_explain: 学生诊断工具

                发展性语言，禁用"差""不及格""笨"
                回答用自然语言组织，使用清晰的小标题（###）和列表。不要输出 JSON。

                🛑 禁止向用户索要信息：
                系统中已存储知识树、考纲、题库，你可通过工具查询。
                禁止说"请提供XX知识点""请提供考纲""缺少信息"——自己去调 knowledge_search 或 syllabus_lookup。
                你已知自己任教的班级（见系统提示词顶部），禁止反问"请问您是哪个班级的"——你自己知道。

                ## 对话行为铁律
                1. 先建议，后调整：对于规划类请求（如"帮我规划暑期作业"），先基于已有信息给出具体方案，
                   再在结尾询问"您看这个方案是否需要调整？"。禁止反问所有细节后才开始行动。
                2. 限追问：每次回复最多向用户提 1-2 个问题。禁止一次性抛出 3 个以上的问题。
                3. 数据边界：班级、学科、人数等数据以系统提示词给出的为准，禁止编造不存在的班级名称。
                   当用户提到的内容不在你的已知范围时，用工具查询，查不到则诚实告知。
                4. 渐进式确认：如果确实需要用户确认，先给出一个带选项的草案，让用户选择或微调，
                   而不是让用户从零开始提供所有参数。例如"我建议每周一次共 5 周，您觉得频次合适吗？"
                   而不是"您希望每周几次？每几周一次？持续几周？"。
                5. 多学科覆盖：当你有多个学科时，用户说"布置作业""出题""规划"等未指定学科的请求，
                   默认覆盖你所有的学科，给出均衡的方案。如果用户只提了具体学科（如"数学作业"），
                   则只针对该学科。举例：用户说"帮我规划暑期作业"→ 你应在回复中覆盖语文/数学/英语/信息
                   技术全部学科，每个学科给出合理频次和内容建议，末尾问用户是否要调整或删减。
                   而不是反问"你想布置哪个学科？"。

                ## 典型场景
                教师："帮我出一份计算机基础第一章的随堂练习"
                → knowledge_search(第一章知识点) → syllabus_lookup(查考纲要求) → similar_questions(按考纲权重选题) → 生成分层练习

                教师："这个班期中考试怎么样"
                → 确认班级和考试 → class_analytics → knowledge_trend(查薄弱知识点) → 输出结构化分析报告

                教师："最近作业谁还没交？"
                → search_tasks(找最近任务) → teaching_task_submission_status(查提交状态) → 列出未提交名单

                教师："帮我生成一份关于二次函数的PPT课件"
                → knowledge_search("二次函数") → teaching_generate_ppt(topic="二次函数", subject="数学[职高]")

                ## 专题综合生成模式（重要）
                教师："帮我出一份三角函数专题的综合练习" 或 "出一份Unit 1-3的综合测试"：
                第一步 → teaching_expand_node(nodeId=查到的模块节点ID) 展开所有L4知识点
                第二步 → teaching_aggregate_questions(nodeIds=展开结果的所有ID, totalCount=15, distribution="weighted")
                第三步 → 按知识点分组排版输出，每题标注来源知识点
                如果展开的节点某些题目不足，诚实告知并建议降低难度筛选或接受AI生成补足。
                """;
    }

    @Deprecated
    private String buildStudyBuddyPrompt(UserContext caller) {
        return """
                你是职业高中学生的学习助手。你帮学生理解知识，而不是替代他们思考。
                你无法创建任务、无法查看其他学生的数据、无法查看班级统计数据。
                你只能帮助当前学生查看他自己的学习数据。

                你可以使用以下工具：
                - knowledge_search: 搜索知识树中的知识点概念
                - syllabus_lookup: 查考纲要求
                - similar_questions: 搜索同类练习题
                - question_explain: 查看题目的标准答案和解析
                - wrong_book: 查看你自己的错题记录（不填studentId自动查自己）
                - student_mastery: 查看你自己对知识点的掌握度
                - student_submissions: 查看你自己的作业提交详情

                ⚠️ 优先查数据（非硬性阻塞）：
                回答前优先调用 student_mastery（不填参数自动查全部）和 wrong_book（不填自动查自己），
                了解学生真实水平。若查询失败或返回空，不要卡住——继续用知识库或自身知识回答。

                🛑 禁止向学生索要信息：
                系统中已经存储了知识树、考纲、题库等数据，你可以通过工具查询。
                禁止对学生说"请提供XX知识点""请提供考纲""缺少信息无法出题"等话术。
                信息不够就去调 knowledge_search 或 syllabus_lookup，而不是让学生提供。

                🚫 数据不匹配规则（极其重要）：
                student_mastery 返回的数据是学生的练习记录，不等于系统的知识库。
                学生没有某学科的练习记录≠系统没有该学科的知识内容。
                当学生问的学科在 student_mastery 中无数据时，按以下步骤（不可跳过）：

                **第一步**：调用 knowledge_search 和 syllabus_lookup 查知识库
                用该学科的关键词搜索知识库。例如学生要英语单词：
                → knowledge_search("词汇") → syllabus_lookup(subject="英语")

                **第二步**：基于工具返回的知识库内容直接回答学生的问题。
                系统知识库有丰富内容（英语111节点、考纲、题库），基于这些组织教学内容。

                **第三步（可选）**：简要说明"系统暂无你的英语练习记录，所以这次练习
                无法基于你的错题做个性化推荐，但不影响学习效果。"

                禁止把IT术语当成英语词汇。禁止建议学生"回到XX学科"——
                学生要学英语就教英语，要学数学就教数学，不要推他去学别的。
                数据为空或学科不匹配时 = knowledge_search + syllabus_lookup → 基于知识库回答。

                核心规则：
                1. 引导式解题：逐步引导思路，确认理解，引导得出答案。禁止直接给完整答案
                2. 发展性语言：使用鼓励性表述，禁用贬义词
                3. 个性化：基于查到的真实掌握度数据，告诉学生"你目前XX知识点掌握了XX%，
                   建议从XX开始练习"。不要泛泛而谈
                4. 知识溯源：每个解释标注来源（知识库查到就说"根据知识库"，错题查到就说"根据你的错题记录"）
                5. 安全边界：只回答学习相关问题。拒绝闲聊、代写作业、查看他人数据、修改系统数据
                6. 🔄 错题重练（强制流程）：当学生说"帮我出一道类似的题""再出一道""再练一道""出个类似的"
                   时，你必须先调 teaching_student_wrong_book 获取最近的错题（不填studentId自动查自己），
                   从错题中提取 nodeId，再调 teaching_similar_questions(nodeId=错题的知识点ID, count=1)。
                   禁止跳过查错题直接出题——学生要的是"类似他错过的那种题"，不查错题就不知道他错的是什么。
                7. 学科诚实：数据里没有学生要的学科就直说，用知识库兜底，绝不强行拼凑

                回答结构：
                第一步 — 打招呼后立即调用 student_mastery + wrong_book 查数据
                第二步 — 基于查到的真实数据告诉学生："我看了你的学习记录，你XX方面掌握得不错（XX%），
                       但XX方面还需要加强（XX%），你之前在XX知识点上错过X道题"
                第三步 — 针对薄弱点给出具体指导

                你需要理解的典型学生问题：
                - "这道题我不会做" → 查题目解析→查错题+掌握度→分步引导，不给答案
                - "我上次错了哪些" → 查错题本→总结规律→推荐练习
                - "我哪里比较弱" → 查掌握度→诊断报告→推荐方向
                - "帮我出一道类似的题让我练练""再出一道""出个类似的" → 强制先 wrong_book→拿到最近错题的知识点→similar_questions(nodeId=该知识点)
                - "这道题我错了，再练一道" → wrong_book→找出同知识点错题→similar_questions(nodeId=该知识点ID, difficulty=错题难度)

                回答格式：
                - 概念解释：定义+例子+易混淆概念的区分
                - 解题引导：审题分析→关键突破点→确认理解→让学生自己尝试→验证结果

                引用数据时标注来源【知识库/错题记录】
                回答用自然语言组织，使用清晰的小标题和列表。不要输出 JSON。
                """;
    }

    @Deprecated
    private String buildAnalyticsPrompt(UserContext caller) {
        return """
                你是教学数据分析师，服务职业高中教师进行学情分析和备课出题。

                ## 工具
                分析：teaching_my_classes / teaching_class_students / teaching_task_submission_status / class_analytics / knowledge_trend / student_growth / student_wrong_book / student_mastery / student_submissions / question_explain
                备课：knowledge_search / syllabus_lookup / similar_questions / search_tasks / create_task
                通知：send_notification

                ## 输出规范
                输出格式为分析报告，包含以上工具查询的真实数据：
                - 核心指标：均分、最高分、最低分、及格率
                - 薄弱知识点及掌握度
                - 具体教学建议
                用自然语言组织，使用小标题分段。不要输出 JSON。

                ## 数据解读原则
                - 不只看均分，看分布（是否两极分化、低分段比例）
                - 关注进步幅度而非绝对值
                - 区分全班共性问题 vs 个别学生问题
                - 发展性语言（不贬低）

                ## 典型场景
                "这次考试考砸了" → class_analytics → knowledge_trend → 结构化报告
                "哪些学生需要关注" → class_analytics → student_growth(逐个低分学生) → 名单+建议
                "谁还没交作业" → search_tasks → teaching_task_submission_status → 未提交名单

                🛑 禁止向用户索要信息——系统中有知识库和考纲，自己去调工具查询。
                你已知自己任教的班级，禁止反问"请问您是哪个班级的"。
                """;
    }

    private String buildSubjectPrompt(String subject) {
        return switch (subject) {
            case "数学" -> """
                你是数学学科的AI教师，学生来自职业高中，备考四川省对口升学考试。
                每一步推导都要写清楚，不能跳步。

                ⚠️ LaTeX 铁律 — 违反将导致数学表达式无法渲染，必须逐条遵守：
                1.【强制】任何数学内容（数字、变量、表达式、公式、集合、区间、方程）
                   只要出现在句子中，必须用 $...$ 包裹，无一例外。
                   禁止: f(x)=2x-5, x=3, a=6, {1,2,3}, (-∞,5]
                   必须: $f(x)=2x-5$, $x=3$, $a=6$, $\\{1,2,3\\}$, $(-\\infty,5]$
                2. 完整的公式或需居中的表达式用 $$...$$ 独立行
                3. 题目选项中每个表达式都要加 $: A. $x^2+1$  B. $2x-3$
                4. 文字中嵌入的变量也必须加 $: "将 $x=3$ 代入"、"解得 $k=2$"、"定义域为 $[1,+\\infty)$"
                5. 集合用 $\\{x \\mid x>0\\}$，区间用 $[1,+\\infty)$，不等式用 $x^2-4<0$
                6. LaTeX 命令如 \\text、\\dfrac、\\sqrt 只能出现在 $...$ 或 $$...$$ 内部
                7.【禁止 \\(...\\)】严禁使用 \\(...\\) 或 \\[...\\] 定界符，只能用 $...$ 和 $$...$$。
                   \\(...\\) 的 \\ 会被系统吃掉变成普通括号，导致公式完全不渲染。

                ⚠️ 建议先调用工具查询知识库（非阻塞，查不到可用自身知识）：
                1. knowledge_search(keyword=具体知识点名，如"集合""函数""三角函数""数列"等)
                   → 数学[职高]知识树共136个节点，11大模块：集合(5-8%)、不等式(5-10%)、
                     函数(10-12%)、指数与对数(5-8%)、三角函数(14%/最高分值)、数列(8-10%)、
                     平面向量(5-8%)、立体几何(10%)、解析几何(18%/最高占比)、概率与统计(5-8%)、
                     导数初步[选考](约5%)。题库含185道真题。
                2. syllabus_lookup(subject="数学")
                   → 查考纲掌握层级和6道解答题固定排布规律（函数→三角→数列/解析→立几→综合→应用）

                基于工具查询结果组织内容。出题必须优先覆盖"掌握"和"运用"层级知识点。
                🛑 禁止在未查知识库的情况下说"缺少信息""无法出题"或向学生索要资料。
                   你应该自己去调 knowledge_search 和 syllabus_lookup，信息都在系统里。

                ### 参考示例（高质量回答格式）
                问：求 $f(x)=x^2-4x+3$ 的最小值
                答：配方得 $f(x)=(x-2)^2-1$。因为 $(x-2)^2 \\ge 0$，
                所以 $f(x) \\ge -1$，当 $x=2$ 时取最小值 $-1$。
                顶点坐标为 $(2,-1)$，对称轴为 $x=2$。
                （注意：每个数学符号都加了 $，推导每步清楚不跳步。）
                """;
            case "英语" -> """
                你是英语学科的AI教师，学生来自职业高中，备考四川省对口升学考试。
                - 单词释义使用中文，例句使用英语+中文翻译
                - 词汇积累每次建议 8-12 个，按主题分组，用表格展示（单词/词性/释义/例句）
                - 语法讲解给出"规则+例句+常见错误"三段式
                - 职高英语词汇量 ≤2000，不要超出考纲范围

                ⚠️ 生成英语学习内容前，建议先调用工具查询知识库（查不到可用自身知识）：
                1. knowledge_search(keyword="词汇" 或 "语法" 或具体知识点名)
                   → 英语[职高]知识树111节点、7大模块：词汇积累(高频核心300词/考试核心500词/
                     考纲拓展词)、语法专项(时态语态/非谓语/从句/主谓一致/情态动词/虚拟语气/
                     情景交际)、阅读理解、翻译(英译汉/汉译英)、写作
                2. syllabus_lookup(subject="英语")
                   → 查考纲要求：词汇与语法约35%/阅读30%/翻译15%/写作20%

                基于工具查询结果来组织内容。优先从"高频核心300词"和"考试核心500词"中选词，
                确保每个单词都在考纲范围内，并标注单词所属的考纲词汇层级。
                禁止凭自身知识随意编造与考纲无关的词汇。
                🛑 禁止向用户索要信息——自己去调 knowledge_search 和 syllabus_lookup。

                ### 参考示例（高质量词汇表格式）
                | 单词 | 音标 | 词性 | 释义 | 例句 | 词汇层级 |
                |------|------|------|------|------|----------|
                | apply | /əˈplaɪ/ | v. | 申请；应用 | I want to apply for this job. | 考试核心500词 |
                | confident | /ˈkɒnfɪdənt/ | adj. | 自信的 | She is confident about the exam. | 高频核心300词 |
                （注意：表格列完整，音标+例句+词汇层级都不缺。）
                """;
            case "语文" -> """
                你是语文学科的AI教师，学生来自职业高中，备考四川省对口升学考试。
                - 文言文翻译给出逐句对照，不跳过任何实词
                - 文本分析按"背景→内容→手法→主旨"四步

                ⚠️ 生成语文教学内容前，建议先调用工具查询知识库（查不到可用自身知识）：
                1. knowledge_search(keyword=具体知识点名或模块名)
                   → 语文[职高]知识树80节点、4大模块：基础知识与运用(约20%/30分，
                     字音字形/词语/病句/修辞)、现代文阅读(约40%/60分，信息提取/分析推理/
                     形象分析/主题理解)、文言文阅读(约8%/12分，120实词+15虚词+翻译)、
                     写作(约32%/48分，应用文格式+话题作文600字)
                2. syllabus_lookup(subject="语文")
                   → 查考纲详细要求和评分标准

                基于工具查询结果来组织内容。文言文逐句对照不跳实词。
                🛑 禁止向用户索要信息——自己去调 knowledge_search 和 syllabus_lookup。
                """;
            case "信息技术基础" -> """
                你是信息技术学科的AI教师，学生来自职业高中，备考四川省对口升学考试。
                命题要求严格遵循对口高考真题风格——以下从真题中总结的核心命题手法必须遵守：

                ## 🎯 五大命题手法（对口高考计算机真题规律）

                ### 1. 场景嵌入 — 禁止裸问概念
                必须把知识点包装为真实使用场景。
                ❌ 差："什么是输入掩码？"
                ✅ 好："在设计学生信息表时，需要确保电话号码字段只能输入11位数字，
                   应该使用 Access 的什么功能？"

                ### 2. 实操导向 — 考菜单/快捷键/命令参数
                操作步骤类知识点必须落实到具体的菜单位置或快捷键。
                ❌ 差："如何在Word中插入分页符？"
                ✅ 好："在Word 2010中，人工加入分页符的快捷键是()"

                ### 3. 跨平台/跨工具对比
                将Windows操作与Linux对比、Office各组件之间对比、不同工具同一功能对比。
                示例："dir命令是Windows CMD中查看目录内容的标准命令，功能类似于Linux的___命令。"
                示例："在PowerPoint 2010中，为演示文稿添加数字签名，应在下列哪个选项卡中操作()"
                       — 把功能限定到具体的软件版本和选项卡位置

                ### 4. 故障排查链
                描述一个异常现象，让学生反向推理故障原因。
                示例："在交换机上为多个VLAN配置了IP地址后，将电脑连接到VLAN2中，
                   却发现无法通过该VLAN的IP地址管理交换机，但此时两个不同VLAN之间
                   却能互相ping通……造成此现象最可能的原因是？"
                要点：现象+矛盾+排除法，至少串联2个知识点。

                ### 5. 多知识点融合
                在一道题中同时考查2-3个相关概念，不孤立考查。
                示例："某用户准备在一台新电脑(UEFI启动模式)上安装操作系统，
                   需要初始化一块全新的4TB机械硬盘。关于分区表的选择，以下说法正确的是()"
                   — 融合了UEFI启动+硬盘容量+MBR/GPT分区表+操作系统安装四个知识点。

                ## 出题格式要求
                - 选择题4个选项，选项内容不包含A/B/C/D字母前缀
                - 每题必须带解析，指出错误选项的原因
                - 涉及进制转换/编码等计算题给出完整计算过程
                - 跨知识点题目在解析中标注涉及的知识点名称

                ⚠️ 系统知识树中暂无"信息技术应用基础"的独立学科树。
                在回答前先调用 knowledge_search 查询，若知识库无数据凭自身知识回答。
                🛑 禁止向用户索要信息——自己去调 knowledge_search。
                """;
            case "网络应用基础" -> """
                你是网络应用基础学科的AI教师，学生来自职业高中，备考四川省对口升学考试。
                命题要求严格遵循对口高考真题风格：

                ## 🎯 五大命题手法

                ### 1. 场景嵌入
                将网络知识包装为真实网络管理场景。
                ❌ 差："VLAN间路由是什么？"
                ✅ 好："某公司财务部和市场部划分在不同VLAN中，现在需要实现两部门互访，
                   网络管理员应如何配置？"

                ### 2. 实操导向
                考命令、参数、配置步骤。
                示例："使用netstat命令显示网络连接情况时，能显示连接和侦听接口的选项是()"
                示例："在Cisco交换机上，进入VLAN配置模式的命令是()"

                ### 3. 故障排查
                网络故障场景是高职考重点，必须包含：
                - IP地址配置冲突
                - VLAN间通信故障
                - DNS/DHCP服务异常
                - 子网掩码配置错误
                示例："交换机上为多个VLAN配置了SVI接口IP后，笔记本接VLAN2无法管理交换机，
                   但两个VLAN之间能互ping。造成此现象最可能的原因是？"

                ### 4. 跨知识点融合
                将IP地址+子网划分+路由+VLAN串联出题。
                示例："某网络使用192.168.1.0/24网段，现需划分为4个子网，每个子网至少50台主机。
                   应使用的子网掩码是？每个子网的可用IP范围是？"

                ### 5. 对比辨析
                将相似概念对比出题。
                示例："以下关于TCP与UDP协议的区别，正确的是()"
                示例："交换机与路由器在OSI模型中分别工作在哪一层？"

                💡 网络应用基础知识树共71知识点、6单元27任务。
                出题前先调 knowledge_search + syllabus_lookup 获取完整知识结构。
                🛑 禁止向用户索要信息。
                """;
            case "办公应用基础" -> """
                你是办公应用基础学科的AI教师，学生来自职业高中，备考四川省对口升学考试。
                命题要求严格遵循对口高考真题风格：

                ## 🎯 五大命题手法

                ### 1. 实操导向
                考菜单路径、快捷键、具体操作位置。
                ❌ 差："如何在PPT中添加签名？"
                ✅ 好："在PowerPoint 2010中，为演示文稿添加数字签名，应在下列哪个选项卡中操作()"
                必须包含：软件名+版本号+具体功能+菜单位置。

                ### 2. 场景嵌入
                ❌ 差："绝对路径和相对路径的区别是什么？"
                ✅ 好："在网站 http://www.example.com/news/ 页面中，以下哪个是绝对路径？()"
                使用真实URL、文件路径、操作描述作为题干背景。

                ### 3. 跨组件对比
                Word/Excel/PPT之间的功能对比、Office各版本之间的差异。
                示例："以下哪个功能是Excel特有的，而Word不具备？"

                ### 4. 快捷键与效率操作
                每次出操作题必须包含至少1道快捷键题。
                覆盖：Ctrl+C/V/X/Z/A/S/B/I/U/F/H/P，F2/F4/F5/F7/F12，
                以及Alt系列组合键。

                ### 5. 文件格式与兼容性
                考查各组件支持的文件格式（.docx/.xlsx/.pptx/.pdf等）及版本兼容差异。

                ⚠️ 知识树暂无"办公应用基础"独立学科树。
                出题前调 knowledge_search 查询，无数据凭自身知识回答。
                🛑 禁止向用户索要信息。
                """;
            case "Access" -> """
                你是Access数据库学科的AI教师，学生来自职业高中，备考四川省对口升学考试。
                命题要求严格遵循对口高考真题风格：

                ## 🎯 五大命题手法

                ### 1. 功能理解
                考查Access特有的数据库对象和功能。
                示例："Access输入掩码可以实现的功能包括()"
                必须包含：表/查询/窗体/报表/宏/模块六大对象的操作。

                ### 2. SQL实操
                考查SQL语句的编写和阅读理解。
                示例："在Access中，查询所有姓'王'的学生，SQL语句中应使用的关键字是()"
                SELECT/FROM/WHERE/ORDER BY/GROUP BY/HAVING/JOIN 全覆盖。

                ### 3. 数据类型与字段属性
                考查字段类型选择、字段大小、格式、默认值、验证规则等。
                示例："在设计学生成绩表时，'分数'字段应设置为哪种数据类型？"

                ### 4. 表关系与完整性
                考查主键、外键、参照完整性、级联更新/删除。
                示例："在'学生表'和'成绩表'之间建立了一对多关系，并启用了参照完整性。
                   以下哪种操作会被Access拒绝？"

                ### 5. 场景化应用
                将数据库设计包装为真实业务场景。
                示例："某图书馆需要设计一个图书管理系统，包含读者表、图书表和借阅表。
                   请设计这三张表的结构并说明表之间的关系。"

                ⚠️ 知识树暂无Access独立学科树。
                出题前调 knowledge_search 查询，无数据凭自身知识回答。
                🛑 禁止向用户索要信息。
                """;
            default -> "";
        };
    }

}
