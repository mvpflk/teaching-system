package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AiServiceGateway;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.QuestionBankService;
import lombok.extern.slf4j.Slf4j;
import com.school.teaching.service.WordQuestionParser;
import com.school.teaching.service.ExcelQuestionParser;
import com.school.teaching.dto.ExcelQuestionImportRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    @org.springframework.context.annotation.Lazy
    @Autowired
    private QuestionBankServiceImpl self;

    @Autowired
    private QuestionBankMapper bankMapper;

    @Autowired
    private KnowledgeNodeMapper nodeMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private com.school.teaching.mapper.QuestionCompositeItemsMapper compositeItemsMapper;

    @Autowired
    private com.school.teaching.common.handler.QuestionTypeHandlerRegistry handlerRegistry;

    @Autowired
    private WordQuestionParser wordParser;

    @Autowired
    private ExcelQuestionParser excelParser;

    @Autowired
    private com.school.teaching.mapper.DictSubjectMapper dictSubjectMapper;
    @Autowired
    private com.school.teaching.mapper.QuestionEditHistoryMapper editHistoryMapper;
    @Autowired
    private com.school.teaching.mapper.UserMapper userMapper;
    @Autowired
    private com.school.teaching.service.TaskService taskService;
    @Autowired
    private com.school.teaching.mapper.TaskQuestionMapper taskQuestionMapper;
    @Autowired
    private com.school.teaching.mapper.TaskMapper taskMapper;
    @Autowired
    private com.school.teaching.service.TeacherService teacherService;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AiServiceGateway aiGateway;

    private boolean isAdminRole() {
        String role = SecurityUtils.getCurrentRole();
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role) || "INSPECTOR".equals(role);
    }

    @Override
    public QuestionBank getById(Long id) {
        return bankMapper.selectById(id);
    }

    @Override
    public List<QuestionBank> listQuestions(String subject, Long categoryId, String questionType,
                                             Integer difficultyLevel, String keyword) {
        LambdaQueryWrapper<QuestionBank> w = new LambdaQueryWrapper<>();
        w.eq(QuestionBank::getStatus, 1);
        if (StringUtils.isNotBlank(subject)) w.eq(QuestionBank::getSubject, subject);
        if (categoryId != null) {
            Set<Long> catIds = new HashSet<>();
            collectDescendantIds(categoryId, catIds);
            catIds.add(categoryId);
            w.in(QuestionBank::getCategoryId, catIds);
        }
        if (StringUtils.isNotBlank(questionType)) w.eq(QuestionBank::getQuestionType, questionType);
        if (difficultyLevel != null) w.eq(QuestionBank::getDifficultyLevel, difficultyLevel);
        if (StringUtils.isNotBlank(keyword)) {
            w.and(q -> q.like(QuestionBank::getQuestionText, keyword));
        }
        w.orderByDesc(QuestionBank::getCreateTime);
        List<QuestionBank> list = bankMapper.selectList(w);
        enrichCreatorNames(list);
        return list;
    }

    @Override
    public IPage<QuestionBank> pageQuestions(String subject, Long categoryId, String questionType,
                                              Integer difficultyLevel, String keyword,
                                              Integer status, String tier, String knowledgeDim,
                                              String source, String sort, Integer page, Integer pageSize) {
        LambdaQueryWrapper<QuestionBank> w = new LambdaQueryWrapper<>();
        if (status != null) {
            w.eq(QuestionBank::getStatus, status);
        } else {
            w.eq(QuestionBank::getStatus, 1);  // 默认只查已采用
        }
        if (StringUtils.isNotBlank(subject)) {
            w.likeRight(QuestionBank::getSubject, subject);
        } else if (!isAdminRole()) {
            List<String> teacherSubjects = getTeacherSubjects(SecurityUtils.getCurrentUserId());
            if (!teacherSubjects.isEmpty()) {
                w.and(wq -> {
                    wq.likeRight(QuestionBank::getSubject, teacherSubjects.get(0));
                    for (int i = 1; i < teacherSubjects.size(); i++)
                        wq.or().likeRight(QuestionBank::getSubject, teacherSubjects.get(i));
                });
            }
        }
        if (categoryId != null) {
            Set<Long> catIds = new HashSet<>();
            collectDescendantIds(categoryId, catIds);
            catIds.add(categoryId);
            w.in(QuestionBank::getCategoryId, catIds);
        }
        if (StringUtils.isNotBlank(questionType)) w.eq(QuestionBank::getQuestionType, questionType);
        if (difficultyLevel != null) w.eq(QuestionBank::getDifficultyLevel, difficultyLevel);
        if (StringUtils.isNotBlank(tier)) w.eq(QuestionBank::getTier, tier);
        if (StringUtils.isNotBlank(knowledgeDim)) w.eq(QuestionBank::getKnowledgeDim, knowledgeDim);
        if (StringUtils.isNotBlank(source)) w.eq(QuestionBank::getSource, source);
        if (StringUtils.isNotBlank(keyword)) {
            w.and(q -> q.like(QuestionBank::getQuestionText, keyword));
        }
        Page<QuestionBank> pageParam = new Page<>(page, pageSize);
        if ("mostUsed".equals(sort)) {
            // 子查询排序：last() 拼 ORDER BY 从句（语法合法，在 WHERE 之后）
            // 关闭分页 Count 优化：JSqlParser 无法解析 last() 中拼入的 SQL 片段
            w.last("ORDER BY (SELECT COUNT(*) FROM task_questions WHERE question_id = id) DESC, id DESC");
            pageParam.setOptimizeCountSql(false);
        } else {
            w.orderByDesc(QuestionBank::getCreateTime);
        }
        IPage<QuestionBank> pageResult = bankMapper.selectPage(pageParam, w);
        enrichCreatorNames(pageResult.getRecords());
        return pageResult;
    }

    private void enrichCreatorNames(List<QuestionBank> questions) {
        if (questions == null || questions.isEmpty()) return;
        var userIds = questions.stream().map(QuestionBank::getCreatedBy).filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) return;
        var userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(com.school.teaching.entity.User::getId, com.school.teaching.entity.User::getRealName));
        questions.forEach(q -> { if (q.getCreatedBy() != null) q.setCreatorName(userMap.getOrDefault(q.getCreatedBy(), "")); });
    }

    @Override
    public List<Map<String, Object>> getSubjects() {
        return dictSubjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getStatus, 1).orderByAsc(DictSubject::getSortOrder))
            .stream().map(s -> Map.<String,Object>of("id", s.getId(), "subjectName", s.getSubjectName())).toList();
    }

    @Override
    public QuestionBank createQuestion(Long userId, QuestionBank question) {
        question.setCreatedBy(userId);
        question.setStatus(0);
        question.setSource("MANUAL");
        question.setCreateTime(LocalDateTime.now());
        sanitizeJsonFields(question);
        bankMapper.insert(question);
        return question;
    }

    private void sanitizeJsonFields(QuestionBank q) {
        if (q.getKnowledgePoints() != null && q.getKnowledgePoints().isBlank()) q.setKnowledgePoints(null);
        if (q.getAnswerSchema() != null && q.getAnswerSchema().isBlank()) q.setAnswerSchema(null);
        if (q.getContentJson() != null && q.getContentJson().isBlank()) q.setContentJson(null);
        // TRUE_FALSE 答案归一化：T/True/对→A，F/False/错→B
        if ("TRUE_FALSE".equals(q.getQuestionType()) && q.getCorrectAnswer() != null) {
            String a = q.getCorrectAnswer().trim().toUpperCase();
            if ("T".equals(a) || "TRUE".equals(a) || "对".equals(q.getCorrectAnswer().trim()) || "正确".equals(q.getCorrectAnswer().trim()))
                q.setCorrectAnswer("A");
            else if ("F".equals(a) || "FALSE".equals(a) || "错".equals(q.getCorrectAnswer().trim()) || "错误".equals(q.getCorrectAnswer().trim()))
                q.setCorrectAnswer("B");
        }
    }

    @Override
    @Transactional
    public QuestionBank updateQuestion(Long id, QuestionBank question, Long userId, boolean isAdmin) {
        QuestionBank existing = bankMapper.selectById(id);
        if (existing == null) return null;
        if (!isAdmin && !userId.equals(existing.getCreatedBy())) {
            throw new BusinessException(403, "仅可编辑自己创建的题目");
        }
        question.setId(id);
        // 版本号递增
        if (existing.getVersion() != null) question.setVersion(existing.getVersion() + 1);
        else question.setVersion(1);
        question.setEditedByTeacher(1); // 教师手动编辑标记
        sanitizeJsonFields(question);
        bankMapper.updateById(question);

        // B1: 写入编辑历史
        try {
            QuestionEditHistory hist = new QuestionEditHistory();
            hist.setQuestionId(id);
            hist.setVersion(question.getVersion());
            hist.setBeforeSnapshot(toJson(existing));
            hist.setAfterSnapshot(toJson(question));
            hist.setEditedBy(userId);
            hist.setEditType("UPDATE");
            hist.setSchoolId(1L);
            editHistoryMapper.insert(hist);
        } catch (Exception e) {
            log.warn("编辑历史写入失败: qid={}", id, e);
        }

        // B3: 教师间通知
        try {
            notifyQuestionEdited(existing, userId, null);
        } catch (Exception e) {
            log.debug("题编辑通知失败: qid={}", id);
        }

        return question;
    }

    private String toJson(QuestionBank q) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(q);
        } catch (Exception e) { return null; }
    }

    private void notifyQuestionEdited(QuestionBank before, Long editorId, String editType) {
        Long originalCreator = before.getCreatedBy();
        if (originalCreator == null || originalCreator.equals(editorId)) return; // 自己编辑自己无需通知
        try {
            notificationService.notify(originalCreator,
                "question_edited",
                "题目更新通知",
                "教师 #" + editorId + " 修改了你创建的题目 #" + before.getId(),
                before.getId());
        } catch (Exception ignored) { log.debug("题目编辑通知失败: {}", ignored.getMessage()); }
    }

    @Override
    public void deleteQuestion(Long id, Long userId, boolean isAdmin) {
        QuestionBank q = bankMapper.selectById(id);
        if (q == null) return;
        if (!isAdmin && !userId.equals(q.getCreatedBy())) {
            throw new BusinessException(403, "仅可删除自己创建的题目");
        }
        // 检查是否被非草稿任务引用
        java.util.List<com.school.teaching.entity.TaskQuestion> refs = taskQuestionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.TaskQuestion>()
                .eq(com.school.teaching.entity.TaskQuestion::getQuestionId, id));
        if (!refs.isEmpty()) {
            java.util.Set<Long> refTaskIds = refs.stream()
                .map(com.school.teaching.entity.TaskQuestion::getTaskId)
                .collect(java.util.stream.Collectors.toSet());
            java.util.List<com.school.teaching.entity.Task> refTasks = taskMapper.selectBatchIds(refTaskIds);
            long nonDraftCount = refTasks.stream()
                .filter(t -> t != null && !"DRAFT".equals(t.getStatus()))
                .count();
            if (nonDraftCount > 0) {
                String taskTitles = refTasks.stream()
                    .filter(t -> t != null && !"DRAFT".equals(t.getStatus()))
                    .map(com.school.teaching.entity.Task::getTitle)
                    .collect(java.util.stream.Collectors.joining("、"));
                throw new BusinessException(409,
                    "该题目被以下任务使用，无法删除：" + taskTitles + "。请先从任务中移除该题目后再试。");
            }
            // 仅被草稿任务引用 → 允许删除，清理引用
            log.info("删除题目id={}，清理{}个草稿任务中的引用", id, refs.size());
            taskQuestionMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.TaskQuestion>()
                    .eq(com.school.teaching.entity.TaskQuestion::getQuestionId, id));
        }
        q.setStatus(-1); // -1=已删除，区别于0=待审核/1=已采用/2=已驳回
        bankMapper.updateById(q);
    }

    @Override
    @Transactional
    public int batchClearQuestions() {
        return bankMapper.update(null,
            new LambdaUpdateWrapper<QuestionBank>()
                .eq(QuestionBank::getStatus, 1)
                .set(QuestionBank::getStatus, -1));
    }

    @Override
    @Transactional
    public Map<String, Object> importFromWord(MultipartFile file, Long categoryId, Long userId) {
        if (file.isEmpty()) throw new BusinessException(400, "请选择文件");
        if (!file.getOriginalFilename().endsWith(".docx")) throw new BusinessException(400, "仅支持 .docx 格式");

        List<QuestionBank> questions;
        try {
            questions = wordParser.parse(file, categoryId, userId);
        } catch (IOException e) {
            throw new BusinessException(500, "解析文档失败: " + e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("imported", questions.size());
        result.put("skippedDup", wordParser.getLastSkippedDup());
        result.put("questions", questions);
        return result;
    }

    @Override
    public Map<String, Object> importFromWordBatch(List<MultipartFile> files, String mappings, Long userId) {
        // 解析 mappings JSON: [{"categoryId":123},{"categoryId":456},...] 与 files 下标对应
        List<Long> catIds = new ArrayList<>();
        if (mappings != null && !mappings.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode arr = om.readTree(mappings);
                for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                    catIds.add(n.has("categoryId") && !n.get("categoryId").isNull() ? n.get("categoryId").asLong() : null);
                }
            } catch (Exception ignored) { log.debug("解析mappings JSON失败: {}", ignored.getMessage()); }
        }
        int totalImported = 0;
        List<Map<String, Object>> fileResults = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            Long catId = i < catIds.size() ? catIds.get(i) : null;
            Map<String, Object> fr = new HashMap<>();
            fr.put("filename", f.getOriginalFilename());
            try {
                // 通过self代理调用，使每个文件拥有独立事务，互不影响
                Map<String, Object> r = self.importFromWord(f, catId, userId);
                int cnt = (int) r.get("imported");
                int skipped = (int) r.getOrDefault("skippedDup", 0);
                totalImported += cnt;
                fr.put("imported", cnt);
                fr.put("skippedDup", skipped);
                fr.put("success", true);
            } catch (Exception e) {
                fr.put("imported", 0);
                fr.put("success", false);
                fr.put("error", e.getMessage() != null ? e.getMessage() : "解析失败");
            }
            fileResults.add(fr);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("imported", totalImported);
        result.put("files", fileResults);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> importFromExcel(MultipartFile file, Long categoryId, Long userId) {
        if (file.isEmpty()) throw new BusinessException(400, "请选择文件");
        List<ExcelQuestionImportRow> rows;
        try {
            rows = excelParser.parse(file);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, e.getMessage());
        } catch (RuntimeException e) {
            throw new BusinessException(500, "解析 Excel 失败: " + e.getMessage());
        }
        if (rows.isEmpty()) throw new BusinessException(400, "未解析到有效题目");
        if (rows.size() > 500) throw new BusinessException(400, "单次最多导入500道题目，当前" + rows.size() + "道");

        List<QuestionBank> questions = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ExcelQuestionImportRow row = rows.get(i);
            if (row.getQuestionText() == null || row.getQuestionText().trim().isEmpty()) continue;
            QuestionBank q = new QuestionBank();
            q.setQuestionType(normalizeType(row.getQuestionType()));
            q.setQuestionText(row.getQuestionText().trim());
            q.setCorrectAnswer(row.getCorrectAnswer() != null ? row.getCorrectAnswer().trim() : "");
            q.setExplanation(row.getExplanation() != null ? row.getExplanation().trim() : "");
            String catSubject = categoryId != null ? getNodeTopName(categoryId) : null;
            q.setSubject(row.getSubject() != null && !row.getSubject().trim().isEmpty() ? row.getSubject().trim()
                : (catSubject != null ? catSubject : ""));
            q.setOptions(buildOptions(row));
            q.setDifficultyLevel(1);
            q.setStatus(1);
            q.setSource("EXCEL_IMPORT");
            q.setCreatedBy(userId);
            if (categoryId != null) q.setCategoryId(categoryId);
            bankMapper.insert(q);
            questions.add(q);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("imported", questions.size());
        result.put("questions", questions);
        return result;
    }

    @Override
    @Transactional
    public int addToExam(Long examId, List<Long> questionIds, Long userId, boolean isAdmin) {
        if (true) return 0; // 410 DISABLED — backup tables dropped
        if (!isAdmin) {
            Exam exam = examMapper.selectById(examId);
            if (exam == null) throw new BusinessException(404, "试卷不存在");
            if (!userId.equals(exam.getTeacherId()))
                throw new BusinessException(403, "仅可为自己创建的试卷添加题目");
        }

        LambdaQueryWrapper<ExamQuestion> sortW = new LambdaQueryWrapper<>();
        sortW.eq(ExamQuestion::getExamId, examId);
        sortW.orderByDesc(ExamQuestion::getSortOrder);
        sortW.last("LIMIT 1");
        ExamQuestion last = examQuestionMapper.selectOne(sortW);
        int nextSort = (last != null && last.getSortOrder() != null) ? last.getSortOrder() + 1 : 1;

        // 批量加载题库题目
        Map<Long, QuestionBank> qbMap = bankMapper.selectBatchIds(questionIds).stream()
            .filter(q -> q.getStatus() != null && q.getStatus() == 1)
            .collect(java.util.stream.Collectors.toMap(QuestionBank::getId, q -> q));
        // 批量查重：已有同题目的题目
        Set<String> existingTexts = examQuestionMapper.selectList(
            new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId)).stream()
            .map(eq -> eq.getQuestionType() + "::" + eq.getQuestionText()).collect(java.util.stream.Collectors.toSet());

        int added = 0;
        for (Long qid : questionIds) {
            QuestionBank q = qbMap.get(qid);
            if (q == null) continue;
            if (existingTexts.contains(q.getQuestionType() + "::" + q.getQuestionText())) continue;

            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(examId);
            eq.setQuestionType(q.getQuestionType());
            eq.setQuestionText(q.getQuestionText());
            eq.setOptions(q.getOptions());
            eq.setCorrectAnswer(q.getCorrectAnswer());
            eq.setExplanation(q.getExplanation());
            eq.setAttachmentUrl(q.getAttachmentUrl());
            eq.setDifficultyLevel(q.getDifficultyLevel());
            eq.setScore(10);
            eq.setSortOrder(nextSort++);
            examQuestionMapper.insert(eq);
            added++;
        }
        return added;
    }

    @Override
    @Transactional
    public Map<String, Object> composeExam(Map<String, Object> body, Long userId, String role) {
        validateComposeRole(role);

        String title = (String) body.get("title");
        String subject = (String) body.get("subject");
        Integer durationMinutes = body.get("durationMinutes") != null
            ? Integer.valueOf(body.get("durationMinutes").toString()) : 60;
        Integer passingScore = body.get("passingScore") != null
            ? Integer.valueOf(body.get("passingScore").toString()) : 60;
        @SuppressWarnings("unchecked")
        List<Integer> questionIds = (List<Integer>) body.get("questionIds");
        if (title == null || title.trim().isEmpty()) throw new BusinessException(400, "请输入试卷标题");
        if (questionIds == null || questionIds.isEmpty()) throw new BusinessException(400, "请选择至少一道题目");

        List<QuestionBank> selectedQuestions = loadSelectedQuestions(questionIds);
        if (selectedQuestions.isEmpty()) throw new BusinessException(400, "未找到有效题目");

        @SuppressWarnings("unchecked")
        Map<String, Integer> scorePresets = (Map<String, Integer>) body.get("scorePresets");

        long totalScore = selectedQuestions.stream()
            .mapToLong(q -> (long) scoreFromPresets(q.getQuestionType(), scorePresets)).sum();

        // 创建 Task 替代旧的 backup_exam
        List<Long> qidLongs = questionIds.stream().map(Integer::longValue).toList();
        Task task = createTaskFromCompose(title, subject, userId, body,
            (int) totalScore, passingScore, durationMinutes, qidLongs, scorePresets);

        return buildComposeResult(task, qidLongs.size());
    }

    private void validateComposeRole(String role) {
        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role)
            && !"TEACHER".equals(role) && !"HEAD_TEACHER".equals(role)) {
            throw new BusinessException(403, "仅教师可创建试卷");
        }
    }

    private List<QuestionBank> loadSelectedQuestions(List<Integer> questionIds) {
        List<Long> qidLongs = questionIds.stream().map(Integer::longValue).toList();
        Map<Long, QuestionBank> qbMap = bankMapper.selectBatchIds(qidLongs).stream()
            .filter(q -> q.getStatus() != null && q.getStatus() == 1)
            .collect(java.util.stream.Collectors.toMap(QuestionBank::getId, q -> q));
        List<QuestionBank> selected = new ArrayList<>();
        for (Integer qid : questionIds) {
            QuestionBank q = qbMap.get(qid.longValue());
            if (q != null) selected.add(q);
        }
        return selected;
    }

    private Task createTaskFromCompose(String title, String subject, Long userId, Map<String, Object> body,
                                        int totalScore, int passingScore, int durationMinutes,
                                        List<Long> questionIds, Map<String, Integer> scorePresets) {
        Task task = new Task();
        task.setTitle(title);
        task.setSubject(subject);
        task.setTaskType("SUMMATIVE");
        task.setScoreType("SCORE_100");
        task.setTotalScore(BigDecimal.valueOf(totalScore));
        task.setTargetType("CLASS");
        if (body.get("classId") != null) {
            task.setTargetId(Long.valueOf(body.get("classId").toString()));
        }
        task.setTaskConfig("{\"durationMinutes\":" + durationMinutes + ",\"passingScore\":" + passingScore + "}");
        task.setQuestionIds(questionIds);
        task.setScorePresets(scorePresets);
        return taskService.create(task);
    }

    private Map<String, Object> buildComposeResult(Task task, int questionCount) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("title", task.getTitle());
        result.put("totalScore", task.getTotalScore());
        result.put("questionCount", questionCount);
        return result;
    }

    private int getDefaultScore(String questionType) {
        return switch (questionType) {
            case "SINGLE_CHOICE" -> 2;
            case "MULTI_CHOICE" -> 3;
            case "TRUE_FALSE", "FILL_IN" -> 1;
            default -> 10;
        };
    }

    private static int scoreFromPresets(String questionType, Map<String, Integer> presets) {
        if (presets != null && presets.containsKey(questionType)) return presets.get(questionType);
        return getDefaultScoreStatic(questionType);
    }

    private static int getDefaultScoreStatic(String questionType) {
        return switch (questionType) {
            case "SINGLE_CHOICE" -> 2;
            case "MULTI_CHOICE" -> 3;
            case "TRUE_FALSE", "FILL_IN" -> 1;
            default -> 10;
        };
    }

    private final Map<Long, KnowledgeNode> nodeCache = new java.util.concurrent.ConcurrentHashMap<>();

    private KnowledgeNode getNodeCached(Long id) {
        return nodeCache.computeIfAbsent(id, nodeMapper::selectById);
    }

    private String getNodeTopName(Long categoryId) {
        if (categoryId == null) return null;
        Long cur = categoryId; KnowledgeNode top = null;
        int d = 0;
        while (cur != null && d < 5) {
            KnowledgeNode cat = getNodeCached(cur);
            if (cat == null) break;
            top = cat; cur = cat.getParentId(); d++;
        }
        return top != null ? top.getName() : null;
    }

    private String getNodeFullPath(Long categoryId) {
        if (categoryId == null) return null;
        List<String> parts = new ArrayList<>();
        Long currentId = categoryId;
        int depth = 0;
        while (currentId != null && depth < 5) {
            KnowledgeNode cat = getNodeCached(currentId);
            if (cat == null) break;
            parts.add(0, cat.getName());
            currentId = cat.getParentId();
            depth++;
        }
        return parts.isEmpty() ? null : String.join(" > ", parts);
    }

    private void collectDescendantIds(Long parentId, Set<Long> result) {
        List<KnowledgeNode> children = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>().eq(KnowledgeNode::getParentId, parentId));
        for (KnowledgeNode child : children) {
            result.add(child.getId());
            collectDescendantIds(child.getId(), result);
        }
    }

    /** 将 Excel 中的题型中文名称映射为枚举值 */
    private String normalizeType(String raw) {
        if (raw == null) return "SINGLE_CHOICE";
        String t = raw.trim().toUpperCase();
        if (t.contains("多选") || t.equals("MULTI_CHOICE") || t.equals("MULTI")) return "MULTI_CHOICE";
        if (t.contains("判断") || t.equals("TRUE_FALSE") || t.equals("TF") || t.equals("T/F")) return "TRUE_FALSE";
        if (t.contains("填空") || t.equals("FILL_IN") || t.equals("FILL")) return "FILL_IN";
        if (t.contains("简答") || t.equals("SHORT_ANSWER") || t.equals("SHORT")) return "SHORT_ANSWER";
        if (t.contains("综合") || t.equals("COMPOSITE")) return "COMPOSITE";
        return "SINGLE_CHOICE";
    }

    /** 将选项列组装为 JSON 数组字符串 */
    private String buildOptions(ExcelQuestionImportRow row) {
        List<String> opts = new ArrayList<>();
        addOpt(opts, row.getOptionA());
        addOpt(opts, row.getOptionB());
        addOpt(opts, row.getOptionC());
        addOpt(opts, row.getOptionD());
        addOpt(opts, row.getOptionE());
        if (opts.isEmpty()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(opts);
        } catch (Exception e) {
            log.warn("选项JSON序列化失败", e);
            return "[]";
        }
    }

    private void addOpt(List<String> opts, String val) {
        if (val != null && !val.trim().isEmpty()) opts.add(val.trim());
    }

    @Override
    public List<Map<String, Object>> getCompositeChildren(Long parentQuestionId) {
        // 1. 一次查询全部关联记录（批量加载，防 N+1）
        List<com.school.teaching.entity.QuestionCompositeItems> links = compositeItemsMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.QuestionCompositeItems>()
                .eq(com.school.teaching.entity.QuestionCompositeItems::getParentQuestionId, parentQuestionId)
                .orderByAsc(com.school.teaching.entity.QuestionCompositeItems::getSortOrder));

        if (links.isEmpty()) return Collections.emptyList();

        // 2. 批量加载所有子题（一次查询，防 N+1）
        Set<Long> childIds = links.stream()
            .map(com.school.teaching.entity.QuestionCompositeItems::getChildQuestionId)
            .collect(Collectors.toSet());
        Map<Long, QuestionBank> childMap = bankMapper.selectBatchIds(childIds).stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q));

        // 3. 内存组装（无 DB 调用）
        List<Map<String, Object>> result = new ArrayList<>();
        for (com.school.teaching.entity.QuestionCompositeItems link : links) {
            QuestionBank child = childMap.get(link.getChildQuestionId());
            if (child == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", child.getId());
            item.put("sortOrder", link.getSortOrder());
            item.put("questionType", child.getQuestionType());
            item.put("questionText", child.getQuestionText());
            item.put("options", child.getOptions());
            item.put("score", null); // 子题分值由 task_questions 决定
            result.add(item);
        }
        return result;
    }

    @Override
    public com.school.teaching.common.QuestionTypeHandler getHandler(com.school.teaching.common.QuestionTypeEnum type) {
        return handlerRegistry.get(type);
    }

    @Override
    public List<Map<String, Object>> matchQuestions(List<Map<String, Object>> knowledgePoints, List<Long> excludeIds) {
        List<Map<String, Object>> results = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>(excludeIds != null ? excludeIds : List.of());

        // 题型映射：前端 → 数据库
        Map<String, String> typeMap = Map.of(
            "SINGLE_CHOICE","SINGLE_CHOICE","MULTI_CHOICE","MULTI_CHOICE",
            "TRUE_FALSE","TRUE_FALSE","FILL_IN","FILL_IN");

        for (Map<String, Object> kp : knowledgePoints) {
            Long categoryId = kp.get("categoryId") instanceof Number n ? n.longValue() : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> typeCounts = kp.get("typeCounts") instanceof Map<?,?> m
                ? (Map<String, Object>) m : Map.of();

            for (Map.Entry<String, Object> tc : typeCounts.entrySet()) {
                String qType = typeMap.getOrDefault(tc.getKey(), tc.getKey());
                int need = tc.getValue() instanceof Number n ? n.intValue() : 0;
                if (need <= 0 || categoryId == null) continue;

                // 收集categoryId及其所有后代节点，确保上层节点也能匹配到叶子节点下的题目
                Set<Long> catIds = new HashSet<>();
                collectDescendantIds(categoryId, catIds);
                catIds.add(categoryId);

                LambdaQueryWrapper<QuestionBank> qw = new LambdaQueryWrapper<QuestionBank>()
                    .in(QuestionBank::getCategoryId, catIds)
                    .eq(QuestionBank::getQuestionType, qType)
                    .eq(QuestionBank::getStatus, 1);
                if (!usedIds.isEmpty()) qw.notIn(QuestionBank::getId, usedIds);
                qw.last("LIMIT " + Math.min(Math.max(need, 1), 500));
                List<QuestionBank> bank = bankMapper.selectList(qw);
                for (QuestionBank q : bank) {
                    usedIds.add(q.getId());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", q.getId());
                    item.put("questionText", q.getQuestionText());
                    item.put("questionType", q.getQuestionType());
                    item.put("correctAnswer", q.getCorrectAnswer());
                    item.put("options", q.getOptions());
                    item.put("explanation", q.getExplanation());
                    item.put("difficultyLevel", q.getDifficultyLevel());
                    item.put("categoryId", q.getCategoryId());
                    item.put("source", "bank");
                    item.put("_kpIdx", kp.get("_idx"));
                    results.add(item);
                }
            }
        }
        return results;
    }

    private List<String> getTeacherSubjects(Long userId) {
        if (userId == null) return List.of();
        return teacherService.getTeachingSubjectsWithIds(userId).stream()
            .map(m -> (String) m.get("subjectName")).filter(s -> s != null && !s.isEmpty()).toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // AI审核 + 批量操作
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public Map<String, Object> aiReview(List<Long> questionIds, boolean autoApprove, Long userId) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(400, "请选择至少一道题目");
        }
        if (questionIds.size() > 50) {
            throw new BusinessException(400, "单次最多审核50道题目");
        }

        // 1. 批量加载待审核题目
        List<QuestionBank> questions = bankMapper.selectBatchIds(questionIds).stream()
            .filter(q -> q.getStatus() != null && q.getStatus() == 0)
            .toList();

        if (questions.isEmpty()) {
            throw new BusinessException(400, "所选题目中没有待审核的题目");
        }

        // 2. 构建审核Prompt
        String prompt = buildReviewPrompt(questions);
        Map<String, Object> aiParams = new LinkedHashMap<>();
        aiParams.put("prompt", prompt);
        aiParams.put("maxTokens", 3000);
        aiParams.put("temperature", 0.3); // 低温度保证一致性

        // 3. 调用AI审核
        String aiResponse;
        try {
            aiResponse = aiGateway.generateContent(aiParams);
        } catch (Exception e) {
            log.error("AI审核调用失败", e);
            throw new BusinessException(500, "AI审核服务暂不可用: " + e.getMessage());
        }

        // 4. 解析AI返回的JSON
        List<Map<String, Object>> results = parseReviewResponse(aiResponse, questions);

        // 5. 统计 + 自动通过
        int approved = 0, needsFix = 0, rejected = 0;
        for (Map<String, Object> r : results) {
            String verdict = (String) r.get("verdict");
            if ("APPROVED".equals(verdict)) {
                approved++;
                if (autoApprove) {
                    Long qid = ((Number) r.get("id")).longValue();
                    doApproveSingle(qid);
                }
            } else if ("REJECTED".equals(verdict)) {
                rejected++;
            } else {
                needsFix++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", results.size());
        result.put("approved", approved);
        result.put("needsFix", needsFix);
        result.put("rejected", rejected);
        result.put("autoApproved", autoApprove ? approved : 0);
        result.put("results", results);
        return result;
    }

    /** 构建AI审核Prompt */
    private String buildReviewPrompt(List<QuestionBank> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是中职教学题库审核专家。请审核以下").append(questions.size()).append("道题目，对每道题从四个维度评估：\n");
        sb.append("1. 题干清晰度：题目表述是否清晰无歧义\n");
        sb.append("2. 答案正确性：正确答案是否确实正确\n");
        sb.append("3. 解析合理性：解析是否准确、有帮助\n");
        sb.append("4. 题型匹配度：题型标签是否与题目实际形式匹配\n\n");
        sb.append("审核结论：APPROVED（通过，可直接入库）/ NEEDS_FIX（需人工修改）/ REJECTED（建议驳回，质量太差）\n\n");
        sb.append("请严格返回以下JSON数组格式（不要包含其他文字）：\n");
        sb.append("[{\"index\":0,\"verdict\":\"APPROVED\",\"reason\":\"审核理由简述\"}]\n\n");
        sb.append("待审核题目列表：\n\n");

        for (int i = 0; i < questions.size(); i++) {
            QuestionBank q = questions.get(i);
            sb.append("--- 题目").append(i + 1).append(" ---\n");
            sb.append("index: ").append(i).append("\n");
            sb.append("题型: ").append(q.getQuestionType()).append("\n");
            sb.append("题干: ").append(q.getQuestionText()).append("\n");
            if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                sb.append("选项: ").append(formatOptions(q.getOptions(), q.getQuestionType())).append("\n");
            }
            if (q.getCorrectAnswer() != null && !q.getCorrectAnswer().isEmpty()) {
                sb.append("答案: ").append(q.getCorrectAnswer()).append("\n");
            }
            if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
                sb.append("解析: ").append(q.getExplanation()).append("\n");
            }
            sb.append("学科: ").append(q.getSubject() != null ? q.getSubject() : "未指定").append("\n\n");
        }
        return sb.toString();
    }

    /** 格式化选项为可读文本 */
    private String formatOptions(String optionsJson, String questionType) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<?> opts = om.readValue(optionsJson, List.class);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < opts.size(); i++) {
                if (i > 0) sb.append("; ");
                char letter = (char) ('A' + i);
                sb.append(letter).append(". ").append(opts.get(i));
            }
            return sb.toString();
        } catch (Exception e) {
            return optionsJson;
        }
    }

    /** 解析AI审核响应 */
    private List<Map<String, Object>> parseReviewResponse(String aiResponse, List<QuestionBank> questions) {
        List<Map<String, Object>> results = new ArrayList<>();
        // 构建index→question的映射
        Map<Integer, QuestionBank> indexMap = new LinkedHashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            indexMap.put(i, questions.get(i));
        }

        try {
            // 尝试提取JSON数组
            String json = aiResponse.trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> parsed = om.readValue(json,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

            for (Map<String, Object> item : parsed) {
                int idx = item.get("index") instanceof Number n ? n.intValue() : -1;
                QuestionBank q = indexMap.get(idx);
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("index", idx);
                if (q != null) {
                    r.put("id", q.getId());
                    r.put("questionText", q.getQuestionText() != null && q.getQuestionText().length() > 60
                        ? q.getQuestionText().substring(0, 60) + "..." : q.getQuestionText());
                    r.put("questionType", q.getQuestionType());
                }
                String verdict = (String) item.getOrDefault("verdict", "NEEDS_FIX");
                // 规范化verdict
                verdict = verdict.toUpperCase().trim();
                if (!"APPROVED".equals(verdict) && !"REJECTED".equals(verdict)) {
                    verdict = "NEEDS_FIX";
                }
                r.put("verdict", verdict);
                r.put("reason", item.getOrDefault("reason", "AI未给出具体理由"));
                results.add(r);
            }
        } catch (Exception e) {
            log.warn("AI审核响应解析失败，原始响应: {}", aiResponse, e);
            // 解析失败时全部标记为NEEDS_FIX
            for (int i = 0; i < questions.size(); i++) {
                QuestionBank q = questions.get(i);
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("index", i);
                r.put("id", q.getId());
                r.put("questionText", q.getQuestionText() != null && q.getQuestionText().length() > 60
                    ? q.getQuestionText().substring(0, 60) + "..." : q.getQuestionText());
                r.put("questionType", q.getQuestionType());
                r.put("verdict", "NEEDS_FIX");
                r.put("reason", "AI审核响应解析失败，请人工审核");
                results.add(r);
            }
        }
        return results;
    }

    /** 单题审核通过（不推送课堂抽问题库，批量操作简化） */
    private void doApproveSingle(Long questionId) {
        QuestionBank q = bankMapper.selectById(questionId);
        if (q != null && q.getStatus() != null && q.getStatus() == 0) {
            q.setStatus(1);
            bankMapper.updateById(q);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> batchApprove(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(400, "请选择至少一道题目");
        }

        int approved = bankMapper.update(null,
            new LambdaUpdateWrapper<QuestionBank>()
                .in(QuestionBank::getId, questionIds)
                .eq(QuestionBank::getStatus, 0)
                .set(QuestionBank::getStatus, 1));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approved", approved);
        result.put("failed", questionIds.size() - approved);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> batchReject(List<Long> questionIds, Long userId) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(400, "请选择至少一道题目");
        }

        int rejected = bankMapper.update(null,
            new LambdaUpdateWrapper<QuestionBank>()
                .in(QuestionBank::getId, questionIds)
                .eq(QuestionBank::getStatus, 0)
                .set(QuestionBank::getStatus, 2));

        // 批量写入编辑历史
        for (Long qid : questionIds) {
            try {
                com.school.teaching.entity.QuestionEditHistory hist = new com.school.teaching.entity.QuestionEditHistory();
                hist.setQuestionId(qid);
                hist.setVersion(1);
                hist.setChangeSummary("批量驳回");
                hist.setEditedBy(userId);
                hist.setEditType("REJECT");
                hist.setSchoolId(1L);
                editHistoryMapper.insert(hist);
            } catch (Exception e) {
                log.warn("批量驳回历史写入失败: qid={}", qid, e);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rejected", rejected);
        result.put("failed", questionIds.size() - rejected);
        return result;
    }

    @Override
    public List<QuestionBank> listByBatchId(String batchId, Long teacherId) {
        return bankMapper.selectList(
            new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getCreatedBy, teacherId)
                .ne(QuestionBank::getStatus, -1)  // 排除已删除
                // 使用 JSON_EXTRACT 精确匹配 batchId，避免 LIKE 模糊匹配到错误记录
                .apply("JSON_EXTRACT(content_json, '$.batchId') = {0}", batchId));
    }

    @Override
    public List<QuestionBank> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.List.of();
        if (ids.size() > 200) throw new BusinessException(400, "单次最多查询 200 道题");
        return bankMapper.selectBatchIds(ids).stream()
            .filter(q -> q.getStatus() == null || q.getStatus() != -1)
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.Map<Long, Long> usageStats(java.util.List<Long> questionIds) {
        java.util.Map<Long, Long> result = new java.util.HashMap<>();
        if (questionIds == null || questionIds.isEmpty()) return result;
        questionIds.forEach(id -> result.put(id, 0L));
        if (taskQuestionMapper == null) return result;
        taskQuestionMapper.selectList(new LambdaQueryWrapper<com.school.teaching.entity.TaskQuestion>()
                .select(TaskQuestion::getQuestionId)
                .in(TaskQuestion::getQuestionId, questionIds))
            .forEach(tq -> result.merge(tq.getQuestionId(), 1L, Long::sum));
        return result;
    }
}
