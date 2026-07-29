package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.ParentFeedbackSummary;

import java.util.List;

public interface ParentFeedbackSummaryService {
    ParentFeedbackSummary getById(Long id);
    ParentFeedbackSummary create(ParentFeedbackSummary summary);
    ParentFeedbackSummary update(Long id, ParentFeedbackSummary summary);
    void delete(Long id);
    IPage<ParentFeedbackSummary> getPage(Long classId, String period,
                                        int page, int size);
    List<ParentFeedbackSummary> getLatestByClass();
}
