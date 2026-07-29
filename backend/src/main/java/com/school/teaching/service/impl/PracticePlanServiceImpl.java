package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AiContentGeneratorService;
import com.school.teaching.service.PracticePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PracticePlanServiceImpl implements PracticePlanService {

    @Autowired private PracticePlanMapper planMapper;
    @Autowired private PracticeRubricMapper rubricMapper;
    @Autowired private PracticeStepMapper stepMapper;
    @Autowired private PracticeStepGradeMapper gradeMapper;
    @Autowired private PracticeSubmissionMapper submissionMapper;

    @Autowired private com.school.teaching.mapper.TaskMapper taskMapper;
    @Autowired private com.school.teaching.mapper.TeacherMapper teacherMapper;
    @Autowired private com.school.teaching.mapper.ClassesMapper classesMapper;
    @Autowired private AiContentGeneratorService aiContentGeneratorService;

    private static final ObjectMapper om = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(PracticePlanServiceImpl.class);

    @Override
    @Transactional
    public PracticePlan create(PracticePlan plan, List<PracticeRubric> rubrics) {
        if (plan.getTitle() == null || plan.getTitle().isBlank())
            throw new BusinessException(400, "方案标题必填");
        plan.setCreateTime(LocalDateTime.now());
        planMapper.insert(plan);
        if (rubrics != null) saveRubrics(plan.getId(), rubrics);
        return plan;
    }

    @Override
    @Transactional
    public PracticePlan update(Long id, PracticePlan plan, List<PracticeRubric> rubrics) {
        PracticePlan existing = planMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "方案不存在");

        Long userId = com.school.teaching.security.SecurityUtils.getCurrentUserId();
        if (!existing.getCreatedBy().equals(userId)) {
            Teacher currentTeacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
            if (currentTeacher == null
                || !Boolean.TRUE.equals(existing.getShared())
                || !existing.getSubject().equals(currentTeacher.getSubject())) {
                throw new BusinessException(403, "无权编辑此方案");
            }
        }

        existing.setTitle(plan.getTitle());
        existing.setDescription(plan.getDescription());
        existing.setPrerequisites(plan.getPrerequisites());
        existing.setEnvironment(plan.getEnvironment());
        existing.setSafetyNotes(plan.getSafetyNotes());
        existing.setTroubleshooting(plan.getTroubleshooting());
        existing.setTeamRoles(plan.getTeamRoles());
        existing.setScoringModel(plan.getScoringModel());
        if (plan.getSimpleMode() != null) existing.setSimpleMode(plan.getSimpleMode());
        if (plan.getShared() != null) existing.setShared(plan.getShared());
        if (plan.getSubject() != null) existing.setSubject(plan.getSubject());
        planMapper.updateById(existing);
        if (rubrics != null) saveRubrics(id, rubrics);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PracticePlan existing = planMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "方案不存在");
        // S5: 校验方案归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (!existing.getCreatedBy().equals(userId)) {
            throw new BusinessException(403, "无权删除他人方案");
        }
        rubricMapper.delete(new LambdaQueryWrapper<PracticeRubric>().eq(PracticeRubric::getPlanId, id));
        planMapper.deleteById(id);
    }

    @Override
    public PracticePlan getById(Long id) {
        return planMapper.selectById(id);
    }

    @Override
    public List<PracticePlan> listByCreator(Long userId) {
        return planMapper.selectList(
            new LambdaQueryWrapper<PracticePlan>()
                .eq(PracticePlan::getCreatedBy, userId)
                .orderByDesc(PracticePlan::getCreateTime));
    }

    @Override
    @Transactional
    public Map<String, Object> publishToTask(Long planId, List<Long> classIds, Long teacherId) {
        PracticePlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(404, "方案不存在");
        // S6: 校验方案归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (!plan.getCreatedBy().equals(userId)) {
            if (!Boolean.TRUE.equals(plan.getShared())) {
                throw new BusinessException(403, "无权发布他人方案");
            }
        }
        List<PracticeRubric> rubrics = getRubrics(planId);
        if (rubrics.isEmpty()) throw new BusinessException(400, "请先配置评分细则");

        // S8: 移除身份降级逻辑 — 直接用当前教师身份发布
        Teacher teacher = teacherMapper.selectOne(
            new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, teacherId));
        if (teacher == null) throw new BusinessException(404, "未找到教师账号，请以教师身份登录");

        if (classIds == null || classIds.isEmpty()) throw new BusinessException(400, "请选择目标班级");

        // 提取步骤：优先使用 steps_json，兼容旧 description JSON
        List<Map<String, Object>> stepsFromDesc;

        if (Boolean.TRUE.equals(plan.getSimpleMode())) {
            stepsFromDesc = List.of(Map.of(
                "title", "提交作品",
                "description", "请上传作品截图并简要描述你的完成过程，包括遇到的问题和解决方法。",
                "attachmentMode", "REFERENCE"
            ));
        } else {
            // 新格式：从 steps_json 读取
            if (plan.getStepsJson() != null && !plan.getStepsJson().isBlank()) {
                try {
                    stepsFromDesc = om.readValue(plan.getStepsJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                } catch (Exception e) {
                    throw new BusinessException(400, "步骤JSON解析失败: " + e.getMessage());
                }
            } else {
                // 兼容旧格式：从 description JSON 提取
                stepsFromDesc = parseStepsFromDescription(plan.getDescription());
                if (stepsFromDesc == null || stepsFromDesc.isEmpty())
                    throw new BusinessException(400, "方案步骤列表为空，请先在向导中编排步骤");
            }
        }

        Long firstTaskId = null;
        for (Long classId : classIds) {
            Long stageId = null;
            Classes cls = classesMapper.selectById(classId);
            if (cls != null) stageId = cls.getStageId();

            Task task = new Task();
            task.setSchoolId(1L);
            task.setStageId(stageId);
            task.setTitle(plan.getTitle());
            task.setTaskType("PRACTICE");
            task.setTargetType("CLASS");
            task.setTargetId(classId);
            task.setTeacherId(teacher.getId());
            task.setDescription(buildTaskDescription(stepsFromDesc, plan.getDescription()));
            // S9: 直接发布为 PUBLISHED，学生端立即可见
            task.setStatus("PUBLISHED");
            task.setScoreType("CUSTOM_RUBRIC");
            task.setAllowCustomSteps(1);
            task.setCreatedAt(LocalDateTime.now());
            taskMapper.insert(task);
            if (firstTaskId == null) firstTaskId = task.getId();

            // 为每个班级的 Task 创建 PracticeStep 模板 (studentId=null 表示模板)
            int idx = 0;
            for (Map<String, Object> sd : stepsFromDesc) {
                PracticeStep step = new PracticeStep();
                step.setTaskId(task.getId());
                step.setStudentId(null);
                step.setStepIndex(idx++);
                step.setTitle((String) sd.getOrDefault("title",
                    sd.getOrDefault("name", "步骤 " + idx)));
                step.setDescription((String) sd.getOrDefault("description", ""));
                // 新增：附件模式和模板文件
                String attMode = (String) sd.getOrDefault("attachmentMode", "REFERENCE");
                step.setAttachmentMode(attMode != null ? attMode : "REFERENCE");
                if (sd.get("referenceAttachments") != null) {
                    try {
                        step.setReferenceAttachments(om.writeValueAsString(sd.get("referenceAttachments")));
                    } catch (Exception ignored) {
                        log.warn("序列化referenceAttachments失败: stepIndex={}", idx);
                    }
                }
                if ("TEMPLATE".equals(attMode) && sd.get("templateFile") != null) {
                    step.setTemplateFile((String) sd.get("templateFile"));
                    step.setTemplateChecksum((String) sd.get("templateChecksum"));
                }
                stepMapper.insert(step);
            }
        }

        plan.setTaskId(firstTaskId);
        planMapper.updateById(plan);
        return Map.of("planId", planId, "taskCount", classIds.size(), "taskId", firstTaskId);
    }

    /** 手动从JSON字符串中提取steps数组（无Jackson依赖） */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractStepsManually(String s) {
        int idx = s.indexOf("\"steps\"");
        if (idx < 0) return List.of();
        idx = s.indexOf('[', idx);
        if (idx < 0) return List.of();
        int depth = 0, start = idx;
        for (int i = idx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) { idx = i + 1; break; } }
        }
        try { return om.readValue(s.substring(start, idx), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}); }
        catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseStepsFromDescription(String description) {
        if (description == null || description.isBlank()) return null;
        String s = description.trim();
        // 尝试 Jackson 解析整个 JSON
        try {
            Map<String, Object> root = om.readValue(s, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            Object stepsObj = root.get("steps");
            if (stepsObj instanceof List) return (List<Map<String, Object>>) stepsObj;
        } catch (Exception e) {
            // Jackson 解析失败，用字符串方式手动提取
        }
        // 手动提取 "steps" 数组：找 "steps": 然后括号匹配
        int idx = s.indexOf("\"steps\"");
        if (idx < 0) return null;
        // 跳过 "steps": 和空白
        idx = s.indexOf('[', idx);
        if (idx < 0) return null;
        int depth = 0, start = idx;
        for (int i = idx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) { idx = i + 1; break; } }
        }
        String stepsJson = s.substring(start, idx);
        try {
            return om.readValue(stepsJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private void validateStepSchema(List<Map<String, Object>> steps) {
        if (steps == null || steps.isEmpty())
            throw new BusinessException(400, "步骤列表不能为空");
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> s = steps.get(i);
            if (s.get("name") == null || s.get("name").toString().isBlank())
                throw new BusinessException(400, "步骤" + (i + 1) + "缺少名称字段");
        }
    }

    private void validateScoringItemSchema(Object scoringItems) {
        if (!(scoringItems instanceof List)) throw new BusinessException(400, "评分项格式错误");
        List<?> list = (List<?>) scoringItems;
        if (list.size() < 2) throw new BusinessException(400, "评分项至少需要2项");
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Map)) throw new BusinessException(400, "评分项" + (i + 1) + "格式错误");
            Map<?, ?> item = (Map<?, ?>) list.get(i);
            if (item.get("maxScore") == null) throw new BusinessException(400, "评分项" + (i + 1) + "缺少分值(maxScore)");
        }
    }

    @Override
    public List<PracticeRubric> getRubrics(Long planId) {
        return rubricMapper.selectList(
            new LambdaQueryWrapper<PracticeRubric>()
                .eq(PracticeRubric::getPlanId, planId)
                .orderByAsc(PracticeRubric::getSortOrder));
    }

    @Override
    @Transactional
    public void saveRubrics(Long planId, List<PracticeRubric> rubrics) {
        rubricMapper.delete(new LambdaQueryWrapper<PracticeRubric>().eq(PracticeRubric::getPlanId, planId));
        for (int i = 0; i < rubrics.size(); i++) {
            PracticeRubric r = rubrics.get(i);
            r.setId(null);
            r.setPlanId(planId);
            if (r.getSortOrder() == null) r.setSortOrder(i);
            rubricMapper.insert(r);
        }
    }

    // ═══════════ ZIP 导入 ═══════════

    @Override
    @Transactional
    public Map<String, Object> importFromZip(Long userId, MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            detectZipMagic(data);
            if (data.length > 10 * 1024 * 1024) throw new BusinessException(400, "文件超过10MB");
            Charset charset = detectZipCharset(data);

            Map<String, byte[]> entries = new LinkedHashMap<>();
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data), charset)) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    if (ze.isDirectory()) continue;
                    String name = ze.getName().replace('\\', '/');
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096]; int n;
                    while ((n = zis.read(buf)) != -1) bos.write(buf, 0, n);
                    entries.put(name, bos.toByteArray());
                }
            }

            String prefix = "";
            for (String k : entries.keySet()) {
                if (k.endsWith("plan.json")) {
                    prefix = k.substring(0, k.length() - "plan.json".length());
                    break;
                }
            }
            if (prefix.isEmpty()) throw new BusinessException(400, "ZIP中未找到plan.json");

            String planJson = new String(entries.get(prefix + "plan.json"), charset);
            @SuppressWarnings("unchecked")
            Map<String, Object> planData = om.readValue(planJson, Map.class);

            PracticePlan plan = new PracticePlan();
            plan.setTitle((String) planData.getOrDefault("title", "导入方案"));
            plan.setDescription((String) planData.getOrDefault("description", ""));
            plan.setPrerequisites(toJson(planData.get("prerequisites")));
            plan.setEnvironment(toJson(planData.get("environment")));
            plan.setSafetyNotes((String) planData.getOrDefault("safetyNotes", (String) planData.get("safety_notes")));
            plan.setTroubleshooting((String) planData.getOrDefault("troubleshooting", ""));
            plan.setScoringModel((String) planData.getOrDefault("scoringModel", planData.getOrDefault("scoring_model", "DUAL_DIMENSION")));
            plan.setCreatedBy(userId);
            plan.setCreateTime(LocalDateTime.now());
            planMapper.insert(plan);

            byte[] rubricBytes = entries.get(prefix + "rubric.json");
            if (rubricBytes != null) {
                String rubricJson = new String(rubricBytes, charset);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rubricList = om.readValue(rubricJson, List.class);
                int sort = 0;
                for (Map<String, Object> rm : rubricList) {
                    PracticeRubric r = new PracticeRubric();
                    r.setPlanId(plan.getId());
                    r.setDimension((String) rm.getOrDefault("dimension", "dim_" + sort));
                    r.setDimensionLabel((String) rm.getOrDefault("dimension_label", rm.getOrDefault("dimensionLabel", "维度" + sort).toString()));
                    r.setWeight(rm.get("weight") != null ? BigDecimal.valueOf(((Number) rm.get("weight")).doubleValue()) : BigDecimal.ZERO);
                    r.setCriteria(toJson(rm.get("criteria")));
                    r.setSortOrder(sort++);
                    rubricMapper.insert(r);
                }
            }

            int stepCount = 0;
            String stepsPrefix = prefix + "steps/";
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                if (!e.getKey().startsWith(stepsPrefix) || !e.getKey().endsWith(".md")) continue;
                String fileName = e.getKey().substring(stepsPrefix.length());
                String stepTitle = fileName.replaceAll("\\.md$", "").replaceAll("^\\d+_", "");
                String content = new String(e.getValue(), charset);

                PracticeStep step = new PracticeStep();
                step.setTaskId(null);
                step.setStudentId(null);
                step.setStepIndex(stepCount);
                step.setTitle(stepTitle);
                step.setDescription(content);
                stepMapper.insert(step);
                stepCount++;
            }

            return Map.of("planId", plan.getId(), "stepCount", stepCount, "rubricCount", rubricBytes != null ? 1 : 0);
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(400, "导入失败: " + e.getMessage()); }
    }

    // ═══════════ Excel 导入 ═══════════

    @Override
    @Transactional
    public Map<String, Object> importFromExcel(Long userId, MultipartFile file) {
        try {
            Map<String, PracticePlan> planMap = new LinkedHashMap<>();
            Map<Long, List<Map<String, Object>>> planSteps = new HashMap<>();
            java.util.concurrent.atomic.AtomicInteger planCount = new java.util.concurrent.atomic.AtomicInteger(0);

            com.alibaba.excel.EasyExcel.read(file.getInputStream(), new com.alibaba.excel.read.listener.ReadListener<Map<Integer, String>>() {
                boolean header = true;
                @Override public void invoke(Map<Integer, String> row, com.alibaba.excel.context.AnalysisContext ctx) {
                    if (header) { header = false; return; }
                    String title = row.getOrDefault(0, "");
                    if (title.isBlank()) return;
                    String desc = row.getOrDefault(1, "");
                    int seq = 0;
                    try { seq = Integer.parseInt(row.getOrDefault(2, "0")); } catch (Exception ignored) { /* 预期内可忽略: seq列非数字时默认为0 */ }
                    String stepName = row.getOrDefault(3, "");
                    String stepDesc = row.getOrDefault(4, "");
                    String rubricDim = row.getOrDefault(5, "");
                    double rubricW = 0;
                    try { rubricW = Double.parseDouble(row.getOrDefault(6, "0")); } catch (Exception ignored) { /* 预期内可忽略: 权重列非数字时默认为0 */ }

                    PracticePlan plan = planMap.computeIfAbsent(title, k -> {
                        PracticePlan p = new PracticePlan();
                        p.setTitle(title);
                        p.setDescription(desc);
                        p.setCreatedBy(userId);
                        p.setCreateTime(LocalDateTime.now());
                        planMapper.insert(p);
                        planCount.incrementAndGet();
                        return p;
                    });

                    if (!stepName.isBlank()) {
                        Map<String, Object> sm = new LinkedHashMap<>();
                        sm.put("name", stepName);
                        sm.put("description", stepDesc);
                        sm.put("seq", seq);
                        planSteps.computeIfAbsent(plan.getId(), k -> new ArrayList<>()).add(sm);
                    }

                    if (!rubricDim.isBlank()) {
                        PracticeRubric r = new PracticeRubric();
                        r.setPlanId(plan.getId());
                        r.setDimension(rubricDim);
                        r.setDimensionLabel(rubricDim);
                        r.setWeight(BigDecimal.valueOf(rubricW));
                        rubricMapper.insert(r);
                    }
                }
                @Override public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext ctx) {
                    for (PracticePlan plan : planMap.values()) {
                        List<Map<String, Object>> steps = planSteps.get(plan.getId());
                        if (steps != null && !steps.isEmpty()) {
                            try {
                                Map<String, Object> descMap = new LinkedHashMap<>();
                                // 保留原有 description 文本
                                if (plan.getDescription() != null && !plan.getDescription().isBlank()) {
                                    descMap.put("originalDesc", plan.getDescription());
                                }
                                descMap.put("steps", steps);
                                plan.setDescription(om.writeValueAsString(descMap));
                                planMapper.updateById(plan);
                            } catch (Exception ignored) {
                                log.warn("序列化方案描述失败: planId={}", plan.getId());
                            }
                        }
                    }
                }
            }).sheet().doRead();

            return Map.of("count", planCount.get());
        } catch (Exception e) {
            throw new BusinessException(400, "Excel导入失败: " + e.getMessage());
        }
    }

    // ═══════════ 工具 ═══════════

    private void detectZipMagic(byte[] data) {
        if (data.length < 4 || data[0] != 'P' || data[1] != 'K')
            throw new BusinessException(400, "文件不是有效的ZIP格式");
    }

    private Charset detectZipCharset(byte[] data) {
        try {
            String sample = new String(data, 0, Math.min(data.length, 4096), "UTF-8");
            if (sample.contains("plan.json")) return Charset.forName("UTF-8");
        } catch (Exception ignored) { /* 预期内可忽略: UTF-8探测失败则回退GBK */ }
        return Charset.forName("GBK");
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try { return om.writeValueAsString(obj); } catch (Exception e) { return null; }
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) { try { return Long.parseLong(s); } catch (Exception ignored) { /* 预期内可忽略: 非数字字符串返回null */ } }
        return null;
    }

    /** 将步骤列表+JSON描述转为结构化的Markdown任务描述（学生可见） */
    private String buildTaskDescription(List<Map<String, Object>> steps, String rawDescription) {
        StringBuilder sb = new StringBuilder();
        // 尝试从rawDescription JSON中提取原始描述文本
        try {
            Map<String, Object> descMap = om.readValue(rawDescription, Map.class);
            Object originalDesc = descMap.get("originalDesc");
            if (originalDesc != null && !originalDesc.toString().isBlank()) {
                sb.append(originalDesc.toString()).append("\n\n");
            }
        } catch (Exception ignored) { /* 预期内可忽略: description非JSON格式时跳过解析 */ }

        if (steps != null) {
            for (Map<String, Object> step : steps) {
                String name = (String) step.getOrDefault("name", "");
                String desc = (String) step.getOrDefault("description", "");
                sb.append("### ").append(name).append("\n\n");
                if (desc != null && !desc.isBlank()) {
                    // description可能有Markdown格式，直接输出
                    sb.append(desc).append("\n\n");
                }
                sb.append("---\n\n");
            }
        }
        return sb.toString().trim();
    }

    @Override
    @Transactional
    public Map<String, Object> batchImportFromMarkdown(Long userId, String markdown) {
        if (markdown == null || markdown.isBlank()) throw new BusinessException(400, "内容为空");
        Teacher teacher = teacherMapper.selectOne(
            new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        String[] parts = markdown.split("\\n(?=## )");
        int imported = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            String[] lines = trimmed.split("\\n");
            String title = lines[0].replaceAll("^#{2,}\\s*", "").trim();
            if (title.isEmpty()) continue;
            StringBuilder descBuilder = new StringBuilder();
            boolean inSteps = false;
            boolean inScoring = false;
            List<Map<String, Object>> simpleSteps = new ArrayList<>();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.matches("^###\\s+步骤.*")) { inSteps = true; inScoring = false; continue; }
                if (line.matches("^###\\s+评分.*")) { inScoring = true; inSteps = false; continue; }
                if (inSteps) {
                    String stepText = line.replaceAll("^\\d+[\\.\\)]\\s*|^[-*]\\s*", "").trim();
                    if (!stepText.isEmpty()) simpleSteps.add(Map.of("name", stepText.length() > 30 ? stepText.substring(0, 30) : stepText, "description", stepText));
                } else {
                    if (!line.trim().isEmpty() && !line.startsWith("#")) descBuilder.append(line).append("\n");
                }
            }
            PracticePlan plan = new PracticePlan();
            plan.setTitle(title);
            plan.setDescription(descBuilder.toString().trim());
            plan.setSimpleMode(true);
            plan.setScoringModel("DUAL_DIMENSION");
            plan.setCreatedBy(userId);
            plan.setCreateTime(LocalDateTime.now());
            if (teacher != null) plan.setSubject(teacher.getSubject());
            planMapper.insert(plan);
            List<PracticeRubric> rubrics = buildDefaultBatchRubrics(plan.getId());
            if (rubrics != null) saveRubrics(plan.getId(), rubrics);
            results.add(Map.of("id", plan.getId(), "title", title, "stepCount", simpleSteps.isEmpty() ? 1 : simpleSteps.size()));
            imported++;
        }
        return Map.of("imported", imported, "plans", results);
    }

    private List<PracticeRubric> buildDefaultBatchRubrics(Long planId) {
        List<PracticeRubric> list = new ArrayList<>();
        Object[][] dims = {{"completeness", "完成度", 0.4}, {"quality", "作品质量", 0.3}, {"report", "报告规范", 0.3}};
        for (int i = 0; i < dims.length; i++) {
            PracticeRubric r = new PracticeRubric();
            r.setPlanId(planId);
            r.setDimension((String) dims[i][0]);
            r.setDimensionLabel((String) dims[i][1]);
            r.setWeight(BigDecimal.valueOf((Double) dims[i][2]));
            r.setSortOrder(i);
            list.add(r);
        }
        return list;
    }

    @Override
    public List<PracticePlan> listSharedBySubject(String subject) {
        if (subject == null || subject.isBlank()) return List.of();
        return planMapper.selectList(
            new LambdaQueryWrapper<PracticePlan>()
                .eq(PracticePlan::getShared, true)
                .eq(PracticePlan::getSubject, subject)
                .orderByDesc(PracticePlan::getUpdateTime));
    }

    @Override
    public long countBySubject(String subject) {
        if (subject == null || subject.isBlank()) return 0;
        return planMapper.selectCount(
            new LambdaQueryWrapper<PracticePlan>()
                .eq(PracticePlan::getSubject, subject));
    }

    @Override
    public Map<String, Object> aiGeneratePlan(Long userId, String title, String subject,
            String requirements, String stageHint) {
        if (title == null || title.isBlank()) throw new BusinessException(400, "标题不能为空");

        Map<String, Object> aiParams = new LinkedHashMap<>();
        aiParams.put("title", title);
        aiParams.put("subject", subject != null ? subject : "");
        aiParams.put("requirements", requirements != null ? requirements : "");
        aiParams.put("stageHint", stageHint != null ? stageHint : "中职");

        try {
            // 调用 generatePracticePlan，如果返回 null 则使用默认结果
            String aiResult;
            try {
                aiResult = aiContentGeneratorService.generatePracticePlan(aiParams);
            } catch (Exception methodNotFound) {
                aiResult = null;
            }

            if (aiResult == null || aiResult.isBlank())
                return buildDefaultAiResult(title);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = om.readValue(aiResult, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) result.getOrDefault("steps", List.of());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rubrics = (List<Map<String, Object>>) result.getOrDefault("rubrics", List.of());

            for (Map<String, Object> step : steps) {
                step.putIfAbsent("attachmentMode", "REFERENCE");
            }

            return Map.of("steps", steps, "rubrics", rubrics);
        } catch (BusinessException e) { throw e; }
        catch (Exception e) {
            throw new BusinessException(500, "AI 生成失败: " + e.getMessage());
        }
    }

    /** AI服务不可用时的默认回退 */
    private Map<String, Object> buildDefaultAiResult(String title) {
        return Map.of(
            "steps", List.of(
                Map.of("title", "理解任务要求", "description", "阅读实训任务说明，理解目标和要求", "attachmentMode", "REFERENCE"),
                Map.of("title", "准备工具和材料", "description", "根据任务需求准备所需的软件工具和素材", "attachmentMode", "REFERENCE"),
                Map.of("title", "执行核心操作", "description", "按照操作步骤完成实训核心内容", "attachmentMode", "REFERENCE"),
                Map.of("title", "检查和完善", "description", "对照要求逐项检查，修改完善", "attachmentMode", "REFERENCE"),
                Map.of("title", "提交作品", "description", "导出最终作品并提交，撰写实训总结", "attachmentMode", "REFERENCE")
            ),
            "rubrics", List.of()
        );
    }
}
