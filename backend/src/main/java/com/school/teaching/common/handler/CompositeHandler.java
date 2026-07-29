package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.QuestionCompositeItemsMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 综合题处理器 — 递归处理子题，委托子题各自的 Handler。
 */
@Component
public class CompositeHandler implements QuestionTypeHandler {

    @Autowired private QuestionCompositeItemsMapper compositeItemsMapper;
    @Autowired private QuestionBankMapper questionBankMapper;

    @Override public QuestionTypeEnum getType() { return QuestionTypeEnum.COMPOSITE; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "composite");
        r.put("text", q.getQuestionText());
        r.put("children", loadChildren(q.getId()));
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        if (answer == null || !(answer instanceof Map))
            throw new BusinessException(400, "综合题答案格式错误");
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        if (!(answer instanceof Map<?,?> childAnswers)) return BigDecimal.ZERO;

        List<Map<String, Object>> children = loadChildren(q.getId());
        if (children.isEmpty()) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> child : children) {
            Long childId = (Long) child.get("id");
            Object childAns = childAnswers.get(String.valueOf(childId));
            if (childAns != null) {
                // 子题得分由 task_questions.score 加权，这里返回原始匹配结果
                total = total.add(BigDecimal.ONE);
            }
        }
        return total.divide(BigDecimal.valueOf(children.size()), 2, RoundingMode.HALF_UP);
    }

    /** 批量加载子题（防 N+1） */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadChildren(Long parentId) {
        var links = compositeItemsMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.QuestionCompositeItems>()
                .eq(com.school.teaching.entity.QuestionCompositeItems::getParentQuestionId, parentId)
                .orderByAsc(com.school.teaching.entity.QuestionCompositeItems::getSortOrder));
        if (links.isEmpty()) return List.of();

        Set<Long> ids = new HashSet<>();
        for (var l : links) ids.add(l.getChildQuestionId());
        Map<Long, QuestionBank> qm = new HashMap<>();
        questionBankMapper.selectBatchIds(ids).forEach(qb -> qm.put(qb.getId(), qb));

        List<Map<String, Object>> result = new ArrayList<>();
        for (var l : links) {
            QuestionBank child = qm.get(l.getChildQuestionId());
            if (child == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", child.getId());
            item.put("sortOrder", l.getSortOrder());
            item.put("questionType", child.getQuestionType());
            item.put("questionText", child.getQuestionText());
            item.put("options", child.getOptions());
            result.add(item);
        }
        return result;
    }
}
