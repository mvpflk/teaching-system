package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.DictSubject;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.PrecisionProgress;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.Student;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ClassesMapper;
import com.school.teaching.mapper.DictSubjectMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import com.school.teaching.precision.PrecisionEnglishService;
import com.school.teaching.precision.PrecisionMathService;
import com.school.teaching.service.ExamSyllabusService;
import com.school.teaching.service.QuestionMatchingService;
import com.school.teaching.service.SystemService;
import com.school.teaching.service.impl.DeepSeekGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrecisionReportService {

    @Autowired private PrecisionProgressMapper progressMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private QuestionBankMapper questionMapper;
    @Autowired private WrongQuestionMapper wrongMapper;
    @Autowired private DictSubjectMapper subjectMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private ExamSyllabusService examSyllabusService;
    @Autowired(required = false) private DeepSeekGateway deepSeekGateway;
    @Autowired private SystemService systemService;
    @Autowired private PrecisionHelper helper;
    @Autowired private QuestionMatchingService questionMatchingService;
    @Autowired(required = false) private PrecisionMathService mathService;
    @Autowired(required = false) private PrecisionEnglishService englishService;

    @Value("${teaching.upload-dir:uploads/avatars}") private String uploadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getReport(Long studentId, String subject) {
        if ("英语[职高]".equals(subject)) {
            return englishService.getEnglishReport(studentId);
        }

        List<PrecisionProgress> list = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getSubject, subject));
        double avg = list.stream().mapToDouble(p ->
            p.getMasteryPercent() != null ? p.getMasteryPercent().doubleValue() : 0).average().orElse(0);
        long mastered = list.stream().filter(p -> "mastered".equals(p.getStatus())).count();
        long learning = list.stream().filter(p -> "learning".equals(p.getStatus())).count();

        int streakWeeks = 0;
        int lastTestScore = 0;
        Student st = studentMapper.selectById(studentId);
        if (st != null && st.getPrecisionProfile() != null) {
            try {
                Map<String, Object> pf = objectMapper.readValue(st.getPrecisionProfile(),
                    new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                Map<String, Object> sp = (Map<String, Object>) pf.get(subject);
                if (sp != null) {
                    streakWeeks = PrecisionHelper.toInt(sp.get("streakWeeks"), 0);
                    lastTestScore = PrecisionHelper.toInt(sp.get("lastOnlineTestScore"), 0);
                }
            } catch (Exception e) { log.warn("读取偏科画像数据失败: {}", e.getMessage()); }
        }

        Set<Long> weakNodeIds = list.stream()
            .filter(p -> p.getMasteryPercent() != null && p.getMasteryPercent().doubleValue() < 60)
            .map(PrecisionProgress::getNodeId)
            .collect(Collectors.toSet());
        Map<Long, KnowledgeNode> nodeMap = Collections.emptyMap();
        if (!weakNodeIds.isEmpty()) {
            nodeMap = nodeMapper.selectBatchIds(weakNodeIds).stream()
                .filter(n -> n.getLevel() == null || n.getLevel() > 1)
                .collect(Collectors.toMap(KnowledgeNode::getId, n -> n, (a, b) -> a));
        }
        final Map<Long, KnowledgeNode> finalNodeMap = nodeMap;

        List<Map<String, Object>> weakNodes = list.stream()
            .filter(p -> p.getMasteryPercent() != null && p.getMasteryPercent().doubleValue() < 60)
            .sorted(Comparator.comparing(PrecisionProgress::getMasteryPercent))
            .map(p -> {
                KnowledgeNode kn = finalNodeMap.get(p.getNodeId());
                if (kn == null) return null;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nodeId", p.getNodeId());
                m.put("nodeName", kn.getName());
                m.put("masteryPercent", p.getMasteryPercent());
                m.put("attempts", p.getTotalAttempts());
                if (kn.getLearningResources() != null) {
                    try {
                        m.put("learningResources", objectMapper.readValue(kn.getLearningResources(), Map.class));
                    } catch (Exception e) {
                        log.warn("学习资源JSON解析失败 nodeId={}", p.getNodeId());
                    }
                }
                return m;
            }).filter(Objects::nonNull)
            .limit(5)
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subject", subject);
        result.put("nodeCount", list.size());
        result.put("masteredCount", mastered);
        result.put("learningCount", learning);
        result.put("avgMastery", Math.round(avg));
        result.put("streakWeeks", streakWeeks);
        result.put("lastTestScore", lastTestScore);
        result.put("weakNodes", weakNodes);

        List<Map<String, Object>> trendData = list.stream()
            .filter(p -> p.getUpdateTime() != null && p.getMasteryPercent() != null)
            .collect(Collectors.groupingBy(
                p -> java.time.format.DateTimeFormatter.ofPattern("yyyy-'W'ww",
                    java.util.Locale.ENGLISH).format(p.getUpdateTime()),
                Collectors.averagingDouble(p -> p.getMasteryPercent().doubleValue())))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("week", e.getKey());
                m.put("masteryPercent", Math.round(e.getValue()));
                return m;
            }).collect(Collectors.toList());
        result.put("trendData", trendData);
        return result;
    }

    public List<Map<String, Object>> getSyllabusMap(Long studentId, String subject) {
        Long subjectId = helper.getSubjectId(subject);
        List<KnowledgeNode> nodes = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getSubjectId, subjectId)
                .eq(KnowledgeNode::getLevel, 2));
        List<PrecisionProgress> progs = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getSubject, subject));
        Map<Long, PrecisionProgress> progMap = progs.stream()
            .collect(Collectors.toMap(PrecisionProgress::getNodeId, p -> p, (a, b) -> a));

        Set<Long> syllabusNodeIds = new java.util.HashSet<>();
        String syllabusTitle = null;
        try {
            var syllabi = examSyllabusService.getSyllabiByNodeId(subjectId);
            if (syllabi != null && !syllabi.isEmpty()) {
                var syllabus = syllabi.get(0);
                syllabusTitle = syllabus.getTitle();
                java.util.List<Long> sNodeIds = examSyllabusService.getNodeIds(syllabus.getId());
                if (sNodeIds != null) syllabusNodeIds.addAll(sNodeIds);
            }
        } catch (Exception e) { log.debug("考纲查询失败，不影响主流程: {}", e.getMessage()); }

        boolean hasSyllabus = !syllabusNodeIds.isEmpty();
        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgeNode n : nodes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nodeId", n.getId()); m.put("name", PrecisionHelper.fixEncoding(n.getName()));
            PrecisionProgress pp = progMap.get(n.getId());
            m.put("masteryPercent", pp != null ? pp.getMasteryPercent() : 0);
            m.put("status", pp != null ? pp.getStatus() : "weak");
            m.put("inSyllabus", hasSyllabus ? syllabusNodeIds.contains(n.getId()) : true);
            result.add(m);
        }
        if (syllabusTitle != null) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", syllabusTitle);
            meta.put("type", "DUIKOU");
            Map<String, Object> metaNode = new LinkedHashMap<>();
            metaNode.put("nodeId", 0L); metaNode.put("_meta", meta);
            result.add(0, metaNode);
        }
        return result;
    }

    public List<Map<String, Object>> getPracticeQuestions(Long studentId, Long nodeId, String subject) {
        List<QuestionBank> pool = questionMatchingService.matchSingleNode(nodeId, subject, 30);
        if (pool.size() < 3 && subject != null) {
            Set<Long> existingIds = pool.stream().map(QuestionBank::getId).collect(Collectors.toSet());
            List<QuestionBank> supplement = questionMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                    .like(QuestionBank::getSubject, subject)
                    .eq(QuestionBank::getStatus, 1)
                    .last("LIMIT 30"));
            for (QuestionBank qb : supplement) {
                if (!existingIds.contains(qb.getId()) && pool.size() < 10) pool.add(qb);
            }
        }
        if (pool.isEmpty()) return List.of();

        Collections.shuffle(pool);
        return pool.stream().limit(10).map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("questionId", q.getId()); m.put("questionType", q.getQuestionType());
            m.put("questionText", PrecisionHelper.fixEncoding(q.getQuestionText()));
            String optsJson = q.getOptions();
            String qType = q.getQuestionType();
            if ("TRUE_FALSE".equals(qType) && (optsJson == null || optsJson.isBlank() || "[]".equals(optsJson.trim()))) {
                optsJson = "[\"A. √\",\"B. ×\"]";
            }
            m.put("options", PrecisionHelper.parseJson(optsJson));
            m.put("correctAnswer", PrecisionHelper.fixEncoding(q.getCorrectAnswer()));
            m.put("explanation", q.getExplanation() != null ? PrecisionHelper.fixEncoding(q.getExplanation()) : "");
            m.put("difficultyLevel", q.getDifficultyLevel());
            m.put("source", q.getStatus() != null && q.getStatus() == 1 ? "bank" : "ai");
            return m;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> aiQa(Long studentId, String question) {
        if (mathService != null) {
            String answer = mathService.aiExplain(studentId, question);
            return Map.of("question", question, "answer", answer);
        }
        return Map.of("answer", "AI答疑暂未启用");
    }

    public Map<String, Object> getStudentKpStatus(Long studentId, Long kpId) {
        PrecisionProgress pp = progressMapper.selectOne(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getNodeId, kpId));
        Map<String, Object> r = new LinkedHashMap<>();
        if (pp == null) {
            r.put("exists", false);
            r.put("status", "none");
            r.put("masteryPercent", 0);
            r.put("totalAttempts", 0);
        } else {
            r.put("exists", true);
            r.put("status", pp.getStatus() != null ? pp.getStatus() : "learning");
            r.put("masteryPercent", pp.getMasteryPercent() != null ? pp.getMasteryPercent().intValue() : 0);
            r.put("totalAttempts", pp.getTotalAttempts() != null ? pp.getTotalAttempts() : 0);
        }
        return r;
    }

    @Transactional
    public Map<String, Object> ensureFromQuality(Long studentId, Long kpId, String subject) {
        Map<String, Object> result = new LinkedHashMap<>();

        PrecisionProgress pp = progressMapper.selectOne(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getNodeId, kpId));
        boolean existed = pp != null;
        if (!existed) {
            pp = new PrecisionProgress();
            pp.setStudentId(studentId);
            pp.setNodeId(kpId);
            pp.setSubject(subject);
            pp.setStatus("learning");
            pp.setMasteryPercent(java.math.BigDecimal.ZERO);
            pp.setTotalAttempts(0);
            pp.setTotalCorrect(0);
            pp.setCreateTime(java.time.LocalDateTime.now());
            progressMapper.insert(pp);
        }

        List<Map<String, Object>> questions = getPracticeQuestions(studentId, kpId, subject);

        result.put("exists", existed);
        result.put("status", pp.getStatus());
        result.put("masteryPercent", pp.getMasteryPercent() != null ? pp.getMasteryPercent().intValue() : 0);
        result.put("questionCount", questions.size());
        result.put("questions", questions.subList(0, Math.min(5, questions.size())));
        return result;
    }

    public Map<String, Object> uploadAnswerPhoto(Long studentId, Long questionId,
            String questionType, MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questionId", questionId);

        if (file.isEmpty()) throw new BusinessException(400, "请选择文件");
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!Set.of("jpg", "jpeg", "png", "webp").contains(ext)) {
            throw new BusinessException(400, "仅支持 JPG/PNG/WebP 格式，不支持：" + ext);
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(400, "文件大小不能超过 5MB");
        }
        // MIME 校验：防止恶意文件伪装扩展名
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "文件类型无效，请上传图片文件");
        }

        Path uploadPath = Paths.get(uploadDir, "precision");
        try {
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = "math_" + studentId + "_" + questionId + "_"
                + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            Path targetFile = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetFile);
            result.put("attachmentPath", "/api/uploads/precision/" + filename);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        }

        String ocrText = "";
        double confidence = 0.0;
        if (deepSeekGateway != null) {
            try {
                byte[] bytes = file.getBytes();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                Map<String, Object> visionResult = deepSeekGateway.callVision(
                    List.of(base64),
                    "请识别这张数学手写解答图片中的全部文字和公式。" +
                    "保留解题步骤编号（如(1)(2)），数学公式转换为LaTeX格式（用$...$包裹）。" +
                    "不要添加额外解释，只输出识别到的内容。",
                    Map.of("temperature", 0.1));
                if (visionResult != null) {
                    ocrText = String.valueOf(visionResult.getOrDefault("content", ""));
                    confidence = ocrText.length() > 10 ? 0.85 : 0.5;
                }
            } catch (Exception e) {
                log.warn("Vision OCR 识别失败 sid={} qid={}: {}", studentId, questionId, e.getMessage());
                ocrText = "AI识别暂时不可用，教师将查看原始照片评阅。";
            }
        } else {
            ocrText = "AI服务未配置，请教师查看原始照片评阅。";
        }

        result.put("ocrText", ocrText);
        result.put("confidence", Math.round(confidence * 100) / 100.0);
        result.put("questionType", questionType);
        return result;
    }
}
