package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiExamPaperServiceImpl {

    private final AiContentGeneratorService aiContentService;
    private final StudentAnswerMapper studentAnswerMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final DeepSeekGateway deepSeekGateway;
    private static final ObjectMapper om = new ObjectMapper();

    public String submitExamPaper(Long teacherId, Map<String, Object> params) {
        params.put("contentType", "EXAM_PAPER");
        return aiContentService.submitGeneration(teacherId, params);
    }

    /**
     * 构建诊断数据 — 优化版：正确JOIN question_bank + 聚合统计替代全量分布，大幅减小Prompt体积避免超时
     */
    public Map<String, Object> buildDiagnosisData(Long taskId) {
        // 1. 任务题目关联
        List<TaskQuestion> tqList = taskQuestionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskQuestion>()
                .eq(TaskQuestion::getTaskId, taskId).orderByAsc(TaskQuestion::getSortOrder));
        if (tqList.isEmpty()) return Map.of("questions", List.of(), "totalStudents", 0);

        // 2. 批量加载题库数据
        List<Long> qbIds = tqList.stream().map(TaskQuestion::getQuestionId).filter(Objects::nonNull).distinct().toList();
        Map<Long, QuestionBank> qbMap = new HashMap<>();
        if (!qbIds.isEmpty()) {
            List<QuestionBank> qbList = questionBankMapper.selectBatchIds(qbIds);
            if (qbList != null) for (QuestionBank qb : qbList) qbMap.put(qb.getId(), qb);
        }

        // 3. 学生答题记录
        List<StudentAnswer> answers = studentAnswerMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getTaskId, taskId));

        // 4. 学生提交记录（用于获取分数和姓名）
        List<TaskSubmission> submissions = submissionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId).in(TaskSubmission::getStatus, "SUBMITTED", "GRADED"));
        Map<Long, TaskSubmission> subByStudent = new HashMap<>();
        for (TaskSubmission sub : submissions) subByStudent.put(sub.getStudentId(), sub);

        // 5. 学生姓名映射
        Map<Long, String> studentNames = new HashMap<>();
        if (!subByStudent.isEmpty()) {
            List<Student> students = studentMapper.selectBatchIds(subByStudent.keySet());
            if (students != null) {
                Set<Long> userIds = students.stream().map(Student::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
                Map<Long, String> userNames = new HashMap<>();
                if (!userIds.isEmpty()) {
                    List<User> users = userMapper.selectBatchIds(userIds);
                    if (users != null) for (User u : users) userNames.put(u.getId(), u.getRealName());
                }
                for (Student s : students) {
                    String name = userNames.getOrDefault(s.getUserId(), "学生" + s.getId());
                    studentNames.put(s.getId(), name);
                }
            }
        }

        // 6. 逐题统计（精简版：正确率+top3错误答案，不传全量分布）
        List<Map<String, Object>> perQuestion = new ArrayList<>();
        for (int qIdx = 0; qIdx < tqList.size(); qIdx++) {
            TaskQuestion tq = tqList.get(qIdx);
            QuestionBank qb = qbMap.get(tq.getQuestionId());

            int correct = 0, total = 0;
            Map<String, Integer> wrongCounts = new LinkedHashMap<>();
            for (StudentAnswer sa : answers) {
                if (tq.getQuestionId().equals(sa.getQuestionId())) {
                    total++;
                    if (sa.getIsCorrect() != null && sa.getIsCorrect() == 1) {
                        correct++;
                    } else {
                        String ans = sa.getStudentAnswer() != null ? sa.getStudentAnswer().trim() : "(空)";
                        if (ans.length() > 40) ans = ans.substring(0, 40) + "...";
                        wrongCounts.merge(ans.isEmpty() ? "(空)" : ans, 1, Integer::sum);
                    }
                }
            }
            if (total == 0) continue;

            // Top 3 wrong answers
            List<String> topWrong = wrongCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3).map(e -> e.getKey() + "(" + e.getValue() + "人)").toList();

            Map<String, Object> qInfo = new LinkedHashMap<>();
            qInfo.put("questionIndex", qIdx + 1);
            qInfo.put("questionText", truncate(qb != null ? qb.getQuestionText() : "", 80));
            qInfo.put("correctAnswer", qb != null ? qb.getCorrectAnswer() : "");
            qInfo.put("questionType", qb != null ? qb.getQuestionType() : "SINGLE_CHOICE");
            qInfo.put("correctRate", Math.round((double) correct / total * 1000) / 10.0);
            qInfo.put("totalAnswers", total);
            qInfo.put("topWrongAnswers", topWrong);
            perQuestion.add(qInfo);
        }

        // 7. 学生层面摘要（姓名+得分，精简到每个学生一行）
        List<Map<String, Object>> studentSummary = new ArrayList<>();
        for (Map.Entry<Long, TaskSubmission> entry : subByStudent.entrySet()) {
            Long sid = entry.getKey();
            TaskSubmission sub = entry.getValue();
            Map<String, Object> ss = new LinkedHashMap<>();
            ss.put("name", studentNames.getOrDefault(sid, "学生" + sid));
            ss.put("score", sub.getScore());
            ss.put("totalScore", null);  // totalScore lives in Task entity, not needed for diagnosis prompt
            studentSummary.add(ss);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("questions", perQuestion);
        data.put("students", studentSummary);
        data.put("totalStudents", studentSummary.size());
        return data;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> batchVisionGrade(List<String> imageBase64List,
            String questionText, String referenceAnswer, String gradingRubric) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < imageBase64List.size(); i += 5) {
            int end = Math.min(i + 5, imageBase64List.size());
            List<String> batch = imageBase64List.subList(i, end);
            String prompt = String.format(
                "请逐一批改%d份学生手写作答。题目：%s\n参考答案：%s\n评分标准：%s\n" +
                "置信度规则：识别清晰且答案确定→confidence≥0.85；手写潦草或答案不确定→confidence=0.70~0.85并标注\"建议复核\"；无法辨认→confidence<0.70标注\"需教师复核\"。\n" +
                "逐份返回JSON数组:[{\"studentIndex\":0,\"recognizedText\":\"\",\"totalScore\":0,\"confidence\":0.9,\"errors\":[],\"note\":\"\"}]",
                batch.size(), questionText, referenceAnswer, gradingRubric);
            try {
                Map<String, Object> resp = deepSeekGateway.callVision(batch, prompt, Map.of());
                String content = (String) resp.get("content");
                int js = content.indexOf('['), je = content.lastIndexOf(']');
                if (js >= 0 && je > js) {
                    List<Map> parsed = om.readValue(content.substring(js, je + 1),
                        om.getTypeFactory().constructCollectionType(List.class, Map.class));
                    for (Map m : parsed) results.add(new LinkedHashMap<>(m));
                }
            } catch (Exception e) { log.warn("Vision批改解析失败: batch {}~{}", i, end, e); }
        }
        return results;
    }
}
