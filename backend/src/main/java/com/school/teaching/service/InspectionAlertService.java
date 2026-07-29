package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.InspectionAlertLog;
import com.school.teaching.entity.InspectionAlertRule;

import java.util.List;

public interface InspectionAlertService {
    void checkAll();
    List<InspectionAlertRule> getRules();
    InspectionAlertRule updateRule(Long id, InspectionAlertRule rule);
    IPage<InspectionAlertLog> getLogs(int page, int size, Boolean isRead);
    int markAsRead(Long id);
    int markAllAsRead();
}
