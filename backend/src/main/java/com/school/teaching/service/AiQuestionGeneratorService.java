package com.school.teaching.service;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.metrics.AiMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionGeneratorService {

    private final AiServiceGateway aiGateway;
    private final AiCallLogMapper aiCallLogMapper;
    private final AiMetricsService aiMetrics;
    private final KnowledgeNodeMapper nodeMapper;
    private final AiTaskStore taskStore;

    @Lazy
    @Autowired
    private AiQuestionGeneratorService self;

    @Value("${ai.daily-quota:25}") private int dailyQuota;

    @Autowired private SystemService systemService;
    @Autowired private com.school.teaching.service.impl.QuestionSaveService questionSaveService;
    @Autowired private com.school.teaching.service.impl.QuestionReviewService questionReviewService;
    @Autowired private com.school.teaching.service.impl.RemedialExerciseService remedialExerciseService;
    @Autowired private com.school.teaching.service.impl.CategoryPathInjector categoryPathInjector;

    /** 同步生成题目（内部调用者使用），通过任务存储 + 阻塞等待实现，避免直接占用线程 */
    public List<Map<String, Object>> generateSync(Long teacherId, Map<String, Object> params) {
        try {
            checkQuota(teacherId);
            injectCategoryPath(params);
            AiCallLog log_ = new AiCallLog();
            log_.setUserId(teacherId);
            log_.setCapability("QUESTION_GEN");
            log_.setCreatedAt(LocalDateTime.now());
            long start = System.currentTimeMillis();
            List<Map<String, Object>> questions = aiGateway.generateQuestions(params);
            long elapsed = System.currentTimeMillis() - start;
            log_.setLatencyMs((int) elapsed);
            log_.setTokensUsed(params.get("tokens_used") instanceof Number n ? n.intValue() : 0);
            aiCallLogMapper.insert(log_);
            return self.saveQuestions(teacherId, params, questions);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "AI 出题失败: " + e.getMessage());
        }
    }

    /** 异步生成题目，结果写入 AiTaskStore */
    public List<Map<String, Object>> generateFromAiAssistant(Long teacherId, Map<String, Object> extraParams) {
        AiCallLog log_ = new AiCallLog();
        log_.setUserId(teacherId);
        log_.setCapability("QUESTION_GEN");
        log_.setCreatedAt(LocalDateTime.now());
        long start = System.currentTimeMillis();
        List<Map<String, Object>> questions = aiGateway.generateQuestions(extraParams);
        long elapsed = System.currentTimeMillis() - start;
        log_.setLatencyMs((int) elapsed);
        log_.setTokensUsed(extraParams.get("_tokensUsed") instanceof Number n ? n.intValue() : 0);
        aiCallLogMapper.insert(log_);
        return self.saveQuestions(teacherId, extraParams, questions);
    }

    /** 批量入库 AI 生成的题目 */
    @Transactional
    public List<Map<String, Object>> saveQuestions(Long teacherId, Map<String, Object> params, List<Map<String, Object>> questions) {
        return questionSaveService.saveQuestions(teacherId, params, questions);
    }

    /** 衍生练习：薄弱分析→AI生成针对性练习题 */
    public Map<String, Object> generateRemedial(Long userId, List<Integer> knowledgeNodeIds, String subject) {
        return remedialExerciseService.generateRemedial(userId, knowledgeNodeIds, subject);
    }

    /** 教师审核通过题目 */
    @Transactional
    public void approve(Long questionId) {
        questionReviewService.approve(questionId);
    }

    /** 教师拒绝题目（软删除） */
    @Transactional
    public void reject(Long questionId) {
        questionReviewService.reject(questionId);
    }

    /** 获取 AI_DRAFT 题目列表 */
    public List<QuestionBank> listDrafts(Long teacherId, Long schoolId) {
        return questionReviewService.listDrafts(teacherId, schoolId);
    }

    public void injectCategoryPath(Map<String, Object> params) {
        categoryPathInjector.injectCategoryPath(params);
    }

    public void checkQuota(Long teacherId) {
        categoryPathInjector.checkQuota(teacherId, dailyQuota);
    }

}
