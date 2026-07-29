package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.common.ExamTaskHandler;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AnswerSheetOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerSheetOcrServiceImpl implements AnswerSheetOcrService {

    private final AnswerSheetOcrMapper ocrMapper;
    private final TaskMapper taskMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final QuestionBankMapper questionMapper;
    private final DeepSeekGateway deepSeekGateway;
    private final TaskSubmissionMapper submissionMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final PrecisionProgressMapper progressMapper;

    @Value("${teaching.upload-dir:uploads}")
    private String uploadDir;

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public Map<String, Object> ocrSheet(Long taskId, Long studentId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(400, "请选择答题卡照片");
        if (file.getSize() > 10 * 1024 * 1024) throw new BusinessException(400, "照片大小不能超过10MB");

        String ext = getExtension(file.getOriginalFilename());
        if (!Set.of("jpg", "jpeg", "png", "webp").contains(ext)) {
            throw new BusinessException(400, "仅支持 JPG/PNG/WebP 格式");
        }

        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        // 1. 保存照片（IO操作，不涉及事务）
        String photoPath = savePhoto(file, taskId, ext);

        // 2. 调用 Vision API 识别（耗时5-30秒，避免在事务内）
        String ocrText = "";
        double confidence = 0.0;
        List<Map<String, Object>> parsedAnswers = new ArrayList<>();

        if (deepSeekGateway != null) {
            try {
                byte[] bytes = file.getBytes();
                String base64 = Base64.getEncoder().encodeToString(bytes);

                List<TaskQuestion> questions = taskQuestionMapper.selectList(
                    new LambdaQueryWrapper<TaskQuestion>()
                        .eq(TaskQuestion::getTaskId, taskId)
                        .orderByAsc(TaskQuestion::getSortOrder));

                String prompt = buildOcrPrompt(task, questions);

                Map<String, Object> visionResult = deepSeekGateway.callVision(
                    List.of(base64), prompt,
                    Map.of("temperature", 0.1));

                if (visionResult != null) {
                    ocrText = String.valueOf(visionResult.getOrDefault("content", ""));
                    parsedAnswers = parseOcrResult(ocrText, questions);
                    confidence = calculateConfidence(parsedAnswers);
                }
            } catch (Exception e) {
                log.warn("答题卡OCR识别失败 taskId={}: {}", taskId, e.getMessage());
                ocrText = "AI识别失败: " + e.getMessage();
            }
        } else {
            ocrText = "AI服务未配置";
        }

        // 3. 自动判分
        Map<String, Object> autoGrade = autoGrade(taskId, parsedAnswers);

        // 4. 写入DB（在事务内）
        AnswerSheetOcr record = doSaveOcrRecord(taskId, studentId, photoPath, ocrText,
            parsedAnswers, autoGrade, confidence);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ocrRecordId", record.getId());
        result.put("photoPath", photoPath);
        result.put("ocrText", ocrText);
        result.put("parsedAnswers", parsedAnswers);
        result.put("overallConfidence", record.getOverallConfidence());
        result.put("autoGradeResult", autoGrade);
        result.put("needsReview", confidence < 0.85);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> manualEntry(Long taskId, Long studentId, List<Map<String, Object>> answers) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        if (studentId == null) throw new BusinessException(400, "学生ID不能为空");

        List<TaskQuestion> questions = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>()
                .eq(TaskQuestion::getTaskId, taskId)
                .orderByAsc(TaskQuestion::getSortOrder));

        // 构建 parsedAnswers 格式
        List<Map<String, Object>> parsedAnswers = new ArrayList<>();
        for (Map<String, Object> ans : answers) {
            Map<String, Object> parsed = new LinkedHashMap<>();
            parsed.put("questionNo", ans.get("questionNo"));
            parsed.put("questionId", ans.get("questionId"));
            parsed.put("answer", ans.getOrDefault("answer", ""));
            parsed.put("confidence", 1.0); // 手动录入置信度=1
            parsedAnswers.add(parsed);
        }

        Map<String, Object> autoGrade = autoGrade(taskId, parsedAnswers);

        // 保存OCR记录
        AnswerSheetOcr record = new AnswerSheetOcr();
        record.setTaskId(taskId);
        record.setStudentId(studentId);
        record.setPhotoPath("MANUAL_ENTRY");
        record.setOcrRawText("手动录入");
        try {
            record.setParsedAnswers(om.writeValueAsString(parsedAnswers));
            record.setAutoGradeResult(om.writeValueAsString(autoGrade));
        } catch (JsonProcessingException e) {
            log.warn("序列化手动录入结果失败: {}", e.getMessage());
        }
        record.setOverallConfidence(BigDecimal.ONE);
        record.setStatus("graded");
        record.setGraderId(SecurityUtils.getCurrentUserId());
        record.setSchoolId(1L);
        ocrMapper.insert(record);

        // 创建TaskSubmission + StudentAnswer + 更新PrecisionProgress
        createSubmissionAndUpdateProgress(task, questions, studentId, parsedAnswers, autoGrade);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ocrRecordId", record.getId());
        result.put("parsedAnswers", parsedAnswers);
        result.put("autoGradeResult", autoGrade);
        return result;
    }

    @Override
    public List<Map<String, Object>> listOcrRecords(Long taskId, String status) {
        LambdaQueryWrapper<AnswerSheetOcr> qw = new LambdaQueryWrapper<AnswerSheetOcr>()
            .eq(AnswerSheetOcr::getTaskId, taskId)
            .orderByDesc(AnswerSheetOcr::getCreateTime);
        if (status != null && !status.isEmpty()) {
            qw.eq(AnswerSheetOcr::getStatus, status);
        }
        List<AnswerSheetOcr> records = ocrMapper.selectList(qw);
        return records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("studentId", r.getStudentId());
            m.put("studentName", r.getStudentName());
            m.put("photoPath", r.getPhotoPath());
            m.put("overallConfidence", r.getOverallConfidence());
            m.put("status", r.getStatus());
            m.put("createTime", r.getCreateTime());
            try {
                m.put("parsedAnswers", r.getParsedAnswers() != null
                    ? om.readValue(r.getParsedAnswers(), new TypeReference<List<Map<String, Object>>>() {})
                    : List.of());
                m.put("autoGradeResult", r.getAutoGradeResult() != null
                    ? om.readValue(r.getAutoGradeResult(), new TypeReference<Map<String, Object>>() {})
                    : Map.of());
            } catch (Exception e) {
                m.put("parsedAnswers", List.of());
                m.put("autoGradeResult", Map.of());
            }
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reviewOcr(Long ocrId, Long reviewerId, boolean confirmed, String note) {
        AnswerSheetOcr record = ocrMapper.selectById(ocrId);
        if (record == null) throw new BusinessException(404, "OCR记录不存在");

        if (confirmed && "parsed".equals(record.getStatus())) {
            // 复核确认时，补充创建TaskSubmission等（OCR记录此前未创建过提交）
            Task task = taskMapper.selectById(record.getTaskId());
            if (task != null && record.getStudentId() != null) {
                List<Map<String, Object>> parsedAnswers;
                try {
                    parsedAnswers = record.getParsedAnswers() != null
                        ? om.readValue(record.getParsedAnswers(), new TypeReference<List<Map<String, Object>>>() {})
                        : List.of();
                } catch (Exception e) {
                    parsedAnswers = List.of();
                }

                Map<String, Object> autoGrade;
                try {
                    autoGrade = record.getAutoGradeResult() != null
                        ? om.readValue(record.getAutoGradeResult(), new TypeReference<Map<String, Object>>() {})
                        : Map.of();
                } catch (Exception e) {
                    autoGrade = Map.of();
                }

                List<TaskQuestion> questions = taskQuestionMapper.selectList(
                    new LambdaQueryWrapper<TaskQuestion>()
                        .eq(TaskQuestion::getTaskId, record.getTaskId())
                        .orderByAsc(TaskQuestion::getSortOrder));

                // 检查是否已有提交，避免重复
                long existing = submissionMapper.selectCount(
                    new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskId, record.getTaskId())
                        .eq(TaskSubmission::getStudentId, record.getStudentId()));
                if (existing == 0) {
                    createSubmissionAndUpdateProgress(task, questions, record.getStudentId(),
                        parsedAnswers, autoGrade);
                }
            }
        }

        record.setStatus(confirmed ? "reviewed" : "failed");
        record.setReviewerId(reviewerId);
        record.setReviewNote(note);
        ocrMapper.updateById(record);
    }

    @Override
    public Map<String, Object> accuracyStats(Long taskId) {
        // 加载所有OCR记录
        List<AnswerSheetOcr> records = ocrMapper.selectList(
            new LambdaQueryWrapper<AnswerSheetOcr>()
                .eq(AnswerSheetOcr::getTaskId, taskId));
        if (records.isEmpty()) throw new BusinessException(404, "该任务无OCR记录");

        // 加载任务题目
        List<TaskQuestion> questions = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>()
                .eq(TaskQuestion::getTaskId, taskId)
                .orderByAsc(TaskQuestion::getSortOrder));
        if (questions.isEmpty()) throw new BusinessException(400, "任务无题目");

        // 加载题库正确答案
        List<Long> qIds = questions.stream()
            .map(TaskQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, QuestionBank> qMap = questionMapper.selectBatchIds(qIds).stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        // ── 逐记录对比 ──
        int totalAnswers = 0;
        int totalCorrect = 0;
        Map<String, int[]> byType = new LinkedHashMap<>();  // {correct, total}

        // 置信度分层: [high(≥0.85), mid(0.70-0.84), low(<0.70)]
        int[] highBucket = {0, 0};  // {correct, total}
        int[] midBucket = {0, 0};
        int[] lowBucket = {0, 0};

        // 每题置信度 vs 正确性列表（用于相关性预判）
        List<Map<String, Object>> confidencePairs = new ArrayList<>();

        for (AnswerSheetOcr record : records) {
            List<Map<String, Object>> parsed = new ArrayList<>();
            try {
                if (record.getParsedAnswers() != null) {
                    parsed = om.readValue(record.getParsedAnswers(),
                        new TypeReference<List<Map<String, Object>>>() {});
                }
            } catch (Exception e) { continue; }

            for (TaskQuestion tq : questions) {
                QuestionBank q = qMap.get(tq.getQuestionId());
                if (q == null || q.getCorrectAnswer() == null) continue;

                int qNo = tq.getSortOrder() + 1;
                Map<String, Object> match = parsed.stream()
                    .filter(a -> {
                        Object qno = a.get("questionNo");
                        return qno instanceof Number n && n.intValue() == qNo;
                    }).findFirst().orElse(null);

                String studentAnswer = match != null
                    ? String.valueOf(match.getOrDefault("answer", "")).trim()
                    : "";
                double conf = 0;
                if (match != null && match.get("confidence") instanceof Number n) {
                    conf = n.doubleValue();
                }

                if (studentAnswer.isEmpty()) continue; // 跳过未识别题

                totalAnswers++;
                boolean correct = ExamTaskHandler.answersMatch(
                    q.getQuestionType(), q.getCorrectAnswer().trim(), studentAnswer);
                if (correct) totalCorrect++;

                // 按题型
                String type = q.getQuestionType();
                int[] bt = byType.computeIfAbsent(type, k -> new int[]{0, 0});
                bt[1]++;
                if (correct) bt[0]++;

                // 按置信度分层
                int[] bucket = conf >= 0.85 ? highBucket : conf >= 0.70 ? midBucket : lowBucket;
                bucket[1]++;
                if (correct) bucket[0]++;

                // 置信度对
                Map<String, Object> pair = new LinkedHashMap<>();
                pair.put("confidence", Math.round(conf * 100) / 100.0);
                pair.put("correct", correct);
                pair.put("questionNo", qNo);
                pair.put("questionType", type);
                confidencePairs.add(pair);
            }
        }

        // ── 构建统计结果 ──
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("taskId", taskId);
        stats.put("totalRecords", records.size());
        stats.put("totalAnswers", totalAnswers);
        stats.put("totalCorrect", totalCorrect);
        stats.put("overallAccuracy", totalAnswers > 0
            ? Math.round(totalCorrect * 1000.0 / totalAnswers) / 10.0 : 0);

        // 按题型
        Map<String, Object> typeStats = new LinkedHashMap<>();
        for (var e : byType.entrySet()) {
            int[] v = e.getValue();
            Map<String, Object> ts = new LinkedHashMap<>();
            ts.put("correct", v[0]);
            ts.put("total", v[1]);
            ts.put("accuracy", v[1] > 0 ? Math.round(v[0] * 1000.0 / v[1]) / 10.0 : 0);
            typeStats.put(e.getKey(), ts);
        }
        stats.put("byQuestionType", typeStats);

        // 按置信度分层
        Map<String, Object> confStats = new LinkedHashMap<>();
        confStats.put("high_ge085", bucketStats(highBucket));
        confStats.put("mid_070_084", bucketStats(midBucket));
        confStats.put("low_lt070", bucketStats(lowBucket));
        stats.put("byConfidenceLevel", confStats);

        // 决策建议
        double overallAcc = (double) stats.get("overallAccuracy");
        stats.put("thresholdMet", overallAcc >= 90.0);
        stats.put("recommendation", overallAcc >= 90.0
            ? "OCR准确率达标(≥90%),可继续使用自动识别方案"
            : "OCR准确率不足(<90%),建议切换至录入员手动录入方案");

        // 置信度-正确性样本（前50条，供散点图分析）
        stats.put("confidencePairs", confidencePairs.size() > 50
            ? confidencePairs.subList(0, 50) : confidencePairs);

        return stats;
    }

    // ── 私有方法 ──

    /**
     * 仅在DB写入时开启事务（不包裹AI调用）
     */
    @Transactional
    protected AnswerSheetOcr doSaveOcrRecord(Long taskId, Long studentId, String photoPath,
                                              String ocrText, List<Map<String, Object>> parsedAnswers,
                                              Map<String, Object> autoGrade, double confidence) {
        AnswerSheetOcr record = new AnswerSheetOcr();
        record.setTaskId(taskId);
        record.setStudentId(studentId);
        record.setPhotoPath(photoPath);
        record.setOcrRawText(ocrText);
        try {
            record.setParsedAnswers(om.writeValueAsString(parsedAnswers));
            record.setAutoGradeResult(om.writeValueAsString(autoGrade));
        } catch (JsonProcessingException e) {
            log.warn("序列化OCR结果失败: {}", e.getMessage());
        }
        record.setOverallConfidence(BigDecimal.valueOf(Math.round(confidence * 100) / 100.0));
        record.setStatus(confidence >= 0.85 ? "graded" : "parsed");
        record.setSchoolId(1L);
        ocrMapper.insert(record);

        // 高置信度(>=0.85)自动创建TaskSubmission + 更新掌握度
        if (confidence >= 0.85 && studentId != null) {
            Task task = taskMapper.selectById(taskId);
            if (task != null) {
                List<TaskQuestion> questions = taskQuestionMapper.selectList(
                    new LambdaQueryWrapper<TaskQuestion>()
                        .eq(TaskQuestion::getTaskId, taskId)
                        .orderByAsc(TaskQuestion::getSortOrder));
                createSubmissionAndUpdateProgress(task, questions, studentId, parsedAnswers, autoGrade);
            }
        }

        return record;
    }

    /**
     * 创建 TaskSubmission + StudentAnswer + 更新 PrecisionProgress
     */
    private void createSubmissionAndUpdateProgress(Task task, List<TaskQuestion> questions,
                                                    Long studentId, List<Map<String, Object>> parsedAnswers,
                                                    Map<String, Object> autoGrade) {
        if (task == null || studentId == null) return;

        // 检查是否已有提交，避免重复
        long existing = submissionMapper.selectCount(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, task.getId())
                .eq(TaskSubmission::getStudentId, studentId));
        if (existing > 0) return;

        Number earnedScoreNum = (Number) autoGrade.getOrDefault("earnedScore", 0);
        Number totalScoreNum = (Number) autoGrade.getOrDefault("totalScore", 0);
        BigDecimal earnedScore = BigDecimal.valueOf(earnedScoreNum.doubleValue());
        BigDecimal totalScore = BigDecimal.valueOf(totalScoreNum.doubleValue());

        // 创建 TaskSubmission
        TaskSubmission submission = new TaskSubmission();
        submission.setTaskId(task.getId());
        submission.setStudentId(studentId);
        submission.setSchoolId(1L);
        submission.setScore(earnedScore);
        submission.setStatus("SUBMITTED");
        submission.setGradedBy(SecurityUtils.getCurrentUserId());
        submission.setGradeType("AUTO");
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setGradedAt(LocalDateTime.now());
        submissionMapper.insert(submission);

        // 创建 StudentAnswer 记录 + 更新 PrecisionProgress
        for (TaskQuestion tq : questions) {
            QuestionBank q = questionMapper.selectById(tq.getQuestionId());
            if (q == null) continue;

            int qNo = tq.getSortOrder() + 1;
            Map<String, Object> match = parsedAnswers.stream()
                .filter(a -> {
                    Object qno = a.get("questionNo");
                    return qno instanceof Number n && n.intValue() == qNo;
                }).findFirst().orElse(null);

            String studentAnswer = match != null
                ? String.valueOf(match.getOrDefault("answer", "")).trim()
                : "";

            boolean isCorrect = !studentAnswer.isEmpty() && q.getCorrectAnswer() != null
                && ExamTaskHandler.answersMatch(q.getQuestionType(), q.getCorrectAnswer().trim(), studentAnswer);

            // 创建 StudentAnswer
            StudentAnswer sa = new StudentAnswer();
            sa.setSubmissionId(submission.getId());
            sa.setTaskId(task.getId());
            sa.setQuestionId(tq.getQuestionId());
            sa.setStudentAnswer(studentAnswer);
            sa.setIsCorrect(isCorrect ? 1 : 0);
            sa.setAutoScore(isCorrect && tq.getScore() != null ? tq.getScore() : BigDecimal.ZERO);
            sa.setSchoolId(1L);
            sa.setAnswerTime(LocalDateTime.now());
            studentAnswerMapper.insert(sa);

            // 更新 PrecisionProgress（加权移动平均，与 PrecisionHelper 一致）
            updateProgressForAnswer(studentId, q, tq.getQuestionId(), isCorrect);
        }
    }

    /**
     * 更新掌握度（加权移动平均，与 PrecisionHelper.updateProgressForAnswer 一致）
     */
    private void updateProgressForAnswer(Long studentId, QuestionBank q, Long questionId, boolean isCorrect) {
        Long nodeId = q.getCategoryId();
        if (nodeId == null) return;

        String subject = q.getSubject();

        PrecisionProgress pp = progressMapper.selectOne(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getNodeId, nodeId));
        if (pp == null) {
            pp = new PrecisionProgress();
            pp.setStudentId(studentId);
            pp.setNodeId(nodeId);
            pp.setSubject(subject);
            pp.setStatus("learning");
            pp.setMasteryPercent(BigDecimal.ZERO);
            pp.setTotalAttempts(0);
            pp.setTotalCorrect(0);
        }
        int oldMastery = pp.getMasteryPercent() != null ? pp.getMasteryPercent().intValue() : 0;
        // 加权移动平均：减少单次答题对掌握度的影响，避免近因偏差
        // weight 随答题次数增加而增大：N=1→0.12, N=20→0.5
        int totalAttempts = pp.getTotalAttempts() != null ? pp.getTotalAttempts() : 0;
        double weight = Math.min(0.5, 0.1 + totalAttempts * 0.02);
        double currentScore = isCorrect ? 100.0 : 0.0;
        int newMastery = (int) Math.max(0, Math.min(100, Math.round(weight * currentScore + (1 - weight) * oldMastery)));
        pp.setMasteryPercent(BigDecimal.valueOf(newMastery));
        pp.setTotalAttempts(totalAttempts + 1);
        if (isCorrect) {
            pp.setTotalCorrect((pp.getTotalCorrect() != null ? pp.getTotalCorrect() : 0) + 1);
        }
        pp.setLastPracticeAt(LocalDateTime.now());
        if (newMastery >= 80) pp.setStatus("mastered");
        else if (newMastery >= 40) pp.setStatus("learning");
        else pp.setStatus("weak");
        if (pp.getId() == null) progressMapper.insert(pp);
        else progressMapper.updateById(pp);
    }

    private Map<String, Object> bucketStats(int[] bucket) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("correct", bucket[0]);
        m.put("total", bucket[1]);
        m.put("accuracy", bucket[1] > 0 ? Math.round(bucket[0] * 1000.0 / bucket[1]) / 10.0 : 0);
        return m;
    }

    private String buildOcrPrompt(Task task, List<TaskQuestion> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append("请识别这张答题卡照片中的所有答案。\n\n");
        sb.append("任务信息：").append(task.getTitle()).append("\n");
        sb.append("共 ").append(questions.size()).append(" 题\n\n");
        sb.append("请按以下JSON格式返回每题的答案：\n");
        sb.append("[\n");
        sb.append("  {\"questionNo\": 1, \"answer\": \"A\", \"confidence\": 0.95},\n");
        sb.append("  {\"questionNo\": 2, \"answer\": \"对\", \"confidence\": 0.90}\n");
        sb.append("]\n\n");
        sb.append("规则：\n");
        sb.append("- questionNo: 题号（从1开始）\n");
        sb.append("- answer: 学生填写的答案（选择题填A/B/C/D，判断题填对/错，填空题填内容）\n");
        sb.append("- confidence: 你对该识别结果的置信度（0-1）\n");
        sb.append("- 如果某题看不清，confidence设为0并answer设为空\n");
        sb.append("- 只返回JSON数组，不要添加其他解释\n");
        return sb.toString();
    }

    private List<Map<String, Object>> parseOcrResult(String ocrText, List<TaskQuestion> questions) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String json = ocrText;
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
                result = om.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("解析OCR结果失败: {}", e.getMessage());
        }
        return result;
    }

    private double calculateConfidence(List<Map<String, Object>> parsedAnswers) {
        if (parsedAnswers.isEmpty()) return 0.0;
        double sum = 0;
        int count = 0;
        for (Map<String, Object> a : parsedAnswers) {
            Object conf = a.get("confidence");
            if (conf instanceof Number n) {
                sum += n.doubleValue();
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private Map<String, Object> autoGrade(Long taskId, List<Map<String, Object>> parsedAnswers) {
        List<TaskQuestion> questions = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>()
                .eq(TaskQuestion::getTaskId, taskId)
                .orderByAsc(TaskQuestion::getSortOrder));

        List<Long> qIds = questions.stream()
            .map(TaskQuestion::getQuestionId)
            .collect(Collectors.toList());
        Map<Long, QuestionBank> qMap = questionMapper.selectBatchIds(qIds).stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        int correct = 0;
        int total = questions.size();
        double totalScore = 0;
        double earnedScore = 0;

        for (TaskQuestion tq : questions) {
            QuestionBank q = qMap.get(tq.getQuestionId());
            if (q == null) continue;
            totalScore += tq.getScore() != null ? tq.getScore().doubleValue() : 0;

            int qNo = tq.getSortOrder() + 1;
            String studentAnswer = parsedAnswers.stream()
                .filter(a -> {
                    Object qno = a.get("questionNo");
                    return qno instanceof Number n && n.intValue() == qNo;
                })
                .map(a -> String.valueOf(a.getOrDefault("answer", "")))
                .findFirst().orElse("");

            if (!studentAnswer.isEmpty() && q.getCorrectAnswer() != null) {
                boolean isCorrect = ExamTaskHandler.answersMatch(
                    q.getQuestionType(), q.getCorrectAnswer().trim(), studentAnswer.trim());
                if (isCorrect) {
                    correct++;
                    earnedScore += tq.getScore() != null ? tq.getScore().doubleValue() : 0;
                }
            }
        }

        Map<String, Object> gradeResult = new LinkedHashMap<>();
        gradeResult.put("correct", correct);
        gradeResult.put("total", total);
        gradeResult.put("totalScore", totalScore);
        gradeResult.put("earnedScore", Math.round(earnedScore * 10) / 10.0);
        gradeResult.put("accuracy", total > 0 ? Math.round(correct * 100.0 / total) : 0);
        return gradeResult;
    }

    private String savePhoto(MultipartFile file, Long taskId, String ext) {
        Path uploadPath = Paths.get(uploadDir, "answer_sheets");
        try {
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = "task_" + taskId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            Path targetFile = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetFile);
            return "/api/uploads/answer_sheets/" + filename;
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
