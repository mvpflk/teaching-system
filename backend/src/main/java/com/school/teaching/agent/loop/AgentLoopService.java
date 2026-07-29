package com.school.teaching.agent.loop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.agent.audit.AgentAuditLogger;
import com.school.teaching.agent.config.AgentConfig;
import com.school.teaching.agent.security.UserContext;
import com.school.teaching.agent.session.ConversationSession;
import com.school.teaching.agent.session.Message;
import com.school.teaching.agent.session.SessionManager;
import com.school.teaching.agent.tool.ToolCall;
import com.school.teaching.agent.tool.ToolRegistry;
import com.school.teaching.agent.tool.ToolResult;
import com.school.teaching.agent.tool.MathOutputValidator;
import com.school.teaching.agent.tool.ToolAccessDeniedException;
import com.school.teaching.agent.memory.AgentMemoryService;
import com.school.teaching.agent.prompt.PromptTemplateCache;
import com.school.teaching.entity.AgentConversation;
import com.school.teaching.entity.AgentSessionPrompt;
import com.school.teaching.mapper.AgentConversationMapper;
import com.school.teaching.mapper.AgentSessionPromptMapper;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.entity.PrecisionProgress;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.security.ContentSafetyFilter;
import com.school.teaching.service.AiServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class AgentLoopService {

    private final AiServiceGateway gateway;
    private final ToolRegistry toolRegistry;
    private final SessionManager sessionManager;
    private final AgentAuditLogger auditLogger;
    private final ExecutorService agentExecutor;
    private final Semaphore concurrencyLimit;
    private final ObjectMapper om;
    private final PrecisionProgressMapper precisionProgressMapper;
    private final AgentConversationMapper agentConversationMapper;
    private final AgentMemoryService memoryService;
    private final com.school.teaching.service.PptGenerationService pptService;
    private final AgentConfig agentConfig;
    private final ContentSafetyFilter safetyFilter;

    @Autowired(required = false)
    private PromptTemplateCache promptTemplateCache;
    @Autowired(required = false)
    private AgentSessionPromptMapper agentSessionPromptMapper;

    /** G-4: 待用户确认的写操作 — sessionId → CompletableFuture */
    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> pendingConfirmations = new ConcurrentHashMap<>();

    public AgentLoopService(AiServiceGateway gateway,
                            ToolRegistry toolRegistry,
                            SessionManager sessionManager, AgentAuditLogger auditLogger,
                            ExecutorService agentExecutor, Semaphore concurrencyLimit,
                            PrecisionProgressMapper precisionProgressMapper,
                            AgentConversationMapper agentConversationMapper,
                            AgentMemoryService memoryService,
                            com.school.teaching.service.PptGenerationService pptService,
                            AgentConfig agentConfig,
                            ContentSafetyFilter safetyFilter) {
        this.gateway = gateway;
        this.toolRegistry = toolRegistry;
        this.sessionManager = sessionManager;
        this.auditLogger = auditLogger;
        this.agentExecutor = agentExecutor;
        this.concurrencyLimit = concurrencyLimit;
        this.om = new ObjectMapper();
        this.precisionProgressMapper = precisionProgressMapper;
        this.agentConversationMapper = agentConversationMapper;
        this.memoryService = memoryService;
        this.pptService = pptService;
        this.agentConfig = agentConfig;
        this.safetyFilter = safetyFilter;
    }

    /** 暴露线程池供健康检查使用 */
    public ExecutorService getExecutor() { return agentExecutor; }

    /** 暴露并发信号量供健康检查使用 */
    public Semaphore getConcurrencyLimit() { return concurrencyLimit; }

    public void execute(String sessionId, String userMessage, AgentType agentType,
                        UserContext caller, SseEmitter emitter, String systemPrompt) {
        execute(sessionId, userMessage, agentType, caller, emitter, systemPrompt, null, null, null, null);
    }

    public void execute(String sessionId, String userMessage, AgentType agentType,
                        UserContext caller, SseEmitter emitter, String systemPrompt,
                        String customApiKey, String customBaseUrl, String customModel) {
        execute(sessionId, userMessage, agentType, caller, emitter, systemPrompt, customApiKey, customBaseUrl, customModel, null);
    }

    public void execute(String sessionId, String userMessage, AgentType agentType,
                        UserContext caller, SseEmitter emitter, String systemPrompt,
                        String customApiKey, String customBaseUrl, String customModel,
                        String subject) {
        agentExecutor.submit(() -> {
            boolean acquired = false;
            try {
                if (!concurrencyLimit.tryAcquire(30, TimeUnit.SECONDS)) {
                    sendEvent(emitter, "error", "AI 服务繁忙，请稍后重试");
                    emitter.complete();
                    return;
                }
                acquired = true;
                runLoop(sessionId, userMessage, agentType, caller, emitter, systemPrompt,
                        customApiKey, customBaseUrl, customModel, subject);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendEvent(emitter, "error", "请求被中断");
                emitter.complete();
            } catch (Exception e) {
                log.error("AgentLoop 异常: sessionId={}, userId={}", sessionId, caller.getUserId(), e);
                sendEvent(emitter, "error", "AI 处理异常，请稍后重试");
                emitter.complete();
            } finally {
                if (acquired) {
                    concurrencyLimit.release();
                }
            }
        });
    }

    private void runLoop(String sessionId, String userMessage, AgentType agentType,
                         UserContext caller, SseEmitter emitter, String systemPrompt,
                         String customApiKey, String customBaseUrl, String customModel,
                         String subject) {
        long startTime = System.currentTimeMillis();
        List<Message> messages = initSessionMessages(sessionId, userMessage, systemPrompt, agentType, caller, emitter);
        if (messages == null) return;

        // 记录本次对话使用的提示词版本
        recordSessionPrompt(sessionId, agentType, subject);

        List<Map<String, Object>> tools = toolRegistry.getDefinitions(caller);

        // G-5 目标跟踪
        String originalGoal = extractGoal(userMessage);
        int stepsWithoutGoal = 0;

        int step = 0;
        sendEvent(emitter, "thinking", "正在思考…");
        try {
            while (step < agentConfig.getMaxSteps()) {
                step++;

                if (System.currentTimeMillis() - startTime > agentConfig.getTotalTimeoutMs()) {
                    sendEvent(emitter, "warning", "分析时间较长，正在基于已有数据生成结果…");
                    break;
                }

                // G-5 目标跟踪偏离检测
                if (originalGoal != null && stepsWithoutGoal > 3) {
                    messages.add(Message.system("注意：当前对话可能偏离了用户的原始目标（" + originalGoal
                            + "）。请在接下来的回复中回到这个主题，或确认是否需要调整目标。"));
                    stepsWithoutGoal = 0;
                }

                // 智能催促
                stepUrgeIfNeeded(step, messages);

                // Token 压缩
                int tokenCount = sessionManager.estimateTokens(messages);
                if (tokenCount > agentConfig.getTokenWarning()) {
                    messages = sessionManager.compressHistory(messages);
                }

                try {
                    String model = customModel != null && !customModel.isBlank() ? customModel : DEFAULT_MODEL;
                    String responseJson = callDeepSeekWithTools(messages, tools, agentType,
                            customApiKey, customBaseUrl, model, caller.getUserId());
                    DeepSeekStreamResponse parsed = parseStreamResponse(responseJson);

                    if (parsed.hasToolCalls()) {
                        handleToolCallsInLoop(parsed.getToolCalls(), messages, caller, emitter, sessionId, originalGoal);
                        continue;
                    }

                    if (parsed.hasContent()) {
                        handleContentInLoop(parsed.getContent(), agentType, emitter, messages,
                                customApiKey, customBaseUrl, customModel, originalGoal);
                        stepsWithoutGoal = 0;
                        break;
                    }
                } catch (Exception apiEx) {
                    if (handleApiError(apiEx, userMessage, agentType, emitter)) {
                        return;
                    }
                    log.warn("DeepSeek API 调用失败 (step={}), 降级为纯文本", step, apiEx);
                    String fallback = fallbackCall(userMessage, agentType);
                    if (fallback != null) {
                        streamTextChunks(emitter, fallback);
                        messages.add(Message.assistant(fallback));
                    }
                    break;
                }
            }

            if (step >= agentConfig.getMaxSteps()) {
                String finalMsg = fallbackCall("请基于以上所有数据给出最终回答", agentType);
                streamTextChunks(emitter, finalMsg);
                messages.add(Message.assistant(finalMsg));
            }

            finalizeSession(sessionId, userMessage, messages, caller, agentType, emitter);
        } catch (Exception e) {
            log.error("AgentLoop 执行异常: sessionId={}, userId={}", sessionId, caller.getUserId(), e);
            sendEvent(emitter, "error", "分析过程中出现异常，已获取的部分结果如上所示。可重新提问获取完整分析。");
        } finally {
            emitter.complete();
        }
    }

    /** E-1: 初始化会话消息（注入 system prompt、摘要、冷启动、掌握度、安全检查） */
    private List<Message> initSessionMessages(String sessionId, String userMessage, String systemPrompt,
                                              AgentType agentType, UserContext caller, SseEmitter emitter) {
        List<Message> messages = sessionManager.loadMessages(sessionId);
        if (messages.isEmpty()) {
            messages.add(Message.system(systemPrompt));
            String prevSummary = sessionManager.getLastSessionSummary(caller.getUserId());
            if (prevSummary != null && !prevSummary.isBlank()) {
                String formatted = formatPrevSummary(prevSummary);
                messages.add(Message.system("【上次对话摘要】" + formatted
                        + "\n你可以参考上次的情况继续辅导，也可以先问学生是否需要回顾上次的内容。"));
            } else {
                String coldStartHint = buildColdStartHint(agentType, caller);
                if (coldStartHint != null) {
                    messages.add(Message.system(coldStartHint));
                }
            }
            if (caller.isStudent() && caller.getStudentId() != null) {
                String masteryContext = buildMasteryContext(caller.getStudentId());
                if (masteryContext != null) {
                    messages.add(Message.system(masteryContext));
                }
            }
        }

        // 安全防线：输入安全校验
        try { safetyFilter.validateInput(userMessage); }
        catch (com.school.teaching.exception.BusinessException e) {
            sendEvent(emitter, "error", e.getMessage());
            emitter.complete(); return null;
        }

        // 3.6 学生心理健康危机检测
        if (caller.isStudent()) {
            String crisisMsg = safetyFilter.checkStudentCrisis(userMessage);
            if (crisisMsg != null) sendEvent(emitter, "crisis_warning", crisisMsg);
        }

        // G-1 规划注入
        if (isMultiIntent(userMessage)) {
            messages.add(Message.system("这条消息包含多个任务。请先制定一个简短的执行计划"
                    + "（列出每个步骤要使用的工具和预期结果），然后按步骤执行。"));
        }
        messages.add(Message.user(userMessage));
        return messages;
    }

    /** E-1: 工具调用处理（含并行执行、写操作确认、阶段化进度） */
    private void handleToolCallsInLoop(List<ToolCall> toolCalls, List<Message> messages,
                                       UserContext caller, SseEmitter emitter, String sessionId,
                                       String originalGoal) {
        for (ToolCall tc : toolCalls) {
            String argsJson;
            try {
                argsJson = om.writeValueAsString(tc.getArguments());
            } catch (JsonProcessingException e) {
                argsJson = String.valueOf(tc.getArguments());
            }
            sendEvent(emitter, "tool_start",
                    Map.of("tool", tc.getName(),
                           "progress", toolProgressDescription(tc.getName()),
                           "args", argsJson));
        }

        messages.add(Message.builder().role("assistant")
                .toolCalls(toDeepSeekToolCallRefs(toolCalls)).build());

        // G-4 写操作确认 — 阻塞等待用户确认
        if (toolCalls.stream().anyMatch(tc -> isWriteTool(tc.getName()))) {
            List<String> writeToolNames = toolCalls.stream()
                    .filter(tc -> isWriteTool(tc.getName()))
                    .map(ToolCall::getName)
                    .toList();
            sendEvent(emitter, "confirm_write", Map.of(
                    "message", "即将执行写操作",
                    "tools", writeToolNames));
            if (!waitForUserConfirm(sessionId, caller.getUserId())) {
                for (ToolCall tc : toolCalls) {
                    sendEvent(emitter, "tool_end",
                            Map.of("tool", tc.getName(), "status", "cancelled"));
                    messages.add(Message.toolResult(tc.getId(),
                            "{\"error\":\"用户已取消该操作\"}"));
                }
                return;
            }
        }

        // G-2 并行或串行执行
        if (toolCalls.size() == 1) {
            executeSingleTool(toolCalls.get(0), caller, emitter, sessionId, messages);
        } else {
            executeToolsParallel(toolCalls, caller, emitter, sessionId, messages);
        }
    }

    /** E-1: LLM 内容输出处理（含 LaTeX 检测、自反思、安全过滤、结构化解析） */
    private void handleContentInLoop(String raw, AgentType agentType, SseEmitter emitter,
                                     List<Message> messages, String customApiKey,
                                     String customBaseUrl, String customModel, String originalGoal) {
        // LaTeX 质量检测
        if (raw.contains("\\") && (raw.contains("$") || raw.contains("f(") || raw.contains("x="))) {
            String warning = MathOutputValidator.validate(raw);
            if (warning != null) raw = raw + warning;
        }

        // G-3 自反思
        String selfReflection = selfReflect(raw);
        if (selfReflection != null) raw = raw + "\n\n" + selfReflection;

        // 安全输出过滤
        String safetyViolation = safetyFilter.checkOutput(raw);
        if (safetyViolation != null) {
            sendEvent(emitter, "error", safetyViolation);
            messages.add(Message.assistant("[内容已被安全过滤]"));
            return;
        }

        streamTextChunks(emitter, raw);
        sendEvent(emitter, "thinking", "思考完成");
        messages.add(Message.assistant(raw));

        // STUDY_BUDDY 答案保护
        if (agentType == AgentType.STUDY_BUDDY) {
            String answerWarning = checkAnswerLeak(raw);
            if (answerWarning != null) sendEvent(emitter, "answer_warning", answerWarning);
        }

        // 结构化输出解析
        Map<String, Object> structured = parseStructuredOutput(raw);
        if (structured != null && AgentContentType.validate(structured)) {
            sendEvent(emitter, "schema", raw);
        } else if (looksLikeJson(raw)) {
            String formatted = reformatToJson(raw, agentType, customApiKey, customBaseUrl, customModel);
            if (formatted != null) sendEvent(emitter, "schema", formatted);
        }
    }

    /** E-1: API 错误处理，返回 true 表示需终止循环 */
    private boolean handleApiError(Exception apiEx, String userMessage, AgentType agentType, SseEmitter emitter) {
        if (isKeyMissingError(apiEx)) {
            sendEvent(emitter, "api_key_missing", "AI 服务未配置 API Key。请联系管理员配置系统 Key。");
            emitter.complete(); return true;
        }
        if (isKeyInvalidError(apiEx)) {
            sendEvent(emitter, "api_key_invalid", "API Key 无效或已过期。请检查 API Key 是否正确、余额是否充足。");
            emitter.complete(); return true;
        }
        if (isRateLimitError(apiEx)) log.warn("DeepSeek 请求受限");
        return false;
    }

    /** E-1: 会话收尾（保存消息、更新摘要、持久化、提取事实） */
    private void finalizeSession(String sessionId, String userMessage, List<Message> messages,
                                 UserContext caller, AgentType agentType, SseEmitter emitter) {
        sessionManager.saveMessages(sessionId, messages);
        updateSessionSummary(sessionId, userMessage, messages);
        persistConversation(sessionId, caller, agentType, messages);
        extractFactsFromConversation(caller, messages, sessionId);
        sendEvent(emitter, "done", Map.of("sessionId", sessionId));
    }

    /** E-1: 智能催促——步骤过多时提醒尽快收尾 */
    private void stepUrgeIfNeeded(int step, List<Message> messages) {
        if (step < 8) return;
        Message last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        boolean justGotToolResult = last != null && "tool".equals(last.getRole());
        if (step >= 9 || !justGotToolResult) {
            messages.add(Message.system("请基于已有数据尽快给出最终答案，无需再查询更多信息。"));
        }
    }

    private static final String DEFAULT_MODEL = "deepseek-v4-pro";

    private String callDeepSeekWithTools(List<Message> messages, List<Map<String, Object>> tools,
                                         AgentType agentType) {
        return callDeepSeekWithTools(messages, tools, agentType, null, null, DEFAULT_MODEL, null);
    }

    private String callDeepSeekWithTools(List<Message> messages, List<Map<String, Object>> tools,
                                         AgentType agentType,
                                         String customApiKey, String customBaseUrl, String customModel,
                                         Long userId) {
        List<Map<String, Object>> apiMessages = buildApiMessages(messages);
        Map<String, Object> result = gateway.callWithTools(
                apiMessages, tools, agentType.getTemperature(), agentType.getMaxTokens(),
                customApiKey, customBaseUrl, customModel, userId);
        return (String) result.get("body");
    }

    private List<Map<String, Object>> buildApiMessages(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message m : messages) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("role", m.getRole());
            if (m.getContent() != null) {
                map.put("content", m.getContent());
            }
            // DeepSeek V4: assistant 消息的 reasoning_content 必须回传
            if (m.getReasoningContent() != null) {
                map.put("reasoning_content", m.getReasoningContent());
            }
            if (m.getToolCalls() != null) {
                map.put("tool_calls", m.getToolCalls());
            }
            if (m.getToolCallId() != null) {
                map.put("tool_call_id", m.getToolCallId());
            }
            result.add(map);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private DeepSeekStreamResponse parseStreamResponse(String responseJson) {
        try {
            Map<String, Object> root = om.readValue(responseJson, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
            if (choices == null || choices.isEmpty()) {
                return DeepSeekStreamResponse.empty();
            }
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            if (message == null) {
                return DeepSeekStreamResponse.empty();
            }

            DeepSeekStreamResponse result = new DeepSeekStreamResponse();
            result.setReasoningContent((String) message.get("reasoning_content"));
            String finishReason = (String) choice.get("finish_reason");

            if ("tool_calls".equals(finishReason)) {
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
                if (toolCalls != null) {
                    List<ToolCall> calls = new ArrayList<>();
                    for (Map<String, Object> tc : toolCalls) {
                        ToolCall call = new ToolCall();
                        call.setId((String) tc.get("id"));
                        Map<String, Object> func = (Map<String, Object>) tc.get("function");
                        if (func != null) {
                            call.setName((String) func.get("name"));
                            String argsStr = (String) func.get("arguments");
                            try {
                                call.setArguments(om.readValue(argsStr, new TypeReference<>() {}));
                            } catch (JsonProcessingException e) {
                                call.setArguments(Map.of("_raw", argsStr));
                            }
                        }
                        calls.add(call);
                    }
                    result.setToolCalls(calls);
                }
            } else if ("stop".equals(finishReason)) {
                result.setContent((String) message.get("content"));
            }

            return result;
        } catch (JsonProcessingException e) {
            log.error("解析 DeepSeek 响应失败", e);
            return DeepSeekStreamResponse.empty();
        }
    }

    private List<Message.ToolCallRef> toDeepSeekToolCallRefs(List<ToolCall> toolCalls) {
        List<Message.ToolCallRef> refs = new ArrayList<>();
        for (ToolCall tc : toolCalls) {
            Message.ToolCallRef ref = new Message.ToolCallRef();
            ref.setId(tc.getId());
            ref.setType("function");
            try {
                ref.setFunction(new Message.ToolCallRef.FunctionRef(
                        tc.getName(), om.writeValueAsString(tc.getArguments())));
            } catch (JsonProcessingException e) {
                ref.setFunction(new Message.ToolCallRef.FunctionRef(tc.getName(), "{}"));
            }
            refs.add(ref);
        }
        return refs;
    }

    private String fallbackCall(String userMessage, AgentType agentType) {
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("prompt", userMessage);
            params.put("temperature", agentType.getTemperature());
            params.put("maxTokens", agentType.getMaxTokens());
            return gateway.generateContent(params);
        } catch (Exception e) {
            log.error("降级调用失败", e);
            if (isKeyMissingError(e) || isKeyInvalidError(e)) return null;
            return "抱歉，AI 服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 两段式生成 — 第二阶段：从自由文本中提取结构化 JSON。
     * 不走熔断器（generateContentQuiet），prompt 极简（只做提取），temperature=0.1。
     */
    private String reformatToJson(String rawText, AgentType agentType,
                                  String customApiKey, String customBaseUrl, String customModel) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("reformatToJson 输入为空");
            return null;
        }
        try {
            String prompt = """
                    从以下教学文本中提取关键信息，按 JSON 格式输出。只输出 JSON，不要其他文字。

                    先判断文本最匹配的内容类型，然后用对应字段填充：

                    knowledge_card: {title, subject, summary,
                      key_points: [{label:"要点名", content:"内容"}],
                      examples: [{input:"示例输入", output:"结果", explanation:"说明"}],
                      common_mistakes:["常见错误1"], related_knowledge:["相关知识"]}

                    vocabulary_list: {title, subject,
                      items: [{word:"单词", pos:"词性", meaning:"中文释义", example:"英文例句", example_cn:"中文翻译"}]}

                    exercise_set: {title, subject, difficulty:"1~5的数字",
                      questions: [{id:"q1", stem:"题目", options:["A.xx","B.xx"], answer:"正确答案", explanation:"解析"}]}

                    step_by_step: {title, subject, answer:"最终答案",
                      steps: [{num:1, title:"步骤名", content:"内容", formula:"公式"}]}

                    analysis_report: {title, subject, summary,
                      metrics: [{label:"指标名", value:"数值", trend:"+5%"或"-3%"}],
                      weak_points: [{node:"知识点", mastery:掌握度0-100, severity:"high或medium"}]}

                    comparison: {title,
                      headers:["列名1","列名2"],
                      rows: [{label:"行标签", values:["值1","值2"]}]}

                    learning_path: {title, subject,
                      nodes: [{name:"节点名", status:"mastered/in_progress/locked", mastery:掌握度0-100}]}

                    缺失的字段设为 null。最顶层必须有 type 字段。

                    文本：
                    %s
                    """.formatted(rawText);

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("prompt", prompt);
            params.put("temperature", 0.1);
            params.put("maxTokens", 4096);
            params.put("_skipSystemPrompt", true);

            String result = gateway.generateContentQuiet(params, customApiKey, customBaseUrl, customModel);
            if (result == null || result.isBlank()) {
                log.warn("reformatToJson 返回空");
                return null;
            }

            Map<String, Object> parsed = parseStructuredOutput(result);
            if (parsed != null && AgentContentType.validate(parsed)) {
                log.info("reformatToJson 成功: type={}", parsed.get("type"));
                return om.writeValueAsString(parsed);
            }
            log.warn("reformatToJson 返回值未通过验证, 长度={}", result.length());
            return null;
        } catch (Exception e) {
            log.warn("reformatToJson 调用异常: {}", e.getMessage());
            return null;
        }
    }

    /** API Key 完全未配置（空白） */
    private boolean isKeyMissingError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        return msg.contains("API Key 未配置") || (msg.contains("API Key") && msg.contains("未配置"));
    }

    /** API Key 无效/过期/格式错误 */
    private boolean isKeyInvalidError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        return msg.contains("API Key 无效") || msg.contains("API Key 解密失败")
            || msg.contains("401") || msg.contains("403");
    }

    /** 请求过于频繁或服务器繁忙 */
    private boolean isRateLimitError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        return msg.contains("429") || msg.contains("请求过于频繁") || msg.contains("服务器繁忙");
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            // SSE 传输中 \n 会被 Spring SseEmitter 当作行分隔符吞掉。
            // 对 String 类型数据统一把 \n 编码为 Unicode PUA U+E000，前端解码还原。
            Object safe = (data instanceof String s) ? s.replace('\n', '') : data;
            emitter.send(SseEmitter.event().name(name).data(safe));
        } catch (IOException e) {
            log.debug("SSE 发送失败（客户端可能已断开）");
        }
    }

    private void streamTextChunks(SseEmitter emitter, String fullText) {
        // SSE 传输中 \n 会被 Spring SseEmitter 当作行分隔符吞掉，
        // 导致 Markdown 表格换行丢失、所有行拼接。用 Unicode PUA 字符 U+E000 编码。
        final char NL = '';
        String safeText = fullText.replace('\n', NL);

        final int chunkSize = 50;
        int i = 0;
        while (i < safeText.length()) {
            int end = Math.min(i + chunkSize, safeText.length());
            // 向后延伸到自然断句点，避免 Markdown 语法被截断
            if (end < safeText.length()) {
                int look = Math.min(end + 20, safeText.length());
                for (int j = end; j < look; j++) {
                    char c = safeText.charAt(j);
                    if (c == NL || c == ' ' || c == '，' || c == '。' || c == '、') {
                        end = j + 1;
                        break;
                    }
                }
            }
            String chunk = safeText.substring(i, end);
            sendEvent(emitter, "text", chunk);
            i = end;  // 无缝衔接，不重叠
            try { Thread.sleep(20); } catch (InterruptedException ex) { break; }
        }
    }

    /**
     * 尝试将模型输出解析为结构化 JSON。
     * 优先直接 JSON.parse，失败则尝试提取 ```json 代码块。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseStructuredOutput(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // 直接解析
        try {
            Map<String, Object> result = om.readValue(raw, new TypeReference<>() {});
            if (result.containsKey("type")) return result;
        } catch (JsonProcessingException ignored) { log.debug("JSON直接解析失败, 尝试代码块提取"); }
        // 提取 ```json ... ``` 块
        int jsonStart = raw.indexOf("```json");
        if (jsonStart >= 0) {
            int contentStart = raw.indexOf('\n', jsonStart);
            if (contentStart > 0) {
                int jsonEnd = raw.indexOf("```", contentStart);
                if (jsonEnd > contentStart) {
                    String extracted = raw.substring(contentStart + 1, jsonEnd).trim();
                    try {
                        Map<String, Object> result = om.readValue(extracted, new TypeReference<>() {});
                        if (result.containsKey("type")) return result;
                    } catch (JsonProcessingException ignored) { log.debug("JSON代码块解析失败, 尝试花括号提取"); }
                }
            }
        }
        // 尝试提取最外层的 { }
        int braceStart = raw.indexOf('{');
        int braceEnd = raw.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            String extracted = raw.substring(braceStart, braceEnd + 1);
            try {
                Map<String, Object> result = om.readValue(extracted, new TypeReference<>() {});
                if (result.containsKey("type")) return result;
            } catch (JsonProcessingException ignored) { log.debug("JSON花括号提取失败, 结构化解析完全失败"); }
        }
        return null;
    }

    private void recordSessionPrompt(String sessionId, AgentType agentType, String subject) {
        if (agentSessionPromptMapper == null || promptTemplateCache == null) return;
        try {
            String templateName = agentType != null ? agentType.name().toLowerCase() : "agent_default";
            int version = promptTemplateCache.getTemplateVersion(templateName, subject);
            if (version == 0) return;
            AgentSessionPrompt record = new AgentSessionPrompt();
            record.setSessionId(sessionId);
            record.setTemplateName(templateName);
            record.setSubject(subject);
            record.setVersion(version);
            agentSessionPromptMapper.insert(record);
        } catch (Exception e) {
            log.warn("Failed to record session prompt version", e);
        }
    }

    private String buildMasteryContext(Long studentId) {
        try {
            LambdaQueryWrapper<PrecisionProgress> q = new LambdaQueryWrapper<PrecisionProgress>()
                    .eq(PrecisionProgress::getStudentId, studentId)
                    .orderByAsc(PrecisionProgress::getMasteryPercent)
                    .last("LIMIT 20");
            List<PrecisionProgress> list = precisionProgressMapper.selectList(q);
            if (list.isEmpty()) return null;

            StringBuilder sb = new StringBuilder("【学生学习掌握度数据】根据系统记录，当前学生的知识点掌握情况如下：\n");
            int weakCount = 0;
            for (PrecisionProgress p : list) {
                int mastery = p.getMasteryPercent() != null ? p.getMasteryPercent().intValue() : 0;
                String level;
                if (mastery < 30) { level = "很低，需要从基础开始"; weakCount++; }
                else if (mastery < 60) { level = "薄弱，建议重点讲解"; weakCount++; }
                else if (mastery < 85) { level = "中等，可以适当提高难度"; }
                else { level = "已掌握，可以出综合题"; }
                sb.append("- ").append(p.getSubject()).append("：掌握度").append(mastery)
                        .append("%（").append(level).append("）\n");
            }
            sb.append("\n辅导建议：对掌握度低于60%的知识点请用最基础的方式讲解，给出详细示例。");
            sb.append("对掌握度85%以上的知识点可以直接出综合题或进行知识拓展。");
            return sb.toString();
        } catch (Exception e) {
            log.warn("获取学生掌握度数据失败: studentId={}", studentId, e);
            return null;
        }
    }

    private void updateSessionSummary(String sessionId, String userMessage, List<Message> messages) {
        try {
            // 提取最后一次 assistant 回复
            String lastAssistant = null;
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message m = messages.get(i);
                if ("assistant".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                    lastAssistant = m.getContent();
                    break;
                }
            }
            if (lastAssistant == null) return;

            // 提取本轮使用的工具名
            List<String> toolsUsed = new ArrayList<>();
            for (Message m : messages) {
                if (m.getToolCalls() != null) {
                    for (Message.ToolCallRef tc : m.getToolCalls()) {
                        if (tc.getFunction() != null) {
                            toolsUsed.add(tc.getFunction().getName());
                        }
                    }
                }
            }

            // 构建结构化摘要（JSON 格式，方便下轮解析）
            String userBrief = userMessage.length() > 60 ? userMessage.substring(0, 60) + "…" : userMessage;
            String answerBrief = lastAssistant.length() > 200
                    ? lastAssistant.substring(0, 200).replace('\n', ' ') + "…"
                    : lastAssistant.replace('\n', ' ');

            StringBuilder summary = new StringBuilder();
            summary.append("{\"userAsk\":\"").append(escapeJson(userBrief)).append("\"");
            if (!toolsUsed.isEmpty()) {
                summary.append(",\"toolsUsed\":[");
                for (int i = 0; i < Math.min(toolsUsed.size(), 8); i++) {
                    if (i > 0) summary.append(",");
                    summary.append("\"").append(escapeJson(toolsUsed.get(i))).append("\"");
                }
                summary.append("]");
            }
            summary.append(",\"answerBrief\":\"").append(escapeJson(answerBrief)).append("\"}");

            sessionManager.setSessionSummary(sessionId, summary.toString());
            log.debug("updateSessionSummary: sessionId={}, tools={}", sessionId, toolsUsed.size());
        } catch (Exception e) {
            log.warn("生成会话摘要失败: sessionId={}", sessionId, e);
        }
    }

    /** 将会话元数据持久化到 MySQL（激活 agent_conversations 表） */
    private void persistConversation(String sessionId, UserContext caller,
                                     AgentType agentType, List<Message> messages) {
        try {
            int tokenCount = sessionManager.estimateTokens(messages);
            AgentConversation conv = new AgentConversation();
            conv.setSessionId(sessionId);
            conv.setUserId(caller.getUserId());
            conv.setAgentType(agentType.name());
            // C-7: 自动生成会话标题，从第一条用户消息中提取
            conv.setTitle(generateConversationTitle(messages));
            conv.setMessageCount(messages.size());
            conv.setTokenCount(tokenCount);

            // upsert: 先尝试插入，利用 uk_session 唯一索引保证原子性
            // 若 sessionId 已存在则更新 message_count 和 token_count
            try {
                agentConversationMapper.insert(conv);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                AgentConversation existing = agentConversationMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentConversation>()
                                .eq(AgentConversation::getSessionId, sessionId));
                if (existing != null) {
                    existing.setMessageCount(messages.size());
                    existing.setTokenCount(tokenCount);
                    existing.setTitle(conv.getTitle()); // 同步更新标题
                    agentConversationMapper.updateById(existing);
                }
            }

            // 同步 Redis 会话标题（会话列表从此读取）
            try {
                ConversationSession redisSession = sessionManager.load(sessionId);
                if (redisSession != null) {
                    redisSession.setTitle(conv.getTitle());
                    sessionManager.save(redisSession);
                }
            } catch (Exception e) {
                log.debug("同步 Redis 会话标题失败（非关键路径）: sessionId={}", sessionId, e);
            }
        } catch (Exception e) {
            log.debug("持久化会话元数据失败（非关键路径）: sessionId={}", sessionId, e);
        }
    }

    /** 从本轮对话中提取事实性记忆（关键词 + LLM 异步辅助提取） */
    private void extractFactsFromConversation(UserContext caller, List<Message> messages, String sessionId) {
        try {
            // 找最后一轮 user 和 assistant 消息
            String userMsg = null;
            String assistantMsg = null;
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message m = messages.get(i);
                if ("user".equals(m.getRole()) && m.getContent() != null && userMsg == null) {
                    userMsg = m.getContent();
                }
                if ("assistant".equals(m.getRole()) && m.getContent() != null && assistantMsg == null) {
                    assistantMsg = m.getContent();
                }
                if (userMsg != null && assistantMsg != null) break;
            }

            // 1. 关键词提取（现有逻辑）
            if (userMsg != null) {
                memoryService.extractFactsFromMessage(
                        caller.getUserId(), caller.getRoleName(), userMsg, sessionId);
            }

            // 2. LLM 辅助提取（异步，非阻塞）
            if (userMsg != null && assistantMsg != null) {
                final String fu = userMsg.length() > 500 ? userMsg.substring(0, 500) : userMsg;
                final String fa = assistantMsg.length() > 300 ? assistantMsg.substring(0, 300) : assistantMsg;
                CompletableFuture.runAsync(() -> {
                    try {
                        extractFactsWithLLM(caller, fu, fa, sessionId);
                    } catch (Exception e) {
                        log.debug("LLM事实提取失败（非关键路径）: sessionId={}", sessionId);
                    }
                });
            }
        } catch (Exception e) {
            log.debug("事实提取失败（非关键路径）: sessionId={}", sessionId, e);
        }
    }

    /** 用 LLM 从对话中提取职业/教学信息 */
    private void extractFactsWithLLM(UserContext caller, String userMessage,
                                     String assistantResponse, String sessionId) {
        String prompt = """
                从以下对话片段中提取用户的职业/教学相关信息。只输出JSON，不要其他文字。
                {
                  "teachingSubject": "用户所教学科名，如数学/英语/语文/信息技术/网络应用/办公应用，无法确定则null",
                  "isHeadTeacher": true 或 false 或 null,
                  "gradeLevel": "用户提到所教年级，如高一/高二/高三/一年级/二年级/三年级，无法确定则null",
                  "expressedNeed": "用户明确表达的教学或学习需求（20字以内），无法确定则null"
                }

                用户消息: %s
                AI回复摘要: %s
                """.formatted(userMessage, assistantResponse);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("prompt", prompt);
        params.put("temperature", 0.1);
        params.put("maxTokens", 200);
        params.put("_skipSystemPrompt", true);

        String result = gateway.generateContentQuiet(params);
        if (result == null || result.isBlank()) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = om.readValue(result, Map.class);
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                Object val = entry.getValue();
                if (val != null && !"null".equals(val) && !"".equals(val.toString().trim())) {
                    memoryService.storeLLMFact(caller.getUserId(), caller.getRoleName(),
                            entry.getKey(), val.toString(), sessionId);
                }
            }
        } catch (Exception e) {
            log.debug("解析LLM提取结果失败: sessionId={}", sessionId);
        }
    }

    /** 简单的 JSON 字符串转义 */
    private static String escapeJson(String s) {
        return AgentHelperUtils.escapeJson(s);
    }

    /** 判断输出文本是否可能包含 JSON 结构，避免无谓的 API 重格式化调用 */
    private static boolean looksLikeJson(String text) {
        return AgentHelperUtils.looksLikeJson(text);
    }

    /** 将结构化 JSON 摘要格式化为 LLM 可读的简短文本 */
    private String formatPrevSummary(String raw) {
        try {
            Map<String, Object> map = om.readValue(raw, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            if (map.get("userAsk") instanceof String q) {
                sb.append("上次提问：").append(q).append("\n");
            }
            if (map.get("toolsUsed") instanceof List<?> tools && !tools.isEmpty()) {
                sb.append("使用了工具：");
                for (int i = 0; i < Math.min(tools.size(), 5); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(tools.get(i));
                }
                sb.append("\n");
            }
            if (map.get("answerBrief") instanceof String a) {
                sb.append("回答概要：").append(a);
            }
            if (sb.isEmpty()) return raw;
            return sb.toString();
        } catch (JsonProcessingException e) {
            // 旧格式（非 JSON）直接返回
            return raw;
        }
    }

    /**
     * 新用户冷启动：首次使用时给 Agent 一个简单的方向引导。
     * 只在新用户无记忆、无历史会话时注入。
     */
    private String buildColdStartHint(AgentType agentType, UserContext caller) {
        if (agentType == AgentType.STUDY_BUDDY) {
            return "这是你与这位学生的第一次对话。请主动向学生介绍你自己（AI学伴），"
                    + "询问学生想学习什么内容、有没有遇到困难的知识点。"
                    + "如果学生不确定，可以建议从最近的错题或薄弱知识点开始。";
        }
        if (agentType == AgentType.LESSON_PREP) {
            return "这是你与这位教师的第一次对话。请主动向教师介绍你能提供的帮助："
                    + "知识检索、教学大纲查询、出题组卷、PPT课件生成、学情分析等。"
                    + "询问教师当前的教学需求和关注的班级/学科。";
        }
        if (agentType == AgentType.ANALYTICS) {
            return "这是你与这位教师的第一次对话。你可以帮助分析班级成绩、"
                    + "知识点掌握趋势、学生个体成长曲线。请主动询问想分析哪个班级和数据维度。";
        }
        return null;
    }

    /** C-7: 从消息列表中第一条用户消息自动生成会话标题 */
    private String generateConversationTitle(List<Message> messages) {
        for (Message m : messages) {
            if ("user".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                String content = m.getContent().replace('\n', ' ').replace('\r', ' ').trim();
                return content.length() > 20 ? content.substring(0, 20) + "…" : content;
            }
        }
        return "新对话";
    }

    /** G-5: 从用户消息中提取第一句作为原始目标 */
    private String extractGoal(String msg) {
        return AgentHelperUtils.extractGoal(msg);
    }

    /** G-5: 判断助手回复是否与原始目标相关（简单关键词重叠检测） */
    private boolean isOnTopic(String content, String goal) {
        return AgentHelperUtils.isOnTopic(content, goal);
    }

    /** G-1: 检测用户消息是否包含多个独立任务 */
    private boolean isMultiIntent(String msg) {
        return AgentHelperUtils.isMultiIntent(msg);
    }

    /** G-3: 输出内容自反思——常见错误模式检测，低成本规则引擎 */
    private String selfReflect(String content) {
        return AgentHelperUtils.selfReflect(content);
    }

    /** 清洗答案值用于比较：去标点、去序号前缀 */
    private String cleanAnswer(String raw) {
        return AgentHelperUtils.cleanAnswer(raw);
    }

    /** 判断两个答案是否等价：先尝试数值比较，再回退字符串比较 */
    private boolean answersEquivalent(String a, String b) {
        return AgentHelperUtils.answersEquivalent(a, b);
    }

    /**
     * 判断学段词汇是否出现在比较/对比/中职语境中（合理引用，不触发警告）。
     * 条件：该词前后 80 字符内出现"中职"或比较句式关键词。
     */
    private boolean isContextualComparison(String content, int pos, String term) {
        return AgentHelperUtils.isContextualComparison(content, pos, term);
    }

    /** G-4: 写操作工具白名单——有副作用（创建/删除/修改/通知）的工具名 */
    private static final Set<String> WRITE_TOOLS = Set.of(
            "teaching_create_task",
            "teaching_send_notification",
            "teaching_generate_ppt"
    );

    /** G-4: 判断是否为写操作工具 */
    private boolean isWriteTool(String toolName) {
        return toolName != null && WRITE_TOOLS.contains(toolName);
    }

    /** G-4: 阻塞等待用户确认写操作（最多等 60 秒） */
    private boolean waitForUserConfirm(String sessionId, Long userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingConfirmations.put(sessionId, future);
        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.info("用户确认超时: sessionId={}, userId={}", sessionId, userId);
            return false;
        } catch (Exception e) {
            log.warn("用户确认异常: sessionId={}", sessionId, e);
            return false;
        } finally {
            pendingConfirmations.remove(sessionId);
        }
    }

    /** G-4: 接收用户的确认/取消决定，由 AgentController.confirm 调用 */
    public void confirmWrite(String sessionId, Long userId, boolean confirmed) {
        CompletableFuture<Boolean> future = pendingConfirmations.get(sessionId);
        if (future == null) {
            log.warn("确认请求不存在或已过期: sessionId={}, userId={}", sessionId, userId);
            return;
        }
        future.complete(confirmed);
        log.info("用户确认写操作: sessionId={}, userId={}, confirmed={}", sessionId, userId, confirmed);
    }

    /** C-3: 工具名 → 中文进度描述，提升用户感知 */
    private String toolProgressDescription(String toolName) {
        return AgentHelperUtils.toolProgressDescription(toolName);
    }

    /** G-2: 单工具调用直接执行（避免并行开销） */
    private void executeSingleTool(ToolCall tc, UserContext caller, SseEmitter emitter,
                                    String sessionId, List<Message> messages) {
        long toolStart = System.currentTimeMillis();
        try {
            ToolResult result = toolRegistry.execute(tc.getName(), tc.getArguments(), caller, sessionId);
            long elapsed = System.currentTimeMillis() - toolStart;
            sendEvent(emitter, "tool_end", Map.of("tool", tc.getName(), "status", "ok"));
            messages.add(Message.toolResult(tc.getId(), safeWriteJson(result)));
            auditLogger.log(sessionId, caller, tc.getName(),
                    safeWriteJson(tc.getArguments()), true, null, elapsed,
                    result.isSuccess() ? "ok" : result.getError());
        } catch (ToolAccessDeniedException e) {
            sendEvent(emitter, "tool_end", Map.of("tool", tc.getName(), "status", "denied"));
            messages.add(Message.toolResult(tc.getId(), "{\"error\":\"" + e.getMessage() + "\"}"));
            auditLogger.log(sessionId, caller, tc.getName(),
                    safeWriteJson(tc.getArguments()), false, e.getMessage(),
                    System.currentTimeMillis() - toolStart, "denied");
        }
    }

    /** G-2: 多工具并行执行——降低多班对比/批量查询场景延迟 */
    private void executeToolsParallel(List<ToolCall> toolCalls, UserContext caller,
                                       SseEmitter emitter, String sessionId, List<Message> messages) {
        List<java.util.concurrent.CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            final int idx = i;
            final ToolCall tc = toolCalls.get(i);
            futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                long toolStart = System.currentTimeMillis();
                try {
                    ToolResult result = toolRegistry.execute(tc.getName(), tc.getArguments(), caller, sessionId);
                    long elapsed = System.currentTimeMillis() - toolStart;
                    return new ToolExecutionResult(idx, tc, result, null, elapsed);
                } catch (ToolAccessDeniedException e) {
                    return new ToolExecutionResult(idx, tc, null, e,
                            System.currentTimeMillis() - toolStart);
                } catch (Exception e) {
                    log.warn("工具执行异常: tool={}, sessionId={}", tc.getName(), sessionId, e);
                    return new ToolExecutionResult(idx, tc, null, e,
                            System.currentTimeMillis() - toolStart);
                }
            }, agentExecutor));
        }

        try {
            java.util.concurrent.CompletableFuture.allOf(
                    futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("并行工具执行超时或中断: sessionId={}", sessionId, e);
        }

        // 按原始顺序收集结果：超时未完成的工具生成错误结果
        List<ToolExecutionResult> results = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            java.util.concurrent.CompletableFuture<ToolExecutionResult> f = futures.get(i);
            ToolExecutionResult r = null;
            try {
                r = f.getNow(null);
            } catch (Exception ex) {
                log.warn("获取并行工具结果异常: tool={}", toolCalls.get(i).getName(), ex);
            }
            if (r == null) {
                // 超时或异常未捕获：生成超时错误结果，确保 LLM 收到 tool result
                r = new ToolExecutionResult(i, toolCalls.get(i), null,
                        new RuntimeException("工具执行超时（30s）"), 30000);
            }
            results.add(r);
        }

        for (ToolExecutionResult r : results) {
            if (r.error instanceof ToolAccessDeniedException) {
                sendEvent(emitter, "tool_end",
                        Map.of("tool", r.tc.getName(), "status", "denied"));
                messages.add(Message.toolResult(r.tc.getId(),
                        "{\"error\":\"" + r.error.getMessage() + "\"}"));
                auditLogger.log(sessionId, caller, r.tc.getName(),
                        safeWriteJson(r.tc.getArguments()), false, r.error.getMessage(),
                        r.elapsed, "denied");
            } else if (r.error != null) {
                sendEvent(emitter, "tool_end",
                        Map.of("tool", r.tc.getName(), "status", "error"));
                messages.add(Message.toolResult(r.tc.getId(),
                        "{\"error\":\"" + r.error.getMessage() + "\"}"));
                auditLogger.log(sessionId, caller, r.tc.getName(),
                        safeWriteJson(r.tc.getArguments()), false, r.error.getMessage(),
                        r.elapsed, "error");
            } else {
                sendEvent(emitter, "tool_end",
                        Map.of("tool", r.tc.getName(), "status", "ok"));
                messages.add(Message.toolResult(r.tc.getId(), safeWriteJson(r.result)));
                auditLogger.log(sessionId, caller, r.tc.getName(),
                        safeWriteJson(r.tc.getArguments()), true, null, r.elapsed,
                        r.result.isSuccess() ? "ok" : r.result.getError());
            }
        }
    }

    /** JSON 序列化安全包装：转换失败返回 "{}" */
    private String safeWriteJson(Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    /** G-2 并行工具执行结果容器 */
    @lombok.AllArgsConstructor
    private static class ToolExecutionResult {
        final int index;
        final ToolCall tc;
        final ToolResult result;
        final Exception error;
        final long elapsed;
    }

    /** 3.1: 检测 STUDY_BUDDY 输出是否直接泄露答案 */
    private String checkAnswerLeak(String content) {
        return AgentHelperUtils.checkAnswerLeak(content);
    }

    // PPT 生成（供 AgentController 直接调用，绕过 Agent 对话链路）
    public String generatePpt(String topic, String subject, String knowledgeContext) {
        return pptService.generate(topic, subject, knowledgeContext);
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    /** Vision OCR 调用（供 AgentController 复用） */
    public Map<String, Object> callVision(String imageBase64, String prompt) {
        return gateway.callVision(List.of(imageBase64), prompt, Map.of("temperature", 0.3));
    }

    @lombok.Data
    private static class DeepSeekStreamResponse {
        private List<ToolCall> toolCalls;
        private String content;
        private String reasoningContent;

        boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }

        boolean hasContent() {
            return content != null && !content.isEmpty();
        }

        static DeepSeekStreamResponse empty() {
            return new DeepSeekStreamResponse();
        }
    }
}
