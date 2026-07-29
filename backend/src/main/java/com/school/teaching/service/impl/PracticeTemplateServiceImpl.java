package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.PracticeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PracticeTemplateServiceImpl implements PracticeTemplateService {

    @Autowired private PracticeTemplateMapper templateMapper;
    @Autowired private PracticePlanMapper planMapper;
    @Autowired private PracticeRubricMapper rubricMapper;

    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public List<PracticeTemplate> list(String subject, String category, String source) {
        LambdaQueryWrapper<PracticeTemplate> qw = new LambdaQueryWrapper<>();
        if (subject != null && !subject.isBlank()) qw.eq(PracticeTemplate::getSubject, subject);
        if (category != null && !category.isBlank()) qw.eq(PracticeTemplate::getCategory, category);
        if (source != null && !source.isBlank()) qw.eq(PracticeTemplate::getSource, source);
        qw.orderByDesc(PracticeTemplate::getUseCount);
        return templateMapper.selectList(qw);
    }

    @Override
    public PracticeTemplate getById(Long id) {
        PracticeTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "模板不存在");
        return t;
    }

    @Override
    @Transactional
    public Map<String, Object> applyTemplate(Long templateId, Long userId) {
        PracticeTemplate t = getById(templateId);

        // 1. 创建方案草稿
        PracticePlan plan = new PracticePlan();
        plan.setTitle(t.getTitle() + "（副本）");
        plan.setDescription(t.getDescription());
        plan.setStepsJson(t.getStepsJson());
        plan.setScoringModel(t.getScoringModel());
        plan.setStatus("DRAFT");
        plan.setCreatedBy(userId);
        plan.setCreateTime(LocalDateTime.now());
        planMapper.insert(plan);

        // 2. 复制评分细则
        if (t.getRubricsJson() != null && !t.getRubricsJson().isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rubricList = om.readValue(t.getRubricsJson(), List.class);
                int sort = 0;
                for (Map<String, Object> rm : rubricList) {
                    PracticeRubric r = new PracticeRubric();
                    r.setPlanId(plan.getId());
                    r.setDimension((String) rm.getOrDefault("dimension", "dim_" + sort));
                    r.setDimensionLabel((String) rm.getOrDefault("dimensionLabel", "维度" + sort));
                    r.setWeight(rm.get("weight") != null
                        ? BigDecimal.valueOf(((Number) rm.get("weight")).doubleValue()) : BigDecimal.ZERO);
                    r.setCriteria(rm.get("criteria") != null ? om.writeValueAsString(rm.get("criteria")) : null);
                    r.setSortOrder(sort++);
                    rubricMapper.insert(r);
                }
            } catch (Exception e) {
                throw new BusinessException(400, "模板评分数据解析失败: " + e.getMessage());
            }
        }

        // 3. 增加模板使用计数
        if (t.getUseCount() == null) t.setUseCount(0);
        t.setUseCount(t.getUseCount() + 1);
        templateMapper.updateById(t);

        return Map.of("planId", plan.getId(), "title", plan.getTitle());
    }

    @Override
    @Transactional
    public PracticeTemplate saveAsTemplate(Long planId, Long userId) {
        PracticePlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(404, "方案不存在");
        if (!plan.getCreatedBy().equals(userId) && !SecurityUtils.isAdmin())
            throw new BusinessException(403, "无权操作");

        PracticeTemplate t = new PracticeTemplate();
        t.setTitle(plan.getTitle());
        t.setDescription(plan.getDescription());
        t.setSubject(plan.getSubject());
        t.setStepsJson(plan.getStepsJson());
        t.setScoringModel(plan.getScoringModel());
        t.setSource("SHARED");
        t.setSourcePlanId(planId);
        t.setUseCount(0);

        // 序列化评分细则
        List<PracticeRubric> rubrics = rubricMapper.selectList(
            new LambdaQueryWrapper<PracticeRubric>()
                .eq(PracticeRubric::getPlanId, planId)
                .orderByAsc(PracticeRubric::getSortOrder));
        try {
            List<Map<String, Object>> rlist = new ArrayList<>();
            for (PracticeRubric r : rubrics) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("dimension", r.getDimension());
                rm.put("dimensionLabel", r.getDimensionLabel());
                rm.put("weight", r.getWeight());
                if (r.getCriteria() != null) {
                    rm.put("criteria", om.readValue(r.getCriteria(), List.class));
                }
                rlist.add(rm);
            }
            t.setRubricsJson(om.writeValueAsString(rlist));
        } catch (Exception e) {
            throw new BusinessException(400, "评分数据序列化失败");
        }

        templateMapper.insert(t);
        return t;
    }

    @Override
    public void incrementUseCount(Long templateId) {
        PracticeTemplate t = templateMapper.selectById(templateId);
        if (t != null) {
            if (t.getUseCount() == null) t.setUseCount(0);
            t.setUseCount(t.getUseCount() + 1);
            templateMapper.updateById(t);
        }
    }
}
