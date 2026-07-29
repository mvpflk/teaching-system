package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.ParentFeedbackSummary;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ParentFeedbackSummaryMapper;
import com.school.teaching.service.ParentFeedbackSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentFeedbackSummaryServiceImpl implements ParentFeedbackSummaryService {

    private final ParentFeedbackSummaryMapper mapper;

    @Override
    public ParentFeedbackSummary getById(Long id) {
        ParentFeedbackSummary r = mapper.selectById(id);
        if (r == null) throw new BusinessException(404, "家长反馈汇总记录不存在");
        return r;
    }

    @Override
    @Transactional
    public ParentFeedbackSummary create(ParentFeedbackSummary summary) {
        mapper.insert(summary);
        return summary;
    }

    @Override
    @Transactional
    public ParentFeedbackSummary update(Long id, ParentFeedbackSummary data) {
        ParentFeedbackSummary existing = getById(id);
        if (data.getClassId() != null) existing.setClassId(data.getClassId());
        if (data.getPeriod() != null) existing.setPeriod(data.getPeriod());
        if (data.getTotalFeedback() != null) existing.setTotalFeedback(data.getTotalFeedback());
        if (data.getPositiveCount() != null) existing.setPositiveCount(data.getPositiveCount());
        if (data.getNegativeCount() != null) existing.setNegativeCount(data.getNegativeCount());
        if (data.getCategoriesJson() != null) existing.setCategoriesJson(data.getCategoriesJson());
        if (data.getSummaryText() != null) existing.setSummaryText(data.getSummaryText());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }

    @Override
    public IPage<ParentFeedbackSummary> getPage(Long classId, String period,
                                                int page, int size) {
        LambdaQueryWrapper<ParentFeedbackSummary> q = new LambdaQueryWrapper<>();
        if (classId != null) q.eq(ParentFeedbackSummary::getClassId, classId);
        if (period != null && !period.isEmpty()) q.eq(ParentFeedbackSummary::getPeriod, period);
        q.orderByDesc(ParentFeedbackSummary::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public List<ParentFeedbackSummary> getLatestByClass() {
        List<ParentFeedbackSummary> all = mapper.selectList(null);
        Map<Long, List<ParentFeedbackSummary>> grouped = all.stream()
                .collect(Collectors.groupingBy(ParentFeedbackSummary::getClassId));
        List<ParentFeedbackSummary> result = new ArrayList<>();
        grouped.forEach((classId, list) -> {
            list.sort(Comparator.comparing(ParentFeedbackSummary::getCreatedAt).reversed());
            result.add(list.get(0));
        });
        result.sort(Comparator.comparing(ParentFeedbackSummary::getClassId));
        return result;
    }
}
