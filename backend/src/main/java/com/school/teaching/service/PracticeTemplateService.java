package com.school.teaching.service;

import com.school.teaching.entity.PracticeTemplate;
import java.util.List;
import java.util.Map;

public interface PracticeTemplateService {
    /** 模板库列表（支持学科/分类/来源筛选） */
    List<PracticeTemplate> list(String subject, String category, String source);

    /** 模板详情 */
    PracticeTemplate getById(Long id);

    /** 从模板创建方案草稿：复制模板数据→创建 plan + rubrics，返回 planId */
    Map<String, Object> applyTemplate(Long templateId, Long userId);

    /** 将方案保存为模板 */
    PracticeTemplate saveAsTemplate(Long planId, Long userId);

    /** 增加使用计数 */
    void incrementUseCount(Long templateId);
}
