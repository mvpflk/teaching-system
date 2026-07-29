package com.school.teaching.service;

import com.school.teaching.entity.Rubric;
import com.school.teaching.entity.RubricDimension;
import java.util.List;
import java.util.Map;

public interface RubricService {

    Rubric create(Rubric rubric);
    Rubric update(Long id, Rubric data);
    void delete(Long id);
    Rubric getById(Long id);
    List<Rubric> listBySchool(Long schoolId);

    /** 从专业预设复制维度到指定量规 */
    List<RubricDimension> copyFromPreset(Long rubricId, Long presetId);

    /** 获取量规的维度列表 */
    List<RubricDimension> getDimensions(Long rubricId);

    /** 保存/更新一个维度 */
    RubricDimension saveDimension(RubricDimension dim);
    void deleteDimension(Long dimId);

    /** 获取预设库列表 */
    List<Map<String, Object>> listPresets(Long schoolId);
}
