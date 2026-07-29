package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.ClassroomQuestion;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.QuestionEditHistory;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ClassroomQuestionMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.QuestionEditHistoryMapper;
import com.school.teaching.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionReviewService {

    private final QuestionBankMapper questionBankMapper;
    private final ClassroomQuestionMapper classroomQuestionMapper;
    private final QuestionEditHistoryMapper editHistoryMapper;

    @Transactional
    public void approve(Long questionId) {
        QuestionBank q = questionBankMapper.selectById(questionId);
        if (q == null) throw new BusinessException(404, "题目不存在");
        q.setStatus(1);
        questionBankMapper.updateById(q);
        if (q.getCategory() != null && !q.getCategory().isEmpty()) {
            ClassroomQuestion cq = new ClassroomQuestion();
            cq.setContent(q.getQuestionText());
            cq.setReferenceAnswer(q.getCorrectAnswer());
            cq.setQuestionType(q.getQuestionType());
            cq.setSource("AI");
            cq.setSourceQuestionId(q.getId());
            cq.setFromAi(1);
            cq.setAiCategory(q.getCategory());
            cq.setIntent(q.getIntent());
            cq.setSubject(q.getSubject());
            classroomQuestionMapper.insert(cq);
        }
    }

    @Transactional
    public void reject(Long questionId) {
        QuestionBank q = questionBankMapper.selectById(questionId);
        if (q == null) throw new BusinessException(404, "题目不存在");
        q.setStatus(2);
        questionBankMapper.updateById(q);
        try {
            QuestionEditHistory hist = new QuestionEditHistory();
            hist.setQuestionId(questionId);
            hist.setVersion(q.getVersion() != null ? q.getVersion() : 1);
            hist.setChangeSummary("教师驳回");
            hist.setBeforeSnapshot(toJson(q));
            hist.setEditedBy(SecurityUtils.getCurrentUserId());
            hist.setEditType("REJECT");
            hist.setSchoolId(1L);
            editHistoryMapper.insert(hist);
        } catch (Exception e) {
            log.warn("驳回历史写入失败: qid={}", questionId, e);
        }
    }

    public List<QuestionBank> listDrafts(Long teacherId, Long schoolId) {
        return questionBankMapper.selectList(new LambdaQueryWrapper<QuestionBank>()
            .eq(QuestionBank::getCreatedBy, teacherId).eq(QuestionBank::getStatus, 0));
    }

    private String toJson(Object obj) {
        try { return new ObjectMapper().writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
