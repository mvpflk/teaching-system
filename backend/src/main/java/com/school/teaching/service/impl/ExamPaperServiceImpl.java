package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ExamPaperService;
import com.school.teaching.service.TaskQuestionService;
import com.school.teaching.service.TaskService;
import com.school.teaching.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements ExamPaperService {

    private final ExamPaperMapper paperMapper;
    private final QuestionBankMapper questionMapper;
    private final TaskMapper taskMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final TeacherMapper teacherMapper;
    private final TaskService taskService;
    private final TaskQuestionService taskQuestionService;

    private static final Logger log = LoggerFactory.getLogger(ExamPaperServiceImpl.class);

    @Override
    public Map<String, Object> pageByCreator(int page, int pageSize, String subject) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<ExamPaper> qw = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCreatorId, userId)
                .eq(ExamPaper::getStatus, 1);
        if (subject != null && !subject.isEmpty()) {
            qw.eq(ExamPaper::getSubject, subject);
        }
        qw.orderByDesc(ExamPaper::getCreatedAt);

        IPage<ExamPaper> p = paperMapper.selectPage(new Page<>(page, pageSize), qw);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", p.getRecords());
        result.put("total", p.getTotal());
        result.put("page", (int) p.getCurrent());
        result.put("pageSize", (int) p.getSize());
        result.put("pages", (int) p.getPages());
        return result;
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw new BusinessException(404, "试卷不存在");
        if (!paper.getCreatorId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(403, "只能删除自己的试卷");
        }
        if (paper.getLockedAt() != null) {
            throw new BusinessException(400, "该试卷已锁定为标准化试卷，不可删除");
        }
        paper.setStatus(0);
        paperMapper.updateById(paper);
    }

    @Override
    @Transactional
    public Map<String, Object> createTaskFromPaper(Long paperId, Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(404, "试卷不存在");
        // E5: 校验试卷归属权 — 只能使用自己的试卷创建任务
        if (!paper.getCreatorId().equals(userId) && !SecurityUtils.isAdmin())
            throw new BusinessException(403, "只能使用自己的试卷创建任务");

        @SuppressWarnings("unchecked")
        List<Integer> targetIdsRaw = (List<Integer>) body.get("targetIds");
        boolean publishNow = Boolean.TRUE.equals(body.get("publishNow"));
        if (targetIdsRaw == null || targetIdsRaw.isEmpty()) throw new BusinessException(400, "请选择目标班级");

        // 从题库加载题目
        List<Long> questionIds = JsonUtils.parse(paper.getQuestionIds(), new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
        if (questionIds == null || questionIds.isEmpty()) throw new BusinessException(400, "试卷题目数据异常");

        List<QuestionBank> questions = questionMapper.selectBatchIds(questionIds);
        if (questions.isEmpty()) throw new BusinessException(400, "题目已被删除");

        // 读取分值预设
        Map<String, Object> scorePresets = null;
        if (paper.getScorePresets() != null && !paper.getScorePresets().isEmpty()) {
            scorePresets = JsonUtils.parseMap(paper.getScorePresets());
        }
        Map<String, Object> examConfig = null;
        if (paper.getExamConfig() != null && !paper.getExamConfig().isEmpty()) {
            examConfig = JsonUtils.parseMap(paper.getExamConfig());
        }

        // 读取每题分值
        Map<Long, Double> questionScores = new HashMap<>();
        Map<String, Double> typeScoreMap = new HashMap<>();
        if (scorePresets != null) {
            for (Map.Entry<String, Object> e : scorePresets.entrySet()) {
                typeScoreMap.put(e.getKey(), ((Number) e.getValue()).doubleValue());
            }
        }
        for (QuestionBank qb : questions) {
            double perScore = typeScoreMap.getOrDefault(qb.getQuestionType(), defaultScore(qb.getQuestionType()));
            questionScores.put(qb.getId(), perScore);
        }

        // 计算总分
        double totalScore = questionScores.values().stream().mapToDouble(Double::doubleValue).sum();

        // T-02: 建立题目ID→QuestionBank的快速查找，用于填充 categoryId
        Map<Long, QuestionBank> qbMap = questions.stream().collect(Collectors.toMap(QuestionBank::getId, q -> q));

        Long firstTaskId = null;
        // 为每个班级创建任务
        for (Integer targetIdRaw : targetIdsRaw) {
            Long classId = targetIdRaw.longValue();
            Task task = new Task();
            task.setTitle(paper.getTitle());
            task.setTaskType("FORMATIVE");
            task.setScoreType("POINT_100");
            task.setSubject(paper.getSubject());
            task.setTotalScore(BigDecimal.valueOf(totalScore));
            task.setTargetType("CLASS");
            task.setTargetId(classId);
            Map<String, Object> config = new LinkedHashMap<>();
            if (examConfig != null) config.putAll(examConfig);
            config.putIfAbsent("durationMinutes", paper.getDurationMinutes() != null ? paper.getDurationMinutes() : 60);
            config.putIfAbsent("passingScore", 60);
            task.setTaskConfig(JsonUtils.toJson(config));
            task.setTeacherId(getTeacherId(userId));
            task.setIsRequired(1);
            task.setAutoWrongbook(1);
            task.setSchoolId(1L);
            task.setStageId(4L);
            task.setStatus("DRAFT");
            taskMapper.insert(task);

            int sort = 0;
            List<TaskQuestion> tqBatch = new ArrayList<>();
            for (Long qid : questionIds) {
                TaskQuestion tq = new TaskQuestion();
                tq.setTaskId(task.getId());
                tq.setQuestionId(qid);
                tq.setSortOrder(sort++);
                tq.setScore(BigDecimal.valueOf(questionScores.getOrDefault(qid, 2.0)));
                tq.setSchoolId(1L);
                tq.setStageId(4L);
                QuestionBank qb = qbMap.get(qid);
                if (qb != null) tq.setCategoryId(qb.getCategoryId());
                tqBatch.add(tq);
            }
            taskQuestionService.batchSaveQuestions(tqBatch);

            if (firstTaskId == null) firstTaskId = task.getId();
            if (publishNow) taskService.publish(task.getId());
        }

        // 更新使用次数
        paper.setUseCount(paper.getUseCount() != null ? paper.getUseCount() + 1 : 1);
        paper.setLastTaskId(firstTaskId);
        paperMapper.updateById(paper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", firstTaskId);
        result.put("questionCount", questions.size());
        result.put("totalScore", totalScore);
        return result;
    }

    // ── 辅助方法 ──

    private Long getTeacherId(Long userId) {
        Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher == null) {
            log.warn("未找到 userId={} 对应的教师记录，使用 userId 作为 teacherId fallback", userId);
            return userId;
        }
        return teacher.getId();
    }

    private double defaultScore(String type) {
        return switch (type) {
            case "SINGLE_CHOICE" -> 2;
            case "MULTI_CHOICE" -> 3;
            case "TRUE_FALSE", "FILL_IN" -> 1;
            default -> 10;
        };
    }

    // ── P0-1: 标准化试卷标记 ──

    @Override
    @Transactional
    public void markStandardized(Long paperId, String paperRole, Long parallelPaperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(404, "试卷不存在");
        if (!paper.getCreatorId().equals(SecurityUtils.getCurrentUserId()) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "无权限操作此试卷");
        }
        if (paper.getLockedAt() != null) {
            throw new BusinessException(400, "试卷已锁定，不可修改标记");
        }

        // 验证平行卷引用
        if (parallelPaperId != null) {
            ExamPaper parallel = paperMapper.selectById(parallelPaperId);
            if (parallel == null) throw new BusinessException(400, "平行卷不存在");
            if (!parallel.getSubject().equals(paper.getSubject())) {
                throw new BusinessException(400, "平行卷必须与本卷学科一致");
            }
            if (parallel.getIsStandardized() == null || parallel.getIsStandardized() != 1) {
                throw new BusinessException(400, "平行卷必须先标记为标准化试卷");
            }
            // 防止循环引用
            if (parallelPaperId.equals(paperId)) {
                throw new BusinessException(400, "不能将自己设为自己的平行卷");
            }
        }

        // 验证paperRole
        if (paperRole != null && !List.of("PRETEST", "POSTTEST", "MIDTEST", "COMMON").contains(paperRole)) {
            throw new BusinessException(400, "无效的试卷角色: " + paperRole);
        }

        paper.setIsStandardized(1);
        paper.setPaperRole(paperRole);
        paper.setParallelPaperId(parallelPaperId);
        paperMapper.updateById(paper);
    }

    @Override
    @Transactional
    public void unmarkStandardized(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(404, "试卷不存在");
        if (!paper.getCreatorId().equals(SecurityUtils.getCurrentUserId()) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "无权限操作此试卷");
        }
        if (paper.getLockedAt() != null) {
            throw new BusinessException(400, "试卷已锁定，解锁后才能取消标准化标记");
        }

        paper.setIsStandardized(0);
        paper.setPaperRole(null);
        paper.setParallelPaperId(null);
        paperMapper.updateById(paper);
    }

    @Override
    @Transactional
    public void lockPaper(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(404, "试卷不存在");
        if (!paper.getCreatorId().equals(SecurityUtils.getCurrentUserId()) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "无权限操作此试卷");
        }
        if (paper.getIsStandardized() == null || paper.getIsStandardized() != 1) {
            throw new BusinessException(400, "请先标记为标准化试卷再锁定");
        }
        if (paper.getLockedAt() != null) {
            throw new BusinessException(400, "试卷已锁定");
        }

        paper.setLockedAt(java.time.LocalDateTime.now());
        paper.setLockedBy(SecurityUtils.getCurrentUserId());
        paperMapper.updateById(paper);
    }

    @Override
    @Transactional
    public void unlockPaper(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(404, "试卷不存在");
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "仅管理员可解锁标准化试卷");
        }
        if (paper.getLockedAt() == null) {
            throw new BusinessException(400, "试卷未锁定");
        }

        paper.setLockedAt(null);
        paper.setLockedBy(null);
        paperMapper.updateById(paper);
    }
}
