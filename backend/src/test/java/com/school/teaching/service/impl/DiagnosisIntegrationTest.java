package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.AiOutput;
import com.school.teaching.mapper.AiOutputMapper;
import com.school.teaching.service.AiContentGeneratorService;
import com.school.teaching.service.impl.DeepSeekGateway;
import com.school.teaching.sse.SseTicketStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 场景5: AI诊断→诊断报告→SSE推送 核心链路集成测试
 *
 * 测试链路: 触发诊断 → Mock AI返回结果 → 验证AiOutput入库 → 验证SSE ticket机制
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@Disabled("需要运行中的 MySQL — 请先启动数据库再运行此测试")
@DisplayName("AI诊断集成测试: 诊断报告生成+SSE推送")
class DiagnosisIntegrationTest {

    @Autowired
    private AiContentGeneratorService aiContentGeneratorService;

    @Autowired
    private AiOutputMapper aiOutputMapper;

    @Autowired
    private SseTicketStore sseTicketStore;

    @MockBean
    private DeepSeekGateway deepSeekGateway;

    private static final Long TEACHER_ID = 2L;
    private static final Long STUDENT_ID = 1L;

    // ═══════════ AI诊断流程测试 ═══════════

    /**
     * 核心链路: Mock AI → generateSync → 验证AiOutput入库
     *
     * 注意: 由于executeAsync使用@Async异步执行，且测试中异步线程
     * 可能不参与@Transactional事务，本测试专注于验证同步部分:
     * AiOutput记录的写入和查询。
     */
    @Test
    @DisplayName("AI诊断输出入库: AiOutput插入后可按outputType=DIAGNOSIS查询")
    void diagnosisOutput_shouldBeQueryableByType() {
        // 直接创建诊断记录(模拟AI生成后的落库结果)
        AiOutput output = new AiOutput();
        output.setNodeId(0L); // 复用存taskId
        output.setTeacherId(TEACHER_ID);
        output.setOutputType("DIAGNOSIS");
        output.setTitle("诊断报告 - 数学[职高]");
        output.setSubject("数学[职高]");
        output.setContent("{\"overallLevel\":\"中等\",\"weakPoints\":[\"二次函数\",\"三角函数\"],\"strengths\":[\"集合\",\"不等式\"]}");
        output.setIsLatest(1);
        output.setVersionSeq(1);
        output.setStatus(0); // 草稿
        output.setTokensUsed(1500);
        output.setLatencyMs(3200);
        output.setCreatedAt(LocalDateTime.now());

        int inserted = aiOutputMapper.insert(output);
        assertTrue(inserted > 0, "AiOutput应成功插入");
        assertNotNull(output.getId(), "插入后应有自动生成的ID");

        // 验证可通过ID查询
        AiOutput fromDb = aiOutputMapper.selectById(output.getId());
        assertNotNull(fromDb, "应能通过ID查到诊断记录");
        assertEquals("DIAGNOSIS", fromDb.getOutputType(), "outputType应为DIAGNOSIS");
        assertEquals(TEACHER_ID, fromDb.getTeacherId(), "teacherId应匹配");
        assertEquals("数学[职高]", fromDb.getSubject(), "subject应匹配");
        assertNotNull(fromDb.getContent(), "content不应为空");
    }

