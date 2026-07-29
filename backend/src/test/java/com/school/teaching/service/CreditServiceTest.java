package com.school.teaching.service;

import com.school.teaching.entity.CreditTransaction;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Disabled("需要运行中的 MySQL，请通过 Docker Compose 启动后再运行此测试")
class CreditServiceTest {

    @Autowired
    private CreditService creditService;

    @Test
    void getCreditInfo_shouldReturnStudentInfo() {
        Map<String, Object> info = creditService.getCreditInfo(1L);
        assertNotNull(info);
        assertEquals(150, info.get("totalCredits"));
        assertEquals(3, info.get("currentStreak"));
        assertEquals(1L, info.get("studentId"));
    }

    @Test
    void getTransactions_shouldReturnList() {
        List<CreditTransaction> txns = creditService.getTransactions(1L);
        assertNotNull(txns);
        assertTrue(txns.size() >= 0);
    }

    @Test
    void getRanking_shouldReturnRankedStudents() {
        List<Map<String, Object>> ranking = creditService.getRanking("total", 10, null, null, null);
        assertNotNull(ranking);
        assertTrue(ranking.size() >= 1);
        Map<String, Object> first = ranking.get(0);
        assertNotNull(first.get("realName"));
        assertNotNull(first.get("totalCredits"));
    }

    @Test
    void getTransactions_shouldFilterByStudentId() {
        List<CreditTransaction> txns = creditService.getTransactions(1L);
        for (CreditTransaction t : txns) {
            assertEquals(1L, t.getStudentId());
        }
    }
}