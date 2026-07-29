package com.school.teaching.agent.session;

import com.school.teaching.agent.config.AgentConfig;
import com.school.teaching.agent.loop.AgentType;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionManager 单元测试 — 使用内存模式（不依赖 Redis）。
 */
class SessionManagerTest {

    private SessionManager manager;
    private AgentConfig agentConfig;

    @BeforeEach
    void setUp() {
        agentConfig = new AgentConfig();
        agentConfig.setSessionTtlDays(7);
        // null StringRedisTemplate → 自动切换内存模式
        manager = new SessionManager(null, agentConfig);
    }

    // ═══════════ 创建会话 ═══════════

    @Test
    @DisplayName("create: 创建会话返回非空 ID + 正确元数据")
    void createReturnsValidSession() {
        ConversationSession session = manager.create(10L, "TEACHER", AgentType.LESSON_PREP);

        assertNotNull(session.getId());
        assertEquals(10L, session.getUserId());
        assertEquals("TEACHER", session.getUserRole());
        assertEquals(AgentType.LESSON_PREP, session.getAgentType());
        assertEquals("新对话", session.getTitle());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getUpdatedAt());
        assertEquals(0, session.getTokenCount());
    }

    @Test
    @DisplayName("create: 不同会话 ID 不重复")
    void createGeneratesUniqueIds() {
        ConversationSession s1 = manager.create(1L, "STUDENT", AgentType.STUDY_BUDDY);
        ConversationSession s2 = manager.create(1L, "STUDENT", AgentType.STUDY_BUDDY);
        assertNotEquals(s1.getId(), s2.getId());
    }

    // ═══════════ 存取 ═══════════

    @Test
    @DisplayName("save → load: 保存后读出数据一致")
    void saveAndLoadRoundtrip() {
        ConversationSession session = manager.create(10L, "TEACHER", AgentType.LESSON_PREP);
        session.setTitle("高二3班计算机基础备课");
        session.setMessages(List.of(
                Message.user("帮我备第一章的课"),
                Message.assistant("好的，我来帮您准备...")
        ));
        manager.save(session);

        ConversationSession loaded = manager.load(session.getId());
        assertNotNull(loaded);
        assertEquals("高二3班计算机基础备课", loaded.getTitle());
        assertEquals(2, loaded.getMessages().size());
        assertEquals("user", loaded.getMessages().get(0).getRole());
        assertEquals("assistant", loaded.getMessages().get(1).getRole());
    }

    @Test
    @DisplayName("load: 不存在的会话返回 null")
    void loadNonExistentReturnsNull() {
        assertNull(manager.load("nonexistent-id"));
    }

    // ═══════════ 消息存取 ═══════════

    @Test
    @DisplayName("loadMessages: 空会话返回空列表")
    void loadMessagesEmptySession() {
        ConversationSession session = manager.create(1L, "STUDENT", AgentType.STUDY_BUDDY);
        List<Message> msgs = manager.loadMessages(session.getId());
        assertTrue(msgs.isEmpty());
    }

    @Test
    @DisplayName("saveMessages: 更新消息后读出正确")
    void saveMessagesUpdatesCorrectly() {
        ConversationSession session = manager.create(1L, "STUDENT", AgentType.STUDY_BUDDY);

        List<Message> msgs = List.of(
                Message.user("这道题怎么做"),
                Message.assistant("我们一步步来分析...")
        );
        manager.saveMessages(session.getId(), msgs);

        List<Message> loaded = manager.loadMessages(session.getId());
        assertEquals(2, loaded.size());
    }

    // ═══════════ 删除 ═══════════

    @Test
    @DisplayName("delete: 删除后 load 返回 null")
    void deleteRemovesSession() {
        ConversationSession session = manager.create(10L, "TEACHER", AgentType.LESSON_PREP);
        String id = session.getId();

        manager.delete(id, 10L);
        assertNull(manager.load(id));
    }

    // ═══════════ Token 估算 ═══════════

    @Test
    @DisplayName("estimateTokens: 空消息列表返回 0")
    void estimateTokensEmpty() {
        assertEquals(0, manager.estimateTokens(null));
        assertEquals(0, manager.estimateTokens(List.of()));
    }

    @Test
    @DisplayName("estimateTokens: 按 cjk*0.7 + ascii*0.3 估算")
    void estimateTokensApproximate() {
        List<Message> msgs = List.of(
                Message.user("你好"),              // 2 CJK → 2*0.7=1.4 → 1
                Message.assistant("你好，请问有什么可以帮助你的？")  // 15 CJK → 15*0.7=10.5 → 10
        );
        int tokens = manager.estimateTokens(msgs);
        // 2*0.7=1.4→1 + 15*0.7=10.5→10 = 11
        assertEquals(11, tokens);
    }

    // ═══════════ 历史压缩 ═══════════

    @Test
    @DisplayName("compressHistory: ≤4 条消息不压缩，原样返回")
    void compressHistoryShortConversationUnchanged() {
        List<Message> msgs = List.of(
                Message.system("你是一个助手"),
                Message.user("问题1"),
                Message.assistant("回答1"),
                Message.user("问题2")
        );
        List<Message> compressed = manager.compressHistory(msgs);
        assertEquals(4, compressed.size());
    }

    @Test
    @DisplayName("compressHistory: >4 条时保留首条 + 摘要 + 最后6条")
    void compressHistoryLongConversationCompressed() {
        List<Message> msgs = new java.util.ArrayList<>();
        msgs.add(Message.system("你是一个助手"));
        for (int i = 0; i < 15; i++) {
            msgs.add(Message.user("问题" + i));
            msgs.add(Message.assistant("回答" + i));
        }
        // 共 1 + 30 = 31 条

        List<Message> compressed = manager.compressHistory(msgs);

        // 保留: 首条 + 摘要 system 消息 + 最后 min(6, 31) = 6 条 = 1 + 1 + 6 = 8 条
        assertTrue(compressed.size() < msgs.size(), "压缩后应该变短");
        assertEquals(msgs.get(0), compressed.get(0), "首条保留");
        assertEquals("system", compressed.get(1).getRole(), "第二条是摘要 system 消息");
    }

    @Test
    @DisplayName("compressHistory: null 输入安全")
    void compressHistoryNullSafe() {
        assertNull(manager.compressHistory(null));
    }

    // ═══════════ AgentType 枚举 ═══════════

    @Test
    @DisplayName("AgentType 三个枚举值正确")
    void agentTypeValues() {
        assertEquals(3, AgentType.values().length);
        assertNotNull(AgentType.valueOf("LESSON_PREP"));
        assertNotNull(AgentType.valueOf("STUDY_BUDDY"));
        assertNotNull(AgentType.valueOf("ANALYTICS"));
    }
}
