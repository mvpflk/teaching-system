package com.school.teaching.agent.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.PrecisionProgress;
import com.school.teaching.entity.WrongQuestion;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent OCR 批阅结果保存服务 — 从 AgentController 提取，遵循 Controller→Service→Mapper 分层约定。
 * 负责将 OCR 批阅结果写入错题本并更新掌握度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOcrService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final PrecisionProgressMapper precisionProgressMapper;

    @Transactional
    public void saveOcrResult(Long studentId, Long questionId, Long nodeId, boolean isCorrect) {
        if (!isCorrect) {
            WrongQuestion existing = wrongQuestionMapper.selectOne(
                    new LambdaQueryWrapper<WrongQuestion>()
                            .eq(WrongQuestion::getStudentId, studentId)
                            .eq(WrongQuestion::getQuestionId, questionId)
                            .last("LIMIT 1"));
            if (existing != null) {
                existing.setWrongCount(existing.getWrongCount() + 1);
                existing.setLastWrongTime(LocalDateTime.now());
                existing.setIsMastered(0);
                wrongQuestionMapper.updateById(existing);
            } else {
                WrongQuestion wq = new WrongQuestion();
                wq.setStudentId(studentId);
                wq.setQuestionId(questionId);
                wq.setWrongCount(1);
                wq.setLastWrongTime(LocalDateTime.now());
                wq.setIsMastered(0);
                wq.setSourceType("AI_OCR");
                wrongQuestionMapper.insert(wq);
            }
        }

        if (nodeId != null) {
            PrecisionProgress pp = precisionProgressMapper.selectOne(
                    new LambdaQueryWrapper<PrecisionProgress>()
                            .eq(PrecisionProgress::getStudentId, studentId)
                            .eq(PrecisionProgress::getNodeId, nodeId)
                            .last("LIMIT 1"));
            if (pp != null) {
                int total = (pp.getTotalAttempts() != null ? pp.getTotalAttempts() : 0) + 1;
                int correct = (pp.getTotalCorrect() != null ? pp.getTotalCorrect() : 0) + (isCorrect ? 1 : 0);
                BigDecimal weight = BigDecimal.valueOf(Math.min(0.5, 0.1 + (total - 1) * 0.02));
                BigDecimal newMastery = weight.multiply(isCorrect ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                        .add(BigDecimal.ONE.subtract(weight).multiply(
                                pp.getMasteryPercent() != null ? pp.getMasteryPercent() : BigDecimal.ZERO));
                pp.setMasteryPercent(newMastery.setScale(1, java.math.RoundingMode.HALF_UP));
                pp.setTotalAttempts(total);
                pp.setTotalCorrect(correct);
                pp.setLastPracticeAt(LocalDateTime.now());
                if (newMastery.compareTo(BigDecimal.valueOf(80)) >= 0) pp.setStatus("mastered");
                else if (newMastery.compareTo(BigDecimal.valueOf(40)) >= 0) pp.setStatus("learning");
                else pp.setStatus("weak");
                precisionProgressMapper.updateById(pp);
            } else {
                PrecisionProgress ppNew = new PrecisionProgress();
                ppNew.setStudentId(studentId);
                ppNew.setNodeId(nodeId);
                ppNew.setMasteryPercent(isCorrect ? BigDecimal.valueOf(50) : BigDecimal.ZERO);
                ppNew.setTotalAttempts(1);
                ppNew.setTotalCorrect(isCorrect ? 1 : 0);
                ppNew.setLastPracticeAt(LocalDateTime.now());
                ppNew.setStatus(isCorrect ? "learning" : "weak");
                precisionProgressMapper.insert(ppNew);
            }
        }

        log.info("OCRSave: studentId={}, questionId={}, correct={}", studentId, questionId, isCorrect);
    }
}