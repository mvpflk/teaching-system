package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.InspectionReport;

import java.time.LocalDate;

public interface InspectionReportService {
    InspectionReport getById(Long id);
    InspectionReport generate(String reportType, LocalDate periodStart, LocalDate periodEnd, Long userId);
    InspectionReport publish(Long id);
    void delete(Long id);
    IPage<InspectionReport> getPage(int page, int size);
}
