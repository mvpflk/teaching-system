package com.school.teaching.service;

import com.school.teaching.entity.ClassTypeConfig;
import java.util.List;
import java.util.Map;

public interface ClassTypeConfigService {
    List<ClassTypeConfig> listByStageId(Long stageId);
    List<ClassTypeConfig> listAll();
    // [已禁用] create — 班级类型固化为普高班+职高班，由SQL种子维护
    // ClassTypeConfig create(ClassTypeConfig config);
    ClassTypeConfig update(Long id, ClassTypeConfig config);
    // [已禁用] delete — 同上
    // void delete(Long id);
    // [已禁用] stats — 简化后不再需要
    // Map<String, Object> getTypeStats(Long stageId);
}
