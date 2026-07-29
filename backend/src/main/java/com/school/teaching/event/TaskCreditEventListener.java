package com.school.teaching.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.common.CreditCalculatorSelector;
import com.school.teaching.common.SchoolContext;
import com.school.teaching.common.ScoreCalculator;
import com.school.teaching.common.ScoreCalculatorSelector;
import com.school.teaching.common.ScoreType;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCreditEventListener {

    private final CreditTransactionMapper creditMapper;
    private final StudentMapper studentMapper;
    private final TaskMapper taskMapper;
    private final ScoreCalculatorSelector calculatorSelector;
    private final CreditCalculatorSelector creditCalculatorSelector;

    /** 教师评分后发放积分：基础分(ScoreCalculator) + 奖励分(CreditCalculator) */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskGraded(TaskEvent event) {
        if (!"task_graded".equals(event.getEventType())) return;
        SchoolContext.set(1L);
        Long studentId = event.getStudentId();
        Long taskId = event.getTaskId();

        try {
            String bizKey = "TASK_GRADED:" + taskId + ":" + studentId;

            // 防重复：使用 biz_key 唯一约束确保同一任务+学生仅发一次分
            Long existing = creditMapper.selectCount(new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getBizKey, bizKey));
            if (existing != null && existing > 0) {
                log.info("积分已发放，跳过: bizKey={}", bizKey);
                return;
            }

            Student student = studentMapper.selectById(studentId);
            if (student == null) { log.warn("积分发放: 学生不存在 studentId={}", studentId); return; }

            Task task = taskMapper.selectById(taskId);
            ScoreType scoreType = task != null && task.getScoreType() != null
                ? ScoreType.valueOf(task.getScoreType()) : ScoreType.POINT_100;

            ScoreCalculator calculator = calculatorSelector.getCalculator(scoreType);
            if (calculator == null) { log.warn("未找到评分计算器: {}", scoreType); return; }

            Map<String, Object> subData = Map.of(
                "score", event.getData().getOrDefault("score", "0"),
                "gradeLevel", event.getData().getOrDefault("gradeLevel", "")
            );
            int baseCredits = calculator.toCreditValue(subData);

            // 额外奖励积分 — 从 credit_rules 表读取规则
            Map<String, Object> bonusCtx = new HashMap<>();
            bonusCtx.put("actionType", "task_graded");
            bonusCtx.put("score", subData.get("score"));
            bonusCtx.put("gradeLevel", subData.get("gradeLevel"));
            bonusCtx.put("studentId", studentId);
            bonusCtx.put("taskId", taskId);
            int bonusCredits = creditCalculatorSelector.getCalculator("DEFAULT").calculate(bonusCtx);

            int totalCredits = baseCredits + bonusCredits;
            if (totalCredits <= 0) return;

            int oldBalance = student.getTotalCredits() != null ? student.getTotalCredits() : 0;
            int newBalance = oldBalance + totalCredits;

            CreditTransaction tx = new CreditTransaction();
            tx.setStudentId(studentId);
            tx.setTransactionType("earn");
            tx.setCreditAmount(totalCredits);
            tx.setBalanceAfter(newBalance);
            tx.setSourceType("TASK");
            tx.setSourceId(taskId);
            tx.setDescription("任务评分积分: base=" + baseCredits + " bonus=" + bonusCredits);
            tx.setBizKey(bizKey);
            creditMapper.insert(tx);

            student.setTotalCredits(newBalance);
            studentMapper.updateById(student);
            log.info("积分发放: studentId={}, scoreType={}, base={}, bonus={}, total={}, bizKey={}",
                studentId, scoreType, baseCredits, bonusCredits, totalCredits, bizKey);
        } catch (Exception e) {
            log.error("积分发放失败: studentId={}, taskId={}", studentId, taskId, e);
        } finally {
            SchoolContext.clear();
        }
    }

    /** 学生提交任务后发放即时积分 — 按时提交奖励 */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskSubmitted(TaskEvent event) {
        if (!"task_submitted".equals(event.getEventType())) return;
        SchoolContext.set(1L);
        Long studentId = event.getStudentId();
        Long taskId = event.getTaskId();

        try {
            Student student = studentMapper.selectById(studentId);
            if (student == null) return;

            String today = java.time.LocalDate.now().toString();
            String bizKey = "TASK_SUBMITTED:" + taskId + ":" + studentId + ":" + today;

            // 防重复：使用 biz_key 唯一约束确保同一任务+学生+日期仅发一次分
            Long count = creditMapper.selectCount(new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getBizKey, bizKey));
            if (count != null && count > 0) return;

            Map<String, Object> bonusCtx = new HashMap<>();
            bonusCtx.put("actionType", "task_submitted");
            bonusCtx.put("studentId", studentId);
            bonusCtx.put("taskId", taskId);
            int credits = creditCalculatorSelector.getCalculator("DEFAULT").calculate(bonusCtx);
            if (credits <= 0) return;

            int oldBalance = student.getTotalCredits() != null ? student.getTotalCredits() : 0;
            int newBalance = oldBalance + credits;

            CreditTransaction tx = new CreditTransaction();
            tx.setStudentId(studentId);
            tx.setTransactionType("earn");
            tx.setCreditAmount(credits);
            tx.setBalanceAfter(newBalance);
            tx.setSourceType("TASK");
            tx.setSourceId(taskId);
            tx.setDescription("按时提交奖励");
            tx.setBizKey(bizKey);
            creditMapper.insert(tx);

            student.setTotalCredits(newBalance);
            studentMapper.updateById(student);
            log.info("提交积分: studentId={}, +{}分, bizKey={}", studentId, credits, bizKey);
        } catch (Exception e) {
            log.error("提交积分发放失败: studentId={}, taskId={}", studentId, taskId, e);
        } finally {
            SchoolContext.clear();
        }
    }
}
