package com.school.teaching.service;

import com.school.teaching.entity.EduStageConfig;

import java.util.List;
import java.util.Map;

/**
 * @deprecated 考虑合并到 ClassServiceImpl — 该接口只有一个实现且方法较少。
 * 保留接口因为 @Transactional 需要 Spring AOP 代理。
 */
@Deprecated
public interface StageConfigService {

    /** 批量修改班级类型 */
    int batchUpdateClassType(List<Integer> classIds, String classType);

    /** 学段转换记录分页查询 */
    Map<String, Object> pageStageChangeLogs(int page, int size);

    /** 教师跨类统计 */
    List<Map<String, Object>> getTeacherCrossTypeStats();

    /** 学段类型统计 */
    Map<String, Object> getStageStats();

    /** 数据一致性校验 */
    List<Map<String, Object>> checkDataConsistency();

    /** 列出所有 edu_stage_config */
    List<EduStageConfig> listConfigs();

    /** 开关 edu_stage_config */
    void toggleConfig(Long id, Integer enabled);
}
