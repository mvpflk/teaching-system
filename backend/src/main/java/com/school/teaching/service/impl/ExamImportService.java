package com.school.teaching.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.school.teaching.entity.Exam;
import com.school.teaching.entity.ExamQuestion;
import com.school.teaching.mapper.ExamMapper;
import com.school.teaching.mapper.ExamQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
// @Service — 410 DISABLED (backup tables dropped)
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// @Service — 410 DISABLED (backup tables dropped)
public class ExamImportService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamQuestionMapper questionMapper;

    @Transactional
    public Exam importFromExcel(MultipartFile file, String examTitle, String subject,
                                 Integer durationMinutes, Integer passingScore) throws IOException {
        // 1. 先创建试卷
        Exam exam = new Exam();
        exam.setTitle(examTitle != null ? examTitle : file.getOriginalFilename());
        exam.setSubject(subject);
        exam.setExamType("exam");
        exam.setDurationMinutes(durationMinutes != null ? durationMinutes : 60);
        exam.setPassingScore(passingScore != null ? passingScore : 60);
        exam.setIsRandomOrder(0);
        exam.setAllowCheatDetection(1);
        exam.setMaxCheatWarnings(3);
        exam.setStatus("draft");
        exam.setStartTime(java.time.LocalDateTime.now());
        exam.setEndTime(java.time.LocalDateTime.now().plusDays(30));
        exam.setTotalScore(0);
        exam.setQuestionCount(0);
        examMapper.insert(exam);

        // 2. 解析Excel
        List<ExamQuestion> questions = new ArrayList<>();
        try (InputStream is = file.getInputStream()) {
            EasyExcel.read(is, ExamExcelRow.class, new ReadListener<ExamExcelRow>() {
                @Override
                public void invoke(ExamExcelRow row, AnalysisContext context) {
                    ExamQuestion q = new ExamQuestion();
                    q.setExamId(exam.getId());
                    q.setQuestionType(mapType(row.getQuestionType()));
                    q.setQuestionText(row.getQuestionText());
                    if (row.getOptions() != null && !row.getOptions().isEmpty()) {
                        q.setOptions(toJsonArray(row.getOptions()));
                    }
                    q.setCorrectAnswer(row.getCorrectAnswer());
                    q.setScore(row.getScore() != null ? row.getScore() : 5);
                    q.setExplanation(row.getExplanation());
                    q.setSortOrder(questions.size() + 1);
                    questions.add(q);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {}
            }).sheet().doRead();
        }

        // 3. 批量插入题目
        int totalScore = 0;
        for (ExamQuestion q : questions) {
            questionMapper.insert(q);
            totalScore += q.getScore();
        }

        // 4. 更新试卷统计
        exam.setQuestionCount(questions.size());
        exam.setTotalScore(totalScore);
        examMapper.updateById(exam);

        return exam;
    }

    public byte[] generateTemplate() throws IOException {
        List<ExamExcelRow> example = new ArrayList<>();
        example.add(new ExamExcelRow("Java基础测试", "Java", "SINGLE_CHOICE", "Java的入口方法是？",
            "A. main|B. start|C. run|D. init", "A", 5, "public static void main"));
        example.add(new ExamExcelRow("", "", "MULTI_CHOICE", "以下哪些是Java关键字？",
            "A. class|B. include|C. public|D. define", "A,C", 5, ""));
        example.add(new ExamExcelRow("", "", "TRUE_FALSE", "Java支持多继承？",
            "", "错", 5, "Java只支持单继承，接口可以多实现"));
        example.add(new ExamExcelRow("", "", "FILL_IN", "Java中打印输出的方法是？",
            "", "System.out.println", 5, ""));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        EasyExcel.write(bos, ExamExcelRow.class).sheet("试卷模板").doWrite(example);
        return bos.toByteArray();
    }

    private String mapType(String type) {
        if (type == null) return "SINGLE_CHOICE";
        return switch (type.trim().toUpperCase()) {
            case "单选题", "SINGLE_CHOICE" -> "SINGLE_CHOICE";
            case "多选题", "MULTI_CHOICE" -> "MULTI_CHOICE";
            case "判断题", "TRUE_FALSE" -> "TRUE_FALSE";
            case "填空题", "FILL_IN" -> "FILL_IN";
            default -> "SINGLE_CHOICE";
        };
    }

    private String toJsonArray(String options) {
        if (options == null || options.isEmpty()) return "[]";
        String[] parts = options.split("\\|");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(parts[i].trim()).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
