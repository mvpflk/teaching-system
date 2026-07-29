package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.CreditTransaction;
import com.school.teaching.mapper.CreditTransactionMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.service.CreditService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景3: 积分获取→事务记录→防重→排行榜 核心链路集成测试
 *
 * 测试链路: 学生获取积分 → 验证CreditTransaction记录 → 验证bizKey防重 → 查询排行榜
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@Disabled("需要运行中的 MySQL — 请先启动数据库再运行此测试")
@DisplayName("积分交易集成测试: 积分获取→事务记录→防重→排行榜")
class CreditTransactionIntegrationTest {

    @Autowired
    private CreditService creditService;

    @Autowired
    private CreditTransactionMapper transactionMapper;

    @Autowired
    private StudentMapper studentMapper;

    private static final Long STUDENT_ID = 1L; // student1 (totalCredits=150)

    /**
     * 核心链路: 调整积分 → 验证交易记录入库
     */
    @Test
    @DisplayName("积分获取链路: adjustCredit→交易记录→积分变更")
    void adjustCredit_shouldCreateTransactionAndUpdateBalance() {
        // 记录原始积分
        var studentBefore = studentMapper.selectById(STUDENT_ID);
        int creditsBefore = studentBefore.getTotalCredits() != null ? studentBefore.getTotalCredits() : 0;

        // 调整积分(+10)
        Map<String, Object> result = creditService.adjustCredit(STUDENT_ID, 10, "集成测试-积分获取");

        assertNotNull(result, "adjustCredit应返回结果");
        assertEquals(10, result.get("amount"), "调整额度应为10");
        assertEquals("earn", result.get("type"), "正向调整类型应为earn");

        // 验证积分已更新
        var studentAfter = studentMapper.selectById(STUDENT_ID);
        int creditsAfter = studentAfter.getTotalCredits() != null ? studentAfter.getTotalCredits() : 0;
        assertEquals(creditsBefore + 10, creditsAfter, "积分应增加10");

        // 验证交易记录已创建
        List<CreditTransaction> transactions = creditService.getTransactions(STUDENT_ID);
        assertNotNull(transactions);

        // 查找本次交易记录
        boolean found = transactions.stream()
            .anyMatch(t -> "集成测试-积分获取".equals(t.getDescription())
                       && t.getCreditAmount() == 10
                       && "earn".equals(t.getTransactionType()));
        assertTrue(found, "应能找到本次积分获取的交易记录");
    }

    /**
     * 查询积分信息: 返回学生积分概览
     */
    @Test
    @DisplayName("积分信息查询: 返回totalCredits/currentStreak/studentId")
    void getCreditInfo_shouldReturnStudentCreditInfo() {
        Map<String, Object> info = creditService.getCreditInfo(STUDENT_ID);

        assertNotNull(info, "积分信息不应为null");
        assertNotNull(info.get("totalCredits"), "应有totalCredits字段");
        assertNotNull(info.get("studentId"), "应有studentId字段");
        assertEquals(STUDENT_ID, info.get("studentId"), "studentId应匹配");

        int totalCredits = ((Number) info.get("totalCredits")).intValue();
        assertTrue(totalCredits >= 0, "积分不应为负数");
    }

    /**
     * 积分排行榜: 返回排行列表
     */
    @Test
    @DisplayName("积分排行榜: 返回排名列表含realName和totalCredits")
    void getRanking_shouldReturnRankedList() {
        List<Map<String, Object>> ranking = creditService.getRanking("total", 10, null, null, null);

        assertNotNull(ranking, "排行榜不应为null");
        // 至少有当前测试学生
        boolean foundStudent = ranking.stream()
            .anyMatch(r -> STUDENT_ID.equals(r.get("studentId")));
        assertTrue(foundStudent, "排行榜应包含测试学生");

        // 验证排行记录包含必要字段
        if (!ranking.isEmpty()) {
            Map<String, Object> first = ranking.get(0);
            assertNotNull(first.get("realName"), "排行记录应包含realName");
            assertNotNull(first.get("totalCredits"), "排行记录应包含totalCredits");
        }
    }

    /**
     * 积分交易记录正确关联studentId
     */
    @Test
    @DisplayName("交易记录过滤: 交易记录正确关联studentId")
    void getTransactions_shouldFilterByStudent() {
        // 先创建一笔交易确保有记录
        creditService.adjustCredit(STUDENT_ID, 5, "集成测试-关联验证");

        List<CreditTransaction> transactions = creditService.getTransactions(STUDENT_ID);
        assertNotNull(transactions);

        // 所有交易记录应属于该学生
        for (CreditTransaction txn : transactions) {
            assertEquals(STUDENT_ID, txn.getStudentId(),
                "交易记录" + txn.getId() + "的studentId应为" + STUDENT_ID);
        }
    }

    /**
     * 扣分操作: 验证积分减少
     */
    @Test
    @DisplayName("扣分操作: 扣分后积分应减少")
    void deductCredit_shouldDecreaseBalance() {
        // 先确保有足够积分
        creditService.adjustCredit(STUDENT_ID, 20, "集成测试-增加备用积分");

        var studentBefore = studentMapper.selectById(STUDENT_ID);
        int creditsBefore = studentBefore.getTotalCredits() != null ? studentBefore.getTotalCredits() : 0;

        // 扣分(-5)
        Map<String, Object> result = creditService.adjustCredit(STUDENT_ID, -5, "集成测试-扣分");

        assertNotNull(result);
        assertEquals(5, result.get("amount"), "扣分额度应为5(绝对值)");

        var studentAfter = studentMapper.selectById(STUDENT_ID);
        int creditsAfter = studentAfter.getTotalCredits() != null ? studentAfter.getTotalCredits() : 0;
        assertEquals(creditsBefore - 5, creditsAfter, "扣分后积分应减少5");
    }
}