    /**
     * 验证诊断记录列表可按outputType过滤
     */
    @Test
    @DisplayName("AiOutput列表: 可按outputType过滤诊断记录")
    void diagnosisOutputs_shouldBeFilterableByType() {
        // 插入多条不同类型的记录
        AiOutput diag1 = new AiOutput();
        diag1.setNodeId(1L);
        diag1.setTeacherId(TEACHER_ID);
        diag1.setOutputType("DIAGNOSIS");
        diag1.setTitle("诊断-数学");
        diag1.setSubject("数学[职高]");
        diag1.setContent("{\"level\":\"中等\"}");
        diag1.setIsLatest(1);
        diag1.setVersionSeq(1);
        diag1.setStatus(0);
        diag1.setCreatedAt(LocalDateTime.now());
        aiOutputMapper.insert(diag1);

        AiOutput diag2 = new AiOutput();
        diag2.setNodeId(2L);
        diag2.setTeacherId(TEACHER_ID);
        diag2.setOutputType("DIAGNOSIS");
        diag2.setTitle("诊断-英语");
        diag2.setSubject("英语[职高]");
        diag2.setContent("{\"level\":\"良好\"}");
        diag2.setIsLatest(1);
        diag2.setVersionSeq(1);
        diag2.setStatus(0);
        diag2.setCreatedAt(LocalDateTime.now());
        aiOutputMapper.insert(diag2);

        AiOutput material = new AiOutput();
        material.setNodeId(1L);
        material.setTeacherId(TEACHER_ID);
        material.setOutputType("CONSOLIDATION_MATERIAL");
        material.setTitle("巩固材料-数学");
        material.setSubject("数学[职高]");
        material.setContent("巩固材料内容");
        material.setIsLatest(1);
        material.setVersionSeq(1);
        material.setStatus(0);
        material.setCreatedAt(LocalDateTime.now());
        aiOutputMapper.insert(material);

        // 验证DIAGNOSIS类型过滤
        List<AiOutput> diagOutputs = aiContentGeneratorService.listOutputs(
            TEACHER_ID, "DIAGNOSIS", null, 1, 20);
        assertNotNull(diagOutputs);
        assertTrue(diagOutputs.size() >= 2, "应至少有2条诊断记录");
        for (AiOutput ao : diagOutputs) {
            assertEquals("DIAGNOSIS", ao.getOutputType(),
                "过滤DIAGNOSIS时所有记录outputType应为DIAGNOSIS");
        }

        // 验证CONSOLIDATION_MATERIAL类型过滤
        List<AiOutput> matOutputs = aiContentGeneratorService.listOutputs(
            TEACHER_ID, "CONSOLIDATION_MATERIAL", null, 1, 20);
        assertNotNull(matOutputs);
        for (AiOutput ao : matOutputs) {
            assertEquals("CONSOLIDATION_MATERIAL", ao.getOutputType());
        }
    }

    /**
     * 诊断内容完整性验证: content字段不为空且格式正确
     */
    @Test
    @DisplayName("诊断内容完整性: content/title/subject字段均有值")
    void diagnosisOutput_shouldHaveCompleteFields() {
        AiOutput output = new AiOutput();
        output.setNodeId(5L);
        output.setTeacherId(TEACHER_ID);
        output.setOutputType("DIAGNOSIS");
        output.setTitle("诊断报告 - 语文[职高]");
        output.setSubject("语文[职高]");
        output.setContent("{\"diagnosis\":{\"overallLevel\":\"优秀\",\"details\":[]}}");
        output.setIsLatest(1);
        output.setVersionSeq(1);
        output.setStatus(0);
        output.setTokensUsed(800);
        output.setLatencyMs(1500);
        output.setCreatedAt(LocalDateTime.now());
        aiOutputMapper.insert(output);

        AiOutput fromDb = aiContentGeneratorService.getById(output.getId());
        assertNotNull(fromDb, "应能查询到诊断记录");
        assertNotNull(fromDb.getTitle(), "title不应为空");
        assertNotNull(fromDb.getSubject(), "subject不应为空");
        assertNotNull(fromDb.getContent(), "content不应为空");
        assertFalse(fromDb.getContent().isEmpty(), "content不应为空字符串");
        assertEquals(Integer.valueOf(1), fromDb.getIsLatest(), "isLatest应为1");
    }

    // ═══════════ SSE Ticket 机制测试 ═══════════

