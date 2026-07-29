package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final DictSubjectMapper dictSubjectMapper;
    private final DictGradeMapper dictGradeMapper;
    private final com.school.teaching.mapper.SchoolTermMapper schoolTermMapper;

    /** 学科列表（来自学科字典 dict_subject） */
    @Cacheable("subjectTree")
    public List<Map<String, Object>> getSubjectTree() {
        List<DictSubject> subjects = dictSubjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getStatus, 1).orderByAsc(DictSubject::getSortOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DictSubject s : subjects) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", s.getId());
            node.put("name", s.getSubjectName());
            node.put("sortOrder", s.getSortOrder());
            node.put("children", List.of());
            result.add(node);
        }
        return result;
    }

    /** 活跃学科扁平列表（用于教师任教学科选择） */
    public List<Map<String, Object>> getActiveSubjects() {
        return dictSubjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getStatus, 1).orderByAsc(DictSubject::getSortOrder))
            .stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("subjectName", s.getSubjectName());
                m.put("sortOrder", s.getSortOrder());
                return m;
            }).toList();
    }

    /** 年级列表（可按学段筛选，stageId 过滤在内存中完成因 dict_grade 无 stage_id 列） */
    @Cacheable("gradeList")
    public List<Map<String, Object>> getGradeList(Long stageId) {
        LambdaQueryWrapper<DictGrade> w = new LambdaQueryWrapper<DictGrade>()
            .eq(DictGrade::getStatus, 1).orderByAsc(DictGrade::getSortOrder);
        return dictGradeMapper.selectList(w).stream()
            .filter(g -> stageId == null || stageId.equals(g.getStageId()))
            .map(g -> Map.<String,Object>of("id", g.getId(), "name", g.getGradeName(), "sortOrder", g.getSortOrder()))
            .collect(Collectors.toList());
    }

    /** 五育标签 */
    @Cacheable("wuyuTags")
    public List<Map<String, String>> getWuyuTags() {
        return List.of(
            Map.of("key","deyu","label","德育"),
            Map.of("key","zhiyu","label","智育"),
            Map.of("key","tiyu","label","体育"),
            Map.of("key","meiyu","label","美育"),
            Map.of("key","laoyu","label","劳育")
        );
    }

    /** 题型枚举 */
    @Cacheable("questionTypes")
    public List<Map<String, Object>> getQuestionTypes() {
        return Arrays.stream(QuestionTypeEnum.values()).map(t -> Map.<String,Object>of(
            "key", t.name(), "label", t.getLabel(), "objective", t.isObjective(), "composite", t.isComposite()
        )).collect(Collectors.toList());
    }

    /** 学期列表 */
    @Cacheable("termList")
    public List<Map<String, Object>> getTermList(Long schoolId) {
        return schoolTermMapper.selectList(
            new LambdaQueryWrapper<SchoolTerm>().eq(SchoolTerm::getSchoolId, schoolId != null ? schoolId : 1L).orderByDesc(SchoolTerm::getStartDate))
            .stream().map(t -> Map.<String,Object>of("id", t.getId(), "name", t.getName(), "startDate", t.getStartDate(), "endDate", t.getEndDate(), "isCurrent", t.getIsCurrent()))
            .collect(Collectors.toList());
    }
}
