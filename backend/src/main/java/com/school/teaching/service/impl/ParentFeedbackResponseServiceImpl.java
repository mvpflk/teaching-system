package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ParentFeedbackResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ParentFeedbackResponseServiceImpl implements ParentFeedbackResponseService {

    @Autowired private ParentFeedbackFormMapper formMapper;
    @Autowired private ParentFeedbackResponseMapper responseMapper;
    @Autowired private ParentChildRelationMapper relationMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private StudentMapper studentMapper;

    @Override
    @Transactional
    public ParentFeedbackResponse submit(ParentFeedbackResponse response) {
        if (response.getFormId() == null) throw new BusinessException(400, "问卷ID必填");
        if (response.getParentId() == null) throw new BusinessException(400, "家长ID必填");

        ParentFeedbackForm form = formMapper.selectById(response.getFormId());
        if (form == null) throw new BusinessException(404, "问卷不存在");
        if (!"SENT".equals(form.getStatus())) throw new BusinessException(400, "问卷未开放填写");

        // 校验该家长是否有关联孩子在目标班级
        List<Student> classStudents = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, form.getClassId()));
        if (classStudents.isEmpty()) throw new BusinessException(400, "该班级无学生");
        Set<Long> classStudentIds = classStudents.stream().map(Student::getId).collect(Collectors.toSet());
        long hasRelation = relationMapper.selectCount(
            new LambdaQueryWrapper<ParentChildRelation>()
                .eq(ParentChildRelation::getParentId, response.getParentId())
                .in(ParentChildRelation::getStudentId, classStudentIds));
        if (hasRelation == 0) throw new BusinessException(403, "您没有孩子在目标班级");

        // 自动填入第一个关联的学生ID（用于统计）
        if (response.getStudentId() == null) {
            List<ParentChildRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<ParentChildRelation>()
                    .eq(ParentChildRelation::getParentId, response.getParentId())
                    .in(ParentChildRelation::getStudentId, classStudentIds)
                    .last("LIMIT 1"));
            if (!relations.isEmpty()) response.setStudentId(relations.get(0).getStudentId());
        }

        // 防重复提交
        long existing = responseMapper.selectCount(
            new LambdaQueryWrapper<ParentFeedbackResponse>()
                .eq(ParentFeedbackResponse::getFormId, response.getFormId())
                .eq(ParentFeedbackResponse::getParentId, response.getParentId()));
        if (existing > 0) throw new BusinessException(409, "已提交过反馈");

        responseMapper.insert(response);
        return response;
    }

    @Override
    public List<ParentFeedbackResponse> getByForm(Long formId) {
        return responseMapper.selectList(
            new LambdaQueryWrapper<ParentFeedbackResponse>()
                .eq(ParentFeedbackResponse::getFormId, formId)
                .orderByDesc(ParentFeedbackResponse::getCreatedAt));
    }

    @Override
    public List<Map<String, Object>> getPendingForParent(Long parentId) {
        // 该家长关联的所有学生ID
        List<ParentChildRelation> relations = relationMapper.selectList(
            new LambdaQueryWrapper<ParentChildRelation>()
                .eq(ParentChildRelation::getParentId, parentId));
        if (relations.isEmpty()) return List.of();
        Set<Long> studentIds = relations.stream().map(ParentChildRelation::getStudentId)
            .collect(Collectors.toSet());

        // 学生所在的班级IDs
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Set<Long> classIds = students.stream().map(Student::getClassId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        if (classIds.isEmpty()) return List.of();

        // 已发送的问卷
        List<ParentFeedbackForm> forms = formMapper.selectList(
            new LambdaQueryWrapper<ParentFeedbackForm>()
                .in(ParentFeedbackForm::getClassId, classIds)
                .eq(ParentFeedbackForm::getStatus, "SENT"));
        if (forms.isEmpty()) return List.of();

        // 该家长已回复的问卷IDs
        Set<Long> formIds = forms.stream().map(ParentFeedbackForm::getId).collect(Collectors.toSet());
        List<ParentFeedbackResponse> responded = responseMapper.selectList(
            new LambdaQueryWrapper<ParentFeedbackResponse>()
                .in(ParentFeedbackResponse::getFormId, formIds)
                .eq(ParentFeedbackResponse::getParentId, parentId));
        Set<Long> respondedFormIds = responded.stream().map(ParentFeedbackResponse::getFormId)
            .collect(Collectors.toSet());

        // 班级名称缓存
        Map<Long, Classes> classMap = classesMapper.selectBatchIds(classIds).stream()
            .collect(Collectors.toMap(Classes::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ParentFeedbackForm form : forms) {
            if (respondedFormIds.contains(form.getId())) continue;
            Classes cls = classMap.get(form.getClassId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("formId", form.getId());
            item.put("title", form.getTitle());
            item.put("period", form.getPeriod());
            item.put("className", cls != null ? cls.getClassName() : "?");
            result.add(item);
        }
        return result;
    }
}
