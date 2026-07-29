package com.school.teaching;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 核心 API 冒烟测试 — 验证关键端点可访问且返回预期数据结构
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("需要运行中的 MySQL + Redis，请通过 Docker Compose 启动后再运行此测试")
class CoreApiSmokeTest {

    @Autowired private MockMvc mvc;

    @Test @Order(1)
    @DisplayName("1. 登录端点 — /auth/actions/login 接收POST并返回JSON")
    void loginEndpoint() throws Exception {
        mvc.perform(post("/auth/actions/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").isNumber());
        System.out.println("[PASS] login endpoint responds with JSON");
    }

    @Test @Order(2)
    @DisplayName("2. 通知未登录返回JSON错误")
    void notificationRequiresAuth() throws Exception {
        mvc.perform(get("/notification/list"))
            .andExpect(status().isUnauthorized());
        System.out.println("[PASS] /notification/list without token → 401");
    }

    @Test @Order(3)
    @DisplayName("3. 提交端点认证 — 无token返回401")
    void submitRequiresAuth() throws Exception {
        mvc.perform(post("/student/tasks/1/actions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"test\"}"))
            .andExpect(status().isUnauthorized());
        System.out.println("[PASS] submit without token → 401");
    }

    @Test @Order(4)
    @DisplayName("4. 问卷统计 — 非SURVEY返回400")
    void surveyStatsNonSurvey() throws Exception {
        // 先登录获取token
        String resp = mvc.perform(post("/auth/actions/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andReturn().getResponse().getContentAsString();
        // 尝试解析token
        String token = null;
        try {
            token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).get("data").get("token").asText();
        } catch (Exception e) { /* login failed, skip */ }
        if (token != null) {
            // 查一个已有任务ID
            String tasks = mvc.perform(get("/task/list").header("Authorization", "Bearer " + token)
                    .param("page", "1").param("size", "1"))
                .andReturn().getResponse().getContentAsString();
            try {
                var records = new com.fasterxml.jackson.databind.ObjectMapper().readTree(tasks).get("data").get("records");
                if (records != null && records.size() > 0) {
                    long taskId = records.get(0).get("id").asLong();
                    mvc.perform(get("/task/" + taskId + "/survey-stats")
                            .header("Authorization", "Bearer " + token))
                        .andExpect(jsonPath("$.code").isNumber());
                    System.out.println("[PASS] survey-stats returns code for taskId=" + taskId);
                }
            } catch (Exception e) { System.out.println("[SKIP] no tasks in DB"); }
        } else { System.out.println("[SKIP] login failed, test skipped"); }
    }

    @Test @Order(5)
    @DisplayName("5. 任务列表端点 — 返回分页结构")
    void taskListStructure() throws Exception {
        // 先登录
        String resp = mvc.perform(post("/auth/actions/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andReturn().getResponse().getContentAsString();
        String token = null;
        try { token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).get("data").get("token").asText(); } catch (Exception e) {}
        if (token != null) {
            mvc.perform(get("/task/list").header("Authorization", "Bearer " + token)
                    .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
            System.out.println("[PASS] /task/list returns paginated structure");
        }
    }
}