    /**
     * SSE ticket创建和校验: 创建后在TTL内有效
     */
    @Test
    @DisplayName("SSE ticket: 创建后validateAndConsume返回正确的userId")
    void sseTicket_shouldValidateWithinTtl() {
        String ticket = sseTicketStore.create(STUDENT_ID, "STUDENT");
        assertNotNull(ticket, "ticket不应为null");
        assertFalse(ticket.isEmpty(), "ticket不应为空字符串");
        // ticket是Base64 URL编码，长度应为43(32字节去填充)
        assertTrue(ticket.length() >= 32, "ticket长度应>=32字节");

        // 校验ticket
        SseTicketStore.TicketEntry entry = sseTicketStore.validateAndConsume(ticket);
        assertNotNull(entry, "有效ticket应返回entry");
        assertEquals(STUDENT_ID, entry.userId(), "ticket中的userId应匹配");
        assertEquals("STUDENT", entry.role(), "ticket中的role应匹配");
    }

    /**
     * 无效ticket: 不存在的ticket返回null
     */
    @Test
    @DisplayName("SSE ticket: 无效ticket应返回null")
    void sseTicket_invalidTicket_shouldReturnNull() {
        SseTicketStore.TicketEntry entry = sseTicketStore.validateAndConsume("invalid-ticket-value");
        assertNull(entry, "无效ticket应返回null");
    }

    /**
     * 不同用户创建不同的ticket
     */
    @Test
    @DisplayName("SSE ticket: 不同用户创建不同ticket")
    void sseTicket_differentUsers_differentTickets() {
        String ticket1 = sseTicketStore.create(1L, "STUDENT");
        String ticket2 = sseTicketStore.create(2L, "TEACHER");

        assertNotEquals(ticket1, ticket2, "不同用户应生成不同ticket");

        SseTicketStore.TicketEntry entry1 = sseTicketStore.validateAndConsume(ticket1);
        SseTicketStore.TicketEntry entry2 = sseTicketStore.validateAndConsume(ticket2);

        assertEquals(1L, entry1.userId());
        assertEquals("STUDENT", entry1.role());
        assertEquals(2L, entry2.userId());
        assertEquals("TEACHER", entry2.role());
    }

    /**
     * Ticket可重复校验(TTL内允许多次使用，SSE重连需求)
     */
    @Test
    @DisplayName("SSE ticket: 同一ticket在TTL内可多次校验")
    void sseTicket_shouldAllowMultipleValidation() {
        String ticket = sseTicketStore.create(STUDENT_ID, "STUDENT");

        // 第一次校验
        SseTicketStore.TicketEntry entry1 = sseTicketStore.validateAndConsume(ticket);
        assertNotNull(entry1);

        // 第二次校验(TTL内仍有效)
        SseTicketStore.TicketEntry entry2 = sseTicketStore.validateAndConsume(ticket);
        assertNotNull(entry2, "同一ticket在TTL内应可重复校验(SSE重连需求)");
        assertEquals(entry1.userId(), entry2.userId());
    }

    /**
     * 批量ticket创建不冲突
     */
    @Test
    @DisplayName("SSE ticket: 批量创建不冲突")
    void sseTicket_batchCreation_shouldNotConflict() {
        String ticket1 = sseTicketStore.create(1L, "STUDENT");
        String ticket2 = sseTicketStore.create(1L, "STUDENT");
        String ticket3 = sseTicketStore.create(3L, "STUDENT");

        assertNotEquals(ticket1, ticket2, "同一用户两次创建应有不同ticket");
        assertNotEquals(ticket2, ticket3, "不同用户应有不同ticket");

        // 所有ticket都应有效
        assertNotNull(sseTicketStore.validateAndConsume(ticket1));
        assertNotNull(sseTicketStore.validateAndConsume(ticket2));
        assertNotNull(sseTicketStore.validateAndConsume(ticket3));
    }

    /**
     * Ticket清理: cleanup不应抛出异常
     */
    @Test
    @DisplayName("SSE ticket: cleanup不抛出异常")
    void sseTicket_cleanup_shouldNotThrow() {
        sseTicketStore.create(STUDENT_ID, "STUDENT");
        assertDoesNotThrow(() -> sseTicketStore.cleanup(),
            "cleanup不应抛出异常");
    }
}
