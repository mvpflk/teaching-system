package com.school.teaching.agent.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.school.teaching.agent.config.AgentConfig;
import com.school.teaching.agent.loop.AgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SessionManager {

    private static final String SESSION_PREFIX = "agent:session:";
    private static final String USER_SESSIONS_PREFIX = "agent:user:sessions:";

    private final StringRedisTemplate redis;
    private final ObjectMapper om;
    private final AgentConfig agentConfig;
    private final ConcurrentHashMap<String, String> memoryStore;  // 本地开发内存模式

    public SessionManager(@Autowired(required = false) StringRedisTemplate redis,
                          AgentConfig agentConfig) {
        this.redis = redis;
        this.agentConfig = agentConfig;
        this.om = new ObjectMapper();
        this.om.registerModule(new JavaTimeModule());
        this.memoryStore = redis == null ? new ConcurrentHashMap<>() : null;
        if (redis == null) {
            log.warn("Redis 不可用，SessionManager 使用内存模式（重启后会话丢失，仅限本地开发）");
        }
    }

    public ConversationSession create(Long userId, String roleName, AgentType agentType) {
        ConversationSession session = ConversationSession.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .userRole(roleName)
                .agentType(agentType)
                .title("新对话")
                .messages(new ArrayList<>())
                .tokenCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        save(session);
        addToUserList(userId, session.getId());
        return session;
    }

    public ConversationSession load(String sessionId) {
        try {
            String json = redis != null
                    ? redis.opsForValue().get(SESSION_PREFIX + sessionId)
                    : memoryStore.get(SESSION_PREFIX + sessionId);
            if (json == null) return null;
            return om.readValue(json, ConversationSession.class);
        } catch (JsonProcessingException e) {
            log.error("加载会话失败: sessionId={}", sessionId, e);
            return null;
        }
    }

    public void save(ConversationSession session) {
        try {
            session.setUpdatedAt(LocalDateTime.now());
            session.setTokenCount(estimateTokens(session.getMessages()));
            String json = om.writeValueAsString(session);
            String key = SESSION_PREFIX + session.getId();
            if (redis != null) {
                redis.opsForValue().set(key, json, agentConfig.getSessionTtlDays(), TimeUnit.DAYS);
            } else {
                memoryStore.put(key, json);
            }
        } catch (JsonProcessingException e) {
            log.error("保存会话失败: sessionId={}", session.getId(), e);
        }
    }

    public void saveMessages(String sessionId, List<Message> messages) {
        ConversationSession session = load(sessionId);
        if (session != null) {
            session.setMessages(messages);
            save(session);
        }
    }

    public List<Message> loadMessages(String sessionId) {
        ConversationSession session = load(sessionId);
        return session != null ? session.getMessages() : new ArrayList<>();
    }

    public void delete(String sessionId, Long userId) {
        String key = SESSION_PREFIX + sessionId;
        String userKey = USER_SESSIONS_PREFIX + userId;
        if (redis != null) {
            redis.delete(key);
            redis.opsForSet().remove(userKey, sessionId);
        } else {
            memoryStore.remove(key);
        }
    }

    public List<String> getUserSessionIds(Long userId) {
        String userKey = USER_SESSIONS_PREFIX + userId;
        if (redis != null) {
            return new ArrayList<>(redis.opsForSet().members(userKey));
        } else {
            return Collections.emptyList();
        }
    }

    public String getLastSessionSummary(Long userId) {
        List<String> ids = getUserSessionIds(userId);
        if (ids.isEmpty()) return null;
        ConversationSession latest = null;
        for (String id : ids) {
            ConversationSession s = load(id);
            if (s != null && (latest == null || s.getUpdatedAt().isAfter(latest.getUpdatedAt()))) {
                latest = s;
            }
        }
        return latest != null ? latest.getSummary() : null;
    }

    public void setSessionSummary(String sessionId, String summary) {
        ConversationSession session = load(sessionId);
        if (session != null) {
            session.setSummary(summary);
            save(session);
        }
    }

    public int estimateTokens(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return 0;
        int count = 0;
        for (Message m : messages) {
            if (m.getContent() != null) {
                count += estimateStringTokens(m.getContent());
            }
            if (m.getToolCalls() != null) {
                for (Message.ToolCallRef tc : m.getToolCalls()) {
                    if (tc.getFunction() != null && tc.getFunction().getArguments() != null) {
                        count += tc.getFunction().getArguments().length() / 2;
                    }
                }
            }
        }
        return count;
    }

    /** 改进的 token 估算：中文字符 ≈0.7 token，英文 ≈0.3 token/char */
    private int estimateStringTokens(String s) {
        int cjk = 0, ascii = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || (c >= 0x3000 && c <= 0x303F)  // CJK 标点
                    || (c >= 0xFF00 && c <= 0xFFEF)) { // 全角字符
                cjk++;
            } else if (c < 128) {
                ascii++;
            }
        }
        return (int) (cjk * 0.7 + ascii * 0.3 + (s.length() - cjk - ascii) * 0.5);
    }

    public List<Message> compressHistory(List<Message> messages) {
        if (messages == null || messages.size() <= 6) {
            return messages;
        }
        // 渐进式压缩：保留更多上下文（10条 + 摘要），减少信息丢失
        int keep = Math.min(10, messages.size());
        List<Message> dropped = messages.subList(1, messages.size() - keep);
        String summary = buildDropSummary(dropped);

        List<Message> compressed = new ArrayList<>();
        compressed.add(messages.get(0));
        compressed.add(Message.system(summary));
        for (int i = messages.size() - keep; i < messages.size(); i++) {
            compressed.add(messages.get(i));
        }
        log.info("会话压缩: 原始{}条 → 压缩后{}条, 摘要长度{}字", messages.size(), compressed.size(), summary.length());
        return compressed;
    }

    /** 从被丢弃的消息中提取关键信息，生成保留数据的简短摘要 */
    private String buildDropSummary(List<Message> dropped) {
        int userMsgs = 0, assistantMsgs = 0, toolResults = 0;
        StringBuilder tools = new StringBuilder();
        Set<String> classes = new LinkedHashSet<>();
        Set<String> knowledgePoints = new LinkedHashSet<>();
        StringBuilder scores = new StringBuilder();
        Set<String> tasks = new LinkedHashSet<>();
        // 保留最后一条 assistant 的推理内容（作为上下文衔接）
        String lastAssistantReasoning = null;

        for (Message m : dropped) {
            if ("user".equals(m.getRole())) userMsgs++;
            else if ("assistant".equals(m.getRole())) {
                assistantMsgs++;
                if (m.getContent() != null && !m.getContent().isBlank()) {
                    lastAssistantReasoning = m.getContent();
                }
            }
            else if ("tool".equals(m.getRole())) {
                toolResults++;
                extractKeyData(m.getContent(), classes, knowledgePoints, scores, tasks);
            }
        }
        for (Message m : dropped) {
            if (m.getToolCalls() != null) {
                for (Message.ToolCallRef tc : m.getToolCalls()) {
                    if (tc.getFunction() != null && tools.indexOf(tc.getFunction().getName()) < 0) {
                        if (tools.length() > 0) tools.append("、");
                        tools.append(tc.getFunction().getName());
                    }
                }
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("【对话摘要】此前已进行 ").append(userMsgs).append(" 轮对话，")
                .append("Assistant 回复 ").append(assistantMsgs).append(" 次，工具调用 ").append(toolResults).append(" 次");
        if (tools.length() > 0) summary.append("（使用了：").append(tools).append("）");
        if (!classes.isEmpty()) summary.append("。涉及的班级：").append(String.join("、", classes));
        if (!knowledgePoints.isEmpty()) summary.append("。涉及的知识点：").append(String.join("、", knowledgePoints));
        if (!tasks.isEmpty()) summary.append("。涉及的任务：").append(String.join("、", tasks));
        if (scores.length() > 0) summary.append("。成绩数据：").append(scores);
        // 保留上一步推理上下文，确保 Agent 不"失忆"
        if (lastAssistantReasoning != null && lastAssistantReasoning.length() > 120) {
            summary.append("。上一步分析：").append(lastAssistantReasoning, 0, Math.min(200, lastAssistantReasoning.length()));
            if (lastAssistantReasoning.length() > 200) summary.append("…");
        } else if (lastAssistantReasoning != null) {
            summary.append("。上一步分析：").append(lastAssistantReasoning);
        }
        summary.append("。请基于以上数据和最近的消息继续回答，无需重复之前的工具调用。");
        return summary.toString();
    }

    /**
     * 从工具结果 JSON 中提取关键数据（班级、知识点、成绩、任务ID）。
     * 仅提取"标识符"级别的信息，不保留完整数据以控制 token 消耗。
     */
    private void extractKeyData(String toolResultJson, Set<String> classes,
                                 Set<String> knowledgePoints, StringBuilder scores,
                                 Set<String> tasks) {
        if (toolResultJson == null || toolResultJson.isBlank()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = om.readValue(toolResultJson, Map.class);
            // 班级名称/ID
            extractFromJson(result, "className", classes);
            extractFromJson(result, "classId", classes);
            // 知识点
            extractFromJsonArr(result, "nodes", "name", knowledgePoints);
            extractFromJson(result, "nodeName", knowledgePoints);
            extractFromJson(result, "knowledgePoint", knowledgePoints);
            // 成绩摘要
            if (result.containsKey("averageScore") || result.containsKey("avgScore")) {
                Object avg = result.getOrDefault("averageScore", result.get("avgScore"));
                if (avg != null && scores.length() > 0) scores.append("；");
                scores.append("均分").append(avg);
            }
            if (result.containsKey("passRate")) {
                scores.append("，通过率").append(result.get("passRate"));
            }
            // 任务ID
            extractFromJson(result, "taskId", tasks);
            extractFromJson(result, "taskName", tasks);
        } catch (Exception ignored) {
            // 解析失败不阻塞压缩流程
        }
    }

    @SuppressWarnings("unchecked")
    private void extractFromJson(Map<String, Object> map, String key, Set<String> target) {
        Object val = map.get(key);
        if (val instanceof String s && !s.isBlank() && target.size() < 10) target.add(s);
        else if (val instanceof Number && target.size() < 10) target.add(val.toString());
    }

    @SuppressWarnings("unchecked")
    private void extractFromJsonArr(Map<String, Object> map, String arrKey, String field, Set<String> target) {
        Object arr = map.get(arrKey);
        if (arr instanceof List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (item instanceof Map<?,?> m) {
                    Object val = m.get(field);
                    if (val instanceof String s && !s.isBlank() && target.size() < 10) target.add(s);
                }
            }
        }
    }

    private void addToUserList(Long userId, String sessionId) {
        String userKey = USER_SESSIONS_PREFIX + userId;
        if (redis != null) {
            redis.opsForSet().add(userKey, sessionId);
            redis.expire(userKey, agentConfig.getSessionTtlDays(), TimeUnit.DAYS);
        }
        // 内存模式: 会话列表暂不维护（重启丢失），不影响功能
    }
}
