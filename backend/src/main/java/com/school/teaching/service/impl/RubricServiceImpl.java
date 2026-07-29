package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.RubricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RubricServiceImpl implements RubricService {

    @Autowired private RubricMapper rubricMapper;
    @Autowired private RubricDimensionMapper dimMapper;
    @Autowired private com.school.teaching.mapper.ProfessionRubricPresetMapper presetMapper;

    @Override @Transactional
    public Rubric create(Rubric rubric) {
        rubric.setCreatedBy(resolveTeacherId());
        rubric.setSchoolId(SecurityUtils.getCurrentSchoolId() != null ? SecurityUtils.getCurrentSchoolId() : 1L);
        rubric.setStageId(SecurityUtils.getCurrentStageId() != null ? SecurityUtils.getCurrentStageId() : 4L);
        rubricMapper.insert(rubric);
        return rubric;
    }

    @Override @Transactional
    public Rubric update(Long id, Rubric data) {
        Rubric r = getById(id);
        r.setName(data.getName()); r.setScope(data.getScope());
        rubricMapper.updateById(r);
        return r;
    }

    @Override @Transactional
    public void delete(Long id) {
        getById(id);
        dimMapper.delete(new LambdaQueryWrapper<RubricDimension>().eq(RubricDimension::getRubricId, id));
        rubricMapper.deleteById(id);
    }

    @Override public Rubric getById(Long id) {
        Rubric r = rubricMapper.selectById(id);
        if (r == null) throw new BusinessException(404, "量规不存在");
        return r;
    }

    @Override public List<Rubric> listBySchool(Long schoolId) {
        return rubricMapper.selectList(new LambdaQueryWrapper<Rubric>().eq(Rubric::getSchoolId, schoolId));
    }

    @Override @Transactional
    public List<RubricDimension> copyFromPreset(Long rubricId, Long presetId) {
        getById(rubricId);
        com.school.teaching.entity.ProfessionRubricPreset preset = presetMapper.selectById(presetId);
        if (preset == null) throw new BusinessException(404, "预设不存在");
        if (preset.getDimensionsJson() == null) return List.of();

        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> dims = om.readValue(preset.getDimensionsJson(), List.class);
            List<RubricDimension> result = new ArrayList<>();
            for (Map<String, Object> d : dims) {
                RubricDimension dim = new RubricDimension();
                dim.setRubricId(rubricId);
                dim.setName((String) d.get("name"));
                dim.setWeight(new java.math.BigDecimal(String.valueOf(d.getOrDefault("weight", "0.25"))));
                dim.setDescription((String) d.get("description"));
                dim.setLevelsJson(om.writeValueAsString(d.get("levels")));
                dimMapper.insert(dim);
                result.add(dim);
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(500, "预设解析失败: " + e.getMessage());
        }
    }

    @Override public List<RubricDimension> getDimensions(Long rubricId) {
        return dimMapper.selectList(new LambdaQueryWrapper<RubricDimension>().eq(RubricDimension::getRubricId, rubricId));
    }

    @Override @Transactional
    public RubricDimension saveDimension(RubricDimension dim) {
        if (dim.getId() != null) { dimMapper.updateById(dim); return dim; }
        dimMapper.insert(dim); return dim;
    }

    @Override @Transactional
    public void deleteDimension(Long dimId) { dimMapper.deleteById(dimId); }

    @Override public List<Map<String, Object>> listPresets(Long schoolId) {
        return presetMapper.selectList(new LambdaQueryWrapper<com.school.teaching.entity.ProfessionRubricPreset>()
                .eq(com.school.teaching.entity.ProfessionRubricPreset::getSchoolId, schoolId))
            .stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId()); m.put("name", p.getName());
                m.put("profession", p.getProfession()); m.put("wuyuTag", p.getWuyuTag());
                return m;
            }).collect(Collectors.toList());
    }

    private Long resolveTeacherId() {
        Long userId = SecurityUtils.getCurrentUserId();
        // simplified
        return userId != null ? userId : 1L;
    }
}
