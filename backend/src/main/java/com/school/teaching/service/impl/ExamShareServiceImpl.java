package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ExamShareService;
import com.school.teaching.service.TaskService;
import com.school.teaching.service.WordQuestionParser;
import com.school.teaching.service.ExcelQuestionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamShareServiceImpl implements ExamShareService {

    @Autowired private ExamShareMapper shareMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private TaskQuestionMapper taskQuestionMapper;
    @Autowired private com.school.teaching.mapper.QuestionBankMapper questionBankMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private TaskService taskService;
    @Autowired private WordQuestionParser wordParser;
    @Autowired private ExcelQuestionParser excelParser;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public Map<String, Object> createShare(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "试卷(任务)不存在");

        ExamShare existing = shareMapper.selectOne(
            new LambdaQueryWrapper<ExamShare>().eq(ExamShare::getTaskId, taskId));
        if (existing != null) {
            boolean expired = existing.getExpiresAt() != null && LocalDateTime.now().isAfter(existing.getExpiresAt());
            return Map.of("shareCode", existing.getShareCode(),
                "examTitle", existing.getExamTitle(),
                "creatorName", existing.getCreatorName() != null ? existing.getCreatorName() : "教师",
                "useCount", existing.getUseCount(),
                "maxUses", existing.getMaxUses(),
                "expired", expired,
                "expiresAt", existing.getExpiresAt() != null ? existing.getExpiresAt().toString() : "");
        }

        User u = userMapper.selectById(userId);
        String creatorName = u != null ? u.getRealName() : "教师";

        long qCount = taskQuestionMapper.selectCount(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));

        String code = null;
        int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            code = sb.toString();
            if (shareMapper.selectCount(new LambdaQueryWrapper<ExamShare>().eq(ExamShare::getShareCode, code)) == 0)
                break;
            if (attempt == maxAttempts - 1) throw new BusinessException(500, "生成分享码失败，请重试");
        }

        ExamShare share = new ExamShare();
        share.setShareCode(code);
        share.setTaskId(taskId);
        share.setTaskType(task.getTaskType());
        share.setCreatorId(userId);
        share.setCreatorName(creatorName);
        share.setExamTitle(task.getTitle());
        share.setExamSubject(task.getSubject());
        share.setQuestionCount((int) qCount);
        share.setCreatedAt(LocalDateTime.now());
        share.setExpiresAt(LocalDateTime.now().plusDays(7));
        share.setMaxUses(10);
        share.setUseCount(0);
        shareMapper.insert(share);
        return Map.of("shareCode", code, "examTitle", task.getTitle(),
            "creatorName", creatorName,
            "expiresAt", share.getExpiresAt().toString());
    }

    @Override
    public List<Map<String, Object>> myShares(Long userId) {
        List<ExamShare> shares = shareMapper.selectList(new LambdaQueryWrapper<ExamShare>()
            .eq(ExamShare::getCreatorId, userId).orderByDesc(ExamShare::getCreatedAt));
        return enrichShares(shares);
    }

    @Override
    public void deleteShare(Long shareId, Long userId) {
        ExamShare share = shareMapper.selectById(shareId);
        if (share == null) throw new BusinessException(404, "分享不存在");
        if (!share.getCreatorId().equals(userId) && !SecurityUtils.isAdmin())
            throw new BusinessException(403, "无权删除");
        shareMapper.deleteById(shareId);
    }

    @Override @Transactional
    public Map<String, Object> importShared(String shareCode, Long userId, Long targetClassId) {
        ExamShare share = shareMapper.selectOne(
            new LambdaQueryWrapper<ExamShare>().eq(ExamShare::getShareCode, shareCode));
        if (share == null) throw new BusinessException(400, "分享码无效");
        if (share.getExpiresAt() != null && LocalDateTime.now().isAfter(share.getExpiresAt()))
            throw new BusinessException(400, "分享码已过期");
        if (share.getUseCount() >= share.getMaxUses())
            throw new BusinessException(400, "已达最大使用次数");
        if (targetClassId == null)
            throw new BusinessException(400, "请选择目标班级");

        List<Long> questionIds;
        String taskType = share.getTaskType() != null ? share.getTaskType() : "SUMMATIVE";
        Task originalTask = null;

        if (share.getTaskId() != null) {
            originalTask = taskMapper.selectById(share.getTaskId());
            if (originalTask == null) throw new BusinessException(404, "原试卷(任务)不存在");
            taskType = originalTask.getTaskType();
            List<TaskQuestion> tqs = taskQuestionMapper.selectList(
                new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, share.getTaskId())
                    .orderByAsc(TaskQuestion::getSortOrder));
            questionIds = tqs.stream().map(TaskQuestion::getQuestionId).toList();
        } else if (share.getExamId() != null) {
            throw new BusinessException(400, "该分享来自旧版试卷，暂不支持导入，请联系分享者重新分享");
        } else {
            throw new BusinessException(400, "分享数据异常");
        }

        String newTitle = share.getExamTitle().replace("(来自分享)", "").trim() + "(来自分享)";

        // 复制原始任务的关键配置
        BigDecimal originalTotalScore = originalTask != null && originalTask.getTotalScore() != null
            ? originalTask.getTotalScore() : BigDecimal.valueOf(100);
        String originalScoreType = originalTask != null && originalTask.getScoreType() != null
            ? originalTask.getScoreType() : "SCORE_100";
        String originalTaskConfig = originalTask != null && originalTask.getTaskConfig() != null
            ? originalTask.getTaskConfig() : "{\"passingScore\":60}";
        Map<String, Integer> originalScorePresets = originalTask != null && originalTask.getScorePresets() != null
            ? originalTask.getScorePresets() : null;

        Task newTask = new Task();
        newTask.setTitle(newTitle);
        newTask.setSubject(share.getExamSubject());
        newTask.setTaskType(taskType);
        newTask.setScoreType(originalScoreType);
        newTask.setTotalScore(originalTotalScore);
        newTask.setTargetType("CLASS");
        newTask.setTargetId(targetClassId);
        newTask.setTaskConfig(originalTaskConfig);
        newTask.setQuestionIds(questionIds);
        newTask.setScorePresets(originalScorePresets);
        newTask = taskService.create(newTask);

        int copied = questionIds != null ? questionIds.size() : 0;
        share.setUseCount(share.getUseCount() + 1);
        // 乐观锁：仅当 use_count < max_uses 时更新，防止并发超限
        int updated = shareMapper.update(null,
            new LambdaUpdateWrapper<ExamShare>()
                .eq(ExamShare::getId, share.getId())
                .lt(ExamShare::getUseCount, share.getMaxUses())
                .setSql("use_count = use_count + 1"));
        if (updated == 0) throw new BusinessException(400, "分享码已被他人抢先使用，请稍后重试");
        return Map.of("taskId", newTask.getId(), "title", newTask.getTitle(),
            "copied", copied, "questionCount", copied);
    }

    @Override
    public Map<String, Object> previewShare(String shareCode) {
        ExamShare share = shareMapper.selectOne(
            new LambdaQueryWrapper<ExamShare>().eq(ExamShare::getShareCode, shareCode));
        if (share == null) throw new BusinessException(400, "分享码无效");
        if (share.getExpiresAt() != null && LocalDateTime.now().isAfter(share.getExpiresAt()))
            throw new BusinessException(400, "分享码已过期");

        List<Map<String, Object>> questions = List.of();
        if (share.getTaskId() != null) {
            List<TaskQuestion> tqs = taskQuestionMapper.selectList(
                new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, share.getTaskId())
                    .orderByAsc(TaskQuestion::getSortOrder));
            var qids = tqs.stream().map(TaskQuestion::getQuestionId).toList();
            if (!qids.isEmpty()) {
                var qmap = questionBankMapper.selectBatchIds(qids).stream()
                    .collect(Collectors.toMap(com.school.teaching.entity.QuestionBank::getId, q -> q));
                questions = qids.stream().map(id -> {
                    var q = qmap.get(id); if (q == null) return null;
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("id", q.getId()); item.put("questionText", q.getQuestionText());
                    item.put("questionType", q.getQuestionType());
                    // 从 tqs 中查找对应分值
                    BigDecimal qScore = BigDecimal.ONE;
                    for (TaskQuestion tq_ : tqs) {
                        if (tq_.getQuestionId().equals(q.getId()) && tq_.getScore() != null) {
                            qScore = tq_.getScore(); break;
                        }
                    }
                    item.put("score", qScore);
                    return item;
                }).filter(Objects::nonNull).toList();
            }
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("title", share.getExamTitle()); result.put("subject", share.getExamSubject());
        result.put("questionCount", questions.size()); result.put("questions", questions);
        return result;
    }

    @Override @Transactional
    public Map<String, Object> uploadExam(MultipartFile file, String title, String subject, Long userId) {
        if (file.isEmpty()) throw new BusinessException(400, "请选择文件");
        String fn = file.getOriginalFilename();
        if (fn == null || (!fn.endsWith(".docx") && !fn.endsWith(".xlsx")))
            throw new BusinessException(400, "仅支持 .docx 或 .xlsx 格式");

        List<com.school.teaching.entity.QuestionBank> questions;
        try {
            if (fn.endsWith(".docx")) {
                questions = wordParser.parse(file, null, null);
                for (var q : questions) {
                    q.setStatus(0);
                    q.setCreatedBy(userId);
                    q.setCreateTime(LocalDateTime.now());
                    questionBankMapper.insert(q);
                }
            } else {
                var excelRows = excelParser.parse(file);
                questions = excelRows.stream().map(r -> {
                    var q = new com.school.teaching.entity.QuestionBank();
                    q.setQuestionType(r.getQuestionType() != null ? r.getQuestionType() : "SINGLE_CHOICE");
                    q.setQuestionText(r.getQuestionText());
                    var opts = new java.util.ArrayList<String>();
                    if (r.getOptionA() != null && !r.getOptionA().isBlank()) opts.add(r.getOptionA().trim());
                    if (r.getOptionB() != null && !r.getOptionB().isBlank()) opts.add(r.getOptionB().trim());
                    if (r.getOptionC() != null && !r.getOptionC().isBlank()) opts.add(r.getOptionC().trim());
                    if (r.getOptionD() != null && !r.getOptionD().isBlank()) opts.add(r.getOptionD().trim());
                    if (r.getOptionE() != null && !r.getOptionE().isBlank()) opts.add(r.getOptionE().trim());
                    q.setOptions(com.school.teaching.utils.JsonUtils.toJson(opts));
                    q.setCorrectAnswer(r.getCorrectAnswer());
                    q.setExplanation(r.getExplanation());
                    q.setSubject(r.getSubject());
                    q.setCreatedBy(userId); q.setStatus(0);  // E6: 统一为草稿状态（与 Word 导入一致）
                    q.setCreateTime(LocalDateTime.now());
                    questionBankMapper.insert(q);
                    return q;
                }).toList();
            }
        } catch (IOException e) { throw new BusinessException(500, "解析文件失败"); }
        if (questions.isEmpty()) throw new BusinessException(400, "未识别到有效题目");
        var qids = questions.stream().map(com.school.teaching.entity.QuestionBank::getId).toList();

        // 统计题型
        var typeCounts = new java.util.LinkedHashMap<String, Long>();
        for (var q : questions) {
            String t = q.getQuestionType() != null ? q.getQuestionType() : "UNKNOWN";
            typeCounts.merge(t, 1L, Long::sum);
        }
        // 返回题目列表，不创建任务，等前端赋分后确认
        var questionList = questions.stream().map(q -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", q.getId()); item.put("questionType", q.getQuestionType());
            item.put("questionText", q.getQuestionText() != null && q.getQuestionText().length() > 60
                ? q.getQuestionText().substring(0, 60) + "..." : q.getQuestionText());
            return item;
        }).toList();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("questionCount", questions.size()); result.put("typeCounts", typeCounts);
        result.put("questions", questionList); result.put("status", "parsed");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> confirmUpload(String title, String subject, Long userId, Long targetClassId,
                                              Map<Long, java.math.BigDecimal> scores) {
        var qids = new ArrayList<>(scores.keySet());
        if (qids.isEmpty()) throw new BusinessException(400, "题目列表为空");
        java.math.BigDecimal totalScore = scores.values().stream().reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        com.school.teaching.entity.Task task = new com.school.teaching.entity.Task();
        task.setTitle(title); task.setSubject(subject);
        task.setTaskType("SUMMATIVE"); task.setScoreType("SCORE_100");
        task.setTotalScore(totalScore); task.setTargetType("CLASS");
        task.setTargetId(targetClassId); task.setQuestionIds(qids);
        // 设置描述：题型分布
        task.setDescription("已上传试卷，共" + qids.size() + "题，总分" + totalScore + "分。发布后可编辑题目。");
        task = taskService.create(task);
        // 设置每题分值
        for (var e : scores.entrySet()) {
            var updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.school.teaching.entity.TaskQuestion>()
                .eq(com.school.teaching.entity.TaskQuestion::getTaskId, task.getId())
                .eq(com.school.teaching.entity.TaskQuestion::getQuestionId, e.getKey())
                .set(com.school.teaching.entity.TaskQuestion::getScore, e.getValue());
            taskQuestionMapper.update(null, updateWrapper);
        }
        // 不自动分享——教师从任务列表发布后手动分享
        return Map.of("taskId", task.getId(), "totalScore", totalScore, "questionCount", qids.size(), "status", "DRAFT");
    }

    @Override
    public List<Map<String, Object>> library(Long userId) {
        List<ExamShare> shares = shareMapper.selectList(new LambdaQueryWrapper<ExamShare>()
            .ne(ExamShare::getCreatorId, userId).orderByDesc(ExamShare::getCreatedAt));
        return enrichShares(shares);
    }

    private List<Map<String, Object>> enrichShares(List<ExamShare> shares) {
        if (shares.isEmpty()) return List.of();
        Set<Long> taskIds = shares.stream().map(ExamShare::getTaskId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Task> taskMap = taskIds.isEmpty() ? Map.of() :
            taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        // 旧格式 examId 兼容 — 可展示基本信息但没有 grade
        return shares.stream()
            .map(s -> itemFromShare(s, taskMap))
            .collect(Collectors.toList());
    }

    private Map<String, Object> itemFromShare(ExamShare s, Map<Long, Task> taskMap) {
        boolean expired = s.getExpiresAt() != null && LocalDateTime.now().isAfter(s.getExpiresAt());
        Map<String, Object> item = new HashMap<>();
        item.put("id", s.getId());
        item.put("shareCode", s.getShareCode());
        item.put("examTitle", s.getExamTitle());
        item.put("examSubject", s.getExamSubject());
        item.put("creatorName", s.getCreatorName());
        item.put("questionCount", s.getQuestionCount());
        item.put("useCount", s.getUseCount());
        item.put("maxUses", s.getMaxUses());
        item.put("createdAt", s.getCreatedAt());
        item.put("expiresAt", s.getExpiresAt());
        item.put("expired", expired);
        item.put("taskId", s.getTaskId());
        item.put("grade", "");
        return item;
    }
}
