package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.ParentFeedbackFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ParentFeedbackFormServiceImpl implements ParentFeedbackFormService {

    @Autowired private ParentFeedbackFormMapper formMapper;
    @Autowired private ParentFeedbackResponseMapper responseMapper;
    @Autowired private ParentFeedbackSummaryMapper summaryMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private ParentChildRelationMapper relationMapper;
    @Autowired private NotificationService notificationService;

    @Override
    @Transactional
    public ParentFeedbackForm create(ParentFeedbackForm form) {
        if (form.getClassId() == null) throw new BusinessException(400, "班级必填");
        if (form.getTitle() == null || form.getTitle().isBlank()) throw new BusinessException(400, "标题必填");
        if (form.getPeriod() == null || form.getPeriod().isBlank()) throw new BusinessException(400, "周期必填");
        form.setStatus("DRAFT");
        formMapper.insert(form);
        return form;
    }

    @Override
    @Transactional
    public ParentFeedbackForm update(Long id, ParentFeedbackForm form) {
        ParentFeedbackForm existing = formMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "问卷不存在");
        if (!"DRAFT".equals(existing.getStatus())) throw new BusinessException(400, "仅草稿状态可编辑");
        existing.setClassId(form.getClassId());
        existing.setTitle(form.getTitle());
        existing.setPeriod(form.getPeriod());
        formMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ParentFeedbackForm existing = formMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "问卷不存在");
        if ("SENT".equals(existing.getStatus())) throw new BusinessException(400, "已发送问卷不可删除，请先关闭");
        formMapper.deleteById(id);
    }

    @Override
    public ParentFeedbackForm getById(Long id) {
        return formMapper.selectById(id);
    }

    @Override
    public IPage<ParentFeedbackForm> getPage(Long classId, String period, int page, int size) {
        LambdaQueryWrapper<ParentFeedbackForm> w = new LambdaQueryWrapper<>();
        if (classId != null) w.eq(ParentFeedbackForm::getClassId, classId);
        if (period != null && !period.isBlank()) w.eq(ParentFeedbackForm::getPeriod, period);
        w.orderByDesc(ParentFeedbackForm::getCreatedAt);
        return formMapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    @Transactional
    public void send(Long formId) {
        ParentFeedbackForm form = formMapper.selectById(formId);
        if (form == null) throw new BusinessException(404, "问卷不存在");
        if (!"DRAFT".equals(form.getStatus())) throw new BusinessException(400, "仅草稿可发送");

        Classes cls = classesMapper.selectById(form.getClassId());
        if (cls == null) throw new BusinessException(404, "班级不存在");

        List<Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, form.getClassId()));
        if (students.isEmpty()) throw new BusinessException(400, "该班级无学生");

        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        List<ParentChildRelation> relations = relationMapper.selectList(
            new LambdaQueryWrapper<ParentChildRelation>().in(ParentChildRelation::getStudentId, studentIds));
        if (relations.isEmpty()) throw new BusinessException(400, "该班级学生无关联家长");

        Set<Long> parentIds = relations.stream().map(ParentChildRelation::getParentId).collect(Collectors.toSet());

        form.setStatus("SENT");
        form.setSentAt(LocalDateTime.now());
        formMapper.updateById(form);

        notificationService.notifyBatch(new ArrayList<>(parentIds), "FEEDBACK_FORM",
            "请填写家长反馈问卷：「" + form.getTitle() + "」",
            "请填写家长反馈问卷：「" + form.getTitle() + "」", formId);
    }

    @Override
    @Transactional
    public void close(Long formId) {
        ParentFeedbackForm form = formMapper.selectById(formId);
        if (form == null) throw new BusinessException(404, "问卷不存在");
        if (!"SENT".equals(form.getStatus())) throw new BusinessException(400, "仅已发送问卷可关闭");
        form.setStatus("CLOSED");
        form.setClosedAt(LocalDateTime.now());
        formMapper.updateById(form);
    }

    @Override
    public Map<String, Object> getStats(Long formId) {
        ParentFeedbackForm form = formMapper.selectById(formId);
        if (form == null) throw new BusinessException(404, "问卷不存在");

        List<ParentFeedbackResponse> responses = responseMapper.selectList(
            new LambdaQueryWrapper<ParentFeedbackResponse>().eq(ParentFeedbackResponse::getFormId, formId));

        List<Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, form.getClassId()));
        long totalParents = 0;
        if (!students.isEmpty()) {
            Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
            totalParents = relationMapper.selectCount(
                new LambdaQueryWrapper<ParentChildRelation>().in(ParentChildRelation::getStudentId, studentIds));
        }

        int responded = responses.size();
        double responseRate = totalParents > 0 ? Math.round(responded * 1000.0 / totalParents) / 10.0 : 0;

        double avgSat = responses.stream().filter(r -> r.getSatisfaction() != null)
            .mapToInt(ParentFeedbackResponse::getSatisfaction).average().orElse(0);
        double avgTq = responses.stream().filter(r -> r.getTeachingQuality() != null)
            .mapToInt(ParentFeedbackResponse::getTeachingQuality).average().orElse(0);
        double avgHl = responses.stream().filter(r -> r.getHomeworkLoad() != null)
            .mapToInt(ParentFeedbackResponse::getHomeworkLoad).average().orElse(0);
        double avgComm = responses.stream().filter(r -> r.getCommunication() != null)
            .mapToInt(ParentFeedbackResponse::getCommunication).average().orElse(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalParents", totalParents);
        result.put("respondedCount", responded);
        result.put("responseRate", Math.round(responseRate * 10) / 10.0);
        result.put("avgSatisfaction", Math.round(avgSat * 10) / 10.0);
        result.put("avgTeachingQuality", Math.round(avgTq * 10) / 10.0);
        result.put("avgHomeworkLoad", Math.round(avgHl * 10) / 10.0);
        result.put("avgCommunication", Math.round(avgComm * 10) / 10.0);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> generateSummary(Long formId) {
        ParentFeedbackForm form = formMapper.selectById(formId);
        if (form == null) throw new BusinessException(404, "问卷不存在");

        Map<String, Object> stats = getStats(formId);
        int responded = ((Number) stats.get("respondedCount")).intValue();
        double avgSat = ((Number) stats.get("avgSatisfaction")).doubleValue();

        int positive = responded > 0 ? (int) (responded * avgSat / 5.0) : 0;
        int negative = responded - positive;

        String summaryText = String.format("共%d位家长参与反馈（回收率%.1f%%），满意度均分%.1f/5，教学质量均分%.1f/5，作业量均分%.1f/5，沟通均分%.1f/5。",
            responded, stats.get("responseRate"), avgSat,
            ((Number) stats.get("avgTeachingQuality")).doubleValue(),
            ((Number) stats.get("avgHomeworkLoad")).doubleValue(),
            ((Number) stats.get("avgCommunication")).doubleValue());

        ParentFeedbackSummary summary = new ParentFeedbackSummary();
        summary.setClassId(form.getClassId());
        summary.setPeriod(form.getPeriod());
        summary.setTotalFeedback(responded);
        summary.setPositiveCount(positive);
        summary.setNegativeCount(negative);
        summary.setSummaryText(summaryText);
        summaryMapper.insert(summary);

        Map<String, Object> result = new LinkedHashMap<>(stats);
        result.put("summaryId", summary.getId());
        result.put("summaryText", summaryText);
        return result;
    }
}
