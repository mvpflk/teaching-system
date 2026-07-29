package com.school.teaching.service.impl;

import static com.school.teaching.service.impl.KnowledgeNodeHelper.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.DictSubject;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.TeacherClass;
import com.school.teaching.entity.WrongQuestion;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.DictSubjectMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.TeacherClassMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AiServiceGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

@Slf4j
@Service
public class KnowledgeNodeManageService {

    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private QuestionBankMapper bankMapper;
    @Autowired private DictSubjectMapper dictSubjectMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;
    @Autowired private com.school.teaching.mapper.AiOutputMapper aiOutputMapper;
    @Autowired private WrongQuestionMapper wrongQuestionMapper;
    @Autowired private AiServiceGateway aiGateway;
    @Autowired private KnowledgeNodeImportHelper importHelper;
    @Autowired private KnowledgeNodeReadService readService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int LV_SUBJECT = 1;
    private static final int LV_CHAPTER = 2;
    private static final int LV_TASK    = 3;
    private static final int LV_KP      = 4;

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "knowledge_tree", allEntries = true)
    public KnowledgeNode create(KnowledgeNode node) {
        if (node.getLevel() == null) node.setLevel(LV_CHAPTER);
        Long effectiveParent = node.getParentId() != null ? node.getParentId() : 0L;
        long dup = nodeMapper.selectCount(new LambdaQueryWrapper<KnowledgeNode>()
            .eq(KnowledgeNode::getParentId, node.getParentId())
            .eq(KnowledgeNode::getName, node.getName()));
        if (dup > 0) throw new BusinessException(400, "该层级下已存在同名节点: " + node.getName());
        nodeMapper.insert(node);
        return node;
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "knowledge_tree", allEntries = true)
    public KnowledgeNode update(Long id, KnowledgeNode node) {
        node.setId(id);
        nodeMapper.updateById(node);
        return node;
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "knowledge_tree", allEntries = true)
    public void delete(Long id) {
        Long subjectId = readService.findSubjectRoot(id);
        List<Long> ids = readService.collectChildIds(id, subjectId);
        ids.add(id);
        long count = bankMapper.selectCount(new LambdaQueryWrapper<QuestionBank>()
                .in(QuestionBank::getCategoryId, ids).ne(QuestionBank::getStatus, -1));
        if (count > 0)
            throw new BusinessException(400, "该节点或其子节点下有 " + count + " 道正式题目，请先移除或重新分类后再删除");
        bankMapper.delete(new LambdaQueryWrapper<QuestionBank>()
                .in(QuestionBank::getCategoryId, ids).eq(QuestionBank::getStatus, 0));
        nodeMapper.deleteBatchIds(ids);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "knowledge_tree", allEntries = true)
    public void setContent(Long nodeId, String markdownContent) {
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) throw new BusinessException(404, "知识节点不存在");

        String role = SecurityUtils.getCurrentRole();
        boolean isAdmin = "SUPER_ADMIN".equals(role) || "ADMIN".equals(role) || "HEAD_TEACHER".equals(role);
        if (!isAdmin) {
            Long userId = SecurityUtils.getCurrentUserId();
            List<TeacherClass> assignments = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, userId));
            List<Long> teachingSubjectIds = new ArrayList<>();
            for (TeacherClass a : assignments) {
                if (a.getSubject() == null || a.getSubject().isEmpty()) continue;
                for (String subName : a.getSubject().split("[,，、]")) {
                    String trimmed = subName.trim();
                    if (trimmed.isEmpty()) continue;
                    DictSubject ds = dictSubjectMapper.selectOne(
                            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getSubjectName, trimmed)
                                    .last("LIMIT 1"));
                    if (ds != null) teachingSubjectIds.add(ds.getId());
                }
            }
            Long rootSubjectId = readService.findSubjectRoot(nodeId);
            if (!teachingSubjectIds.contains(rootSubjectId)) {
                throw new BusinessException(403, "您不教授该学科，无法编辑此知识库内容");
            }
        }
        node.setContent(markdownContent);
        nodeMapper.updateById(node);
    }

    @Transactional
    public int importFromExcel(MultipartFile file) throws Exception {
        int count = 0;
        try (org.apache.poi.ss.usermodel.Workbook wb =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);

            Map<String, Long> subjectCache = new HashMap<>();
            List<KnowledgeNode> rootNodes = nodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>().eq(KnowledgeNode::getLevel, LV_SUBJECT));
            for (KnowledgeNode rn : rootNodes) {
                subjectCache.put(rn.getName(), rn.getId());
            }
            Map<Long, Map<String, Long>> chapterCache = new HashMap<>();
            Map<Long, Map<Long, Map<String, Long>>> taskCache = new HashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;
                String subjectName = getCellStr(row, 0);
                String chapterName = getCellStr(row, 1);
                String taskName = getCellStr(row, 2);
                String kpName = getCellStr(row, 3);
                String content = getCellStr(row, 4);
                if (subjectName.isBlank() || chapterName.isBlank()) continue;

                boolean isNewFormat = !taskName.isBlank() && !kpName.isBlank();
                if (!isNewFormat) {
                    kpName = taskName;
                    content = kpName;
                }
                if (kpName == null || kpName.isBlank()) continue;

                Long subjectNodeId = subjectCache.computeIfAbsent(subjectName, name -> {
                    DictSubject s = dictSubjectMapper.selectOne(
                            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getSubjectName, name)
                                    .last("LIMIT 1"));
                    if (s == null) {
                        log.warn("导入跳过: 学科'{}'不在dict_subject表中", name);
                        return null;
                    }
                    KnowledgeNode exist = nodeMapper.selectOne(
                            new LambdaQueryWrapper<KnowledgeNode>()
                                    .eq(KnowledgeNode::getLevel, LV_SUBJECT)
                                    .eq(KnowledgeNode::getSubjectId, s.getId())
                                    .last("LIMIT 1"));
                    if (exist != null) return exist.getId();
                    KnowledgeNode n = new KnowledgeNode();
                    n.setLevel(LV_SUBJECT); n.setName(name);
                    n.setSubjectId(s.getId());
                    n.setSortOrder(0);
                    nodeMapper.insert(n);
                    return n.getId();
                });
                if (subjectNodeId == null) continue;

                Map<String, Long> chapMap = chapterCache.computeIfAbsent(subjectNodeId, k -> new HashMap<>());
                Long chapterId = chapMap.computeIfAbsent(chapterName, name -> {
                    KnowledgeNode exist = nodeMapper.selectOne(
                            new LambdaQueryWrapper<KnowledgeNode>()
                                    .eq(KnowledgeNode::getParentId, subjectNodeId)
                                    .eq(KnowledgeNode::getLevel, LV_CHAPTER)
                                    .eq(KnowledgeNode::getName, name)
                                    .last("LIMIT 1"));
                    if (exist != null) return exist.getId();
                    KnowledgeNode n = new KnowledgeNode();
                    n.setParentId(subjectNodeId); n.setSubjectId(
                            nodeMapper.selectById(subjectNodeId) != null ?
                                    nodeMapper.selectById(subjectNodeId).getSubjectId() : null);
                    n.setLevel(LV_CHAPTER); n.setName(name); n.setSortOrder(0);
                    nodeMapper.insert(n);
                    return n.getId();
                });

                Long parentId;
                int kpLevel;
                if (isNewFormat) {
                    Map<Long, Map<String, Long>> chapTaskMap = taskCache.computeIfAbsent(
                            subjectNodeId, k -> new HashMap<>());
                    Map<String, Long> taskMap = chapTaskMap.computeIfAbsent(chapterId, k -> new HashMap<>());
                    Long taskId = taskMap.computeIfAbsent(taskName, name -> {
                        KnowledgeNode exist = nodeMapper.selectOne(
                                new LambdaQueryWrapper<KnowledgeNode>()
                                        .eq(KnowledgeNode::getParentId, chapterId)
                                        .eq(KnowledgeNode::getLevel, LV_TASK)
                                        .eq(KnowledgeNode::getName, name)
                                        .last("LIMIT 1"));
                        if (exist != null) return exist.getId();
                        KnowledgeNode n = new KnowledgeNode();
                        n.setParentId(chapterId); n.setLevel(LV_TASK);
                        n.setName(name); n.setSortOrder(0);
                        nodeMapper.insert(n);
                        return n.getId();
                    });
                    parentId = taskId;
                    kpLevel = LV_KP;
                } else {
                    parentId = chapterId;
                    kpLevel = LV_TASK;
                }

                KnowledgeNode kp = new KnowledgeNode();
                kp.setParentId(parentId);
                kp.setName(kpName); kp.setLevel(kpLevel);
                kp.setContent(content != null && !content.isBlank() ? content : null);
                kp.setSortOrder(0);
                nodeMapper.insert(kp); count++;
            }
        }
        return count;
    }

    @Transactional
    public int importFromZip(Long subjectId, MultipartFile zipFile) throws Exception {
        byte[] header = new byte[4];
        try (var is = zipFile.getInputStream()) {
            int read = is.read(header);
            if (read < 4 || header[0] != 0x50 || header[1] != 0x4B)
                throw new BusinessException(400, "仅支持 ZIP 格式文件（.zip）");
        }
        if (zipFile.getSize() > 20 * 1024 * 1024)
            throw new BusinessException(400, "ZIP 文件不能超过 20MB");

        DictSubject subject = dictSubjectMapper.selectById(subjectId);
        if (subject == null) throw new BusinessException(404, "学科不存在");

        KnowledgeNode subjectNode = nodeMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getLevel, LV_SUBJECT)
                        .eq(KnowledgeNode::getSubjectId, subjectId).last("LIMIT 1"));
        if (subjectNode == null) {
            subjectNode = new KnowledgeNode();
            subjectNode.setSubjectId(subjectId); subjectNode.setLevel(LV_SUBJECT);
            subjectNode.setName(subject.getSubjectName()); subjectNode.setSortOrder(0);
            nodeMapper.insert(subjectNode);
        }

        List<KnowledgeNode> existing = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getSubjectId, subjectId));
        Map<String, KnowledgeNode> chapterMap = new LinkedHashMap<>();
        Map<String, KnowledgeNode> taskMap = new LinkedHashMap<>();
        Map<String, KnowledgeNode> kpMap = new LinkedHashMap<>();
        for (KnowledgeNode n : existing) {
            if (n.getName() == null) continue;
            if (n.getLevel() != null && n.getLevel() == LV_CHAPTER) {
                chapterMap.put(n.getName().trim(), n);
            } else if (n.getLevel() != null && n.getLevel() == LV_TASK && n.getParentId() != null) {
                taskMap.put((n.getParentId() + ":" + n.getName()).trim(), n);
            } else {
                kpMap.put((n.getParentId() + ":" + n.getName()).trim(), n);
            }
        }

        byte[] zipBytes = zipFile.getBytes();
        Charset zipCharset = detectZipCharset(zipBytes);

        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), zipCharset)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                String nameLow = name.toLowerCase();
                if (!nameLow.endsWith(".md") && !nameLow.endsWith(".txt")) continue;
                if (name.contains("..")) continue;

                String[] parts = name.replace('\\', '/').split("/");
                if (parts.length < 2) continue;

                int startIdx = 0;
                String subjectName = subject.getSubjectName();
                if (subjectName != null && parts[0].trim().equals(subjectName.trim())) {
                    startIdx = 1;
                    if (parts.length <= startIdx + 1) continue;
                }

                String chapterName = parts[startIdx].trim();
                String fileName = parts[parts.length - 1].trim();
                if (fileName.toLowerCase().endsWith(".md"))
                    fileName = fileName.substring(0, fileName.length() - 3).trim();
                else if (fileName.toLowerCase().endsWith(".txt"))
                    fileName = fileName.substring(0, fileName.length() - 4).trim();
                if (chapterName.isEmpty() || fileName.isEmpty()) continue;

                int effectiveLen = parts.length - startIdx;
                boolean isNewFormat = effectiveLen >= 3;
                String taskName = isNewFormat ? parts[startIdx + 1].trim() : null;
                String kpName = fileName;

                byte[] bytes = zis.readAllBytes();
                if (bytes.length > 500 * 1024) continue;
                String content = decodeContent(bytes);
                if (!isValidContent(content, fileName)) continue;

                KnowledgeNode chapter = chapterMap.get(chapterName);
                if (chapter == null) {
                    chapter = new KnowledgeNode();
                    chapter.setSubjectId(subjectId);
                    chapter.setParentId(subjectNode.getId());
                    chapter.setName(chapterName);
                    chapter.setLevel(LV_CHAPTER); chapter.setSortOrder(0);
                    nodeMapper.insert(chapter);
                    chapterMap.put(chapterName, chapter);
                }

                Long parentId;
                int kpLevel;
                if (isNewFormat && taskName != null && !taskName.isEmpty()) {
                    String tkKey = chapter.getId() + ":" + taskName;
                    KnowledgeNode task = taskMap.get(tkKey);
                    if (task == null) {
                        task = new KnowledgeNode();
                        task.setSubjectId(subjectId);
                        task.setParentId(chapter.getId());
                        task.setName(taskName);
                        task.setLevel(LV_TASK); task.setSortOrder(0);
                        nodeMapper.insert(task);
                        taskMap.put(tkKey, task);
                    }
                    parentId = task.getId();
                    kpLevel = LV_KP;
                } else {
                    parentId = chapter.getId();
                    kpLevel = LV_TASK;
                }

                String kpKey = parentId + ":" + kpName;
                KnowledgeNode kp = kpMap.get(kpKey);
                if (kp == null) {
                    kp = new KnowledgeNode();
                    kp.setSubjectId(subjectId);
                    kp.setParentId(parentId);
                    kp.setName(kpName);
                    kp.setLevel(kpLevel); kp.setSortOrder(0);
                    kp.setContent(content);
                    nodeMapper.insert(kp);
                    kpMap.put(kpKey, kp);
                } else {
                    kp.setContent(content);
                    nodeMapper.updateById(kp);
                }
                count++;
                zis.closeEntry();
            }
        }
        return count;
    }

    @Transactional
    public int importFromTxt(Long subjectId, MultipartFile file) throws Exception {
        DictSubject subject = dictSubjectMapper.selectById(subjectId);
        if (subject == null) throw new BusinessException(404, "学科不存在");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new BusinessException(400, "TXT 文件不能超过 10MB");

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return importHelper.doImportFromLines(subjectId, subject, content);
    }

    @Transactional
    public int importFromDocx(Long subjectId, MultipartFile file) throws Exception {
        DictSubject subject = dictSubjectMapper.selectById(subjectId);
        if (subject == null) throw new BusinessException(404, "学科不存在");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new BusinessException(400, "Word 文件不能超过 10MB");

        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph p : paragraphs) {
                String style = p.getStyle();
                String text = p.getText().trim();
                if (text.isEmpty()) continue;

                if (style != null && style.startsWith("Heading")) {
                    int level = Integer.parseInt(style.replace("Heading", "").trim());
                    if (level >= 1 && level <= 3) {
                        sb.append("#".repeat(level)).append(" ").append(text).append("\n");
                        continue;
                    }
                }
                if (isLikelyHeading(p)) {
                    sb.append("# ").append(text).append("\n");
                    continue;
                }
                sb.append(text).append("\n");
            }
        } catch (java.io.IOException e) {
            throw new BusinessException(400, "无法解析 Word 文件：" + e.getMessage());
        }
        return importHelper.doImportFromLines(subjectId, subject, sb.toString());
    }

    @Transactional
    public void clearBySubject(Long subjectId) {
        if (subjectId == null) throw new BusinessException(400, "请指定学科");

        KnowledgeNode subjectNode = nodeMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getLevel, LV_SUBJECT)
                        .eq(KnowledgeNode::getSubjectId, subjectId).last("LIMIT 1"));
        if (subjectNode == null) return;

        List<Long> allIds = readService.collectChildIds(subjectNode.getId(), subjectId);
        if (allIds.isEmpty()) return;

        aiOutputMapper.delete(new LambdaQueryWrapper<com.school.teaching.entity.AiOutput>()
                .in(com.school.teaching.entity.AiOutput::getNodeId, allIds));

        List<Long> bankIds = bankMapper.selectList(new LambdaQueryWrapper<QuestionBank>()
                .select(QuestionBank::getId)
                .in(QuestionBank::getCategoryId, allIds))
                .stream().map(QuestionBank::getId).collect(Collectors.toList());
        if (!bankIds.isEmpty()) {
            wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                    .in(WrongQuestion::getQuestionId, bankIds));
        }
        bankMapper.delete(new LambdaQueryWrapper<QuestionBank>()
                .in(QuestionBank::getCategoryId, allIds)
                .ne(QuestionBank::getStatus, 0));

        nodeMapper.deleteBatchIds(allIds);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "knowledge_tree", allEntries = true)
    public Map<String, Object> generateLearningResources(Long nodeId) {
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) throw new BusinessException(404, "知识节点不存在");

        Long subjectId = readService.findSubjectRoot(nodeId);
        if (subjectId == null) throw new BusinessException(400, "无法确定节点所属学科");
        DictSubject subject = dictSubjectMapper.selectById(subjectId);
        String subjectName = subject != null ? subject.getSubjectName() : null;
        if (subjectName == null) throw new BusinessException(400, "无法确定节点所属学科");

        String prompt = buildLearningResourcePrompt(node, subjectName);
        Map<String, Object> aiParams = new HashMap<>();
        aiParams.put("prompt", prompt);
        aiParams.put("temperature", 0.7);
        aiParams.put("maxTokens", 3000);

        String aiJson = callAiWithRetry(aiParams, 3, 3);
        log.info("AI返回长度={}, 前150字={}", aiJson.length(), aiJson.substring(0, Math.min(150, aiJson.length())));
        validateLearningResourcesJson(aiJson);

        String nodePath = readService.getNodeFullPath(nodeId);
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(aiJson);
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.convertValue(root, Map.class);
            Map<String, Object> enriched = new LinkedHashMap<>(raw);

            if (enriched.containsKey("videoSearchKeywords") && !enriched.containsKey("videoUrls")) {
                @SuppressWarnings("unchecked")
                List<String> kws = (List<String>) enriched.get("videoSearchKeywords");
                List<Map<String, String>> videoUrls = new ArrayList<>();
                for (String kw : kws) {
                    Map<String, String> v = new LinkedHashMap<>();
                    v.put("title", kw);
                    try {
                        v.put("url", "https://search.bilibili.com/all?keyword=" +
                            URLEncoder.encode(kw, "UTF-8"));
                    } catch (Exception ignored) {
                        v.put("url", "https://search.bilibili.com/all?keyword=" + kw);
                    }
                    v.put("platform", "bilibili");
                    videoUrls.add(v);
                }
                enriched.put("videoUrls", videoUrls);
                enriched.remove("videoSearchKeywords");
            }
            enriched.put("aiGenerated", true);
            enriched.put("generatedAt", LocalDateTime.now().toString());
            enriched.put("subjectName", subjectName);
            enriched.put("nodeName", node.getName());
            enriched.put("nodePath", nodePath);

            if (enriched.containsKey("videoUrls")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> vidList = (List<Map<String, String>>) enriched.get("videoUrls");
                if (vidList != null && !vidList.isEmpty()) {
                    enriched.put("videoUrl", vidList.get(0).get("url"));
                }
            }

            if (enriched.containsKey("examples") || enriched.containsKey("practices")) {
                List<Long> exampleIds = new ArrayList<>();
                List<Long> practiceIds = new ArrayList<>();
                String subj = subjectName;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> examples = (List<Map<String, Object>>) enriched.get("examples");
                if (examples != null) {
                    for (Map<String, Object> ex : examples) {
                        try {
                            QuestionBank qb = new QuestionBank();
                            qb.setCategoryId(nodeId);
                            qb.setSubject(subj);
                            qb.setQuestionType("FILL_IN");
                            qb.setQuestionText(String.valueOf(ex.getOrDefault("question", "")));
                            qb.setCorrectAnswer(String.valueOf(ex.getOrDefault("answer", "")));
                            qb.setExplanation(String.valueOf(ex.getOrDefault("explanation", "")));
                            qb.setStatus(1);
                            qb.setDifficultyLevel(2);
                            qb.setSchoolId(1L);
                            bankMapper.insert(qb);
                            exampleIds.add(qb.getId());
                        } catch (Exception e) {
                            log.warn("将AI例题写入question_bank失败: {}", e.getMessage());
                        }
                    }
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> practices = (List<Map<String, Object>>) enriched.get("practices");
                if (practices != null) {
                    for (Map<String, Object> pr : practices) {
                        try {
                            QuestionBank qb = new QuestionBank();
                            qb.setCategoryId(nodeId);
                            qb.setSubject(subj);
                            qb.setQuestionType("FILL_IN");
                            qb.setQuestionText(String.valueOf(pr.getOrDefault("question", "")));
                            qb.setCorrectAnswer(String.valueOf(pr.getOrDefault("answer", "")));
                            Object diff = pr.get("difficulty");
                            qb.setDifficultyLevel(diff instanceof Number ? ((Number) diff).intValue() : 2);
                            qb.setStatus(1);
                            qb.setSchoolId(1L);
                            bankMapper.insert(qb);
                            practiceIds.add(qb.getId());
                        } catch (Exception e) {
                            log.warn("将AI练习题写入question_bank失败: {}", e.getMessage());
                        }
                    }
                }

                if (!exampleIds.isEmpty()) enriched.put("exampleIds", exampleIds);
                if (!practiceIds.isEmpty()) enriched.put("practiceIds", practiceIds);
            }

            String lrJson = objectMapper.writeValueAsString(enriched);
            node.setLearningResources(lrJson);
            node.setResourceGeneratedAt(LocalDateTime.now());
            node.setResourceStatus("PENDING");
            node.setResourceRejectReason(null);
            int newVersion = (node.getResourceVersion() != null ? node.getResourceVersion() : 0) + 1;
            node.setResourceVersion(newVersion);
            nodeMapper.update(null, new LambdaUpdateWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getId, nodeId)
                .set(KnowledgeNode::getLearningResources, lrJson)
                .set(KnowledgeNode::getResourceGeneratedAt, node.getResourceGeneratedAt())
                .set(KnowledgeNode::getResourceStatus, "PENDING")
                .set(KnowledgeNode::getResourceRejectReason, null)
                .set(KnowledgeNode::getResourceVersion, newVersion));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("nodeId", nodeId);
            result.put("subject", subjectName);
            result.put("learningResources", enriched);
            result.put("resourceStatus", "PENDING");
            result.put("resourceVersion", newVersion);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("处理AI返回内容失败 nodeId={}", nodeId, e);
            throw new BusinessException(500, "AI返回内容解析失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "knowledge_tree", allEntries = true)
    public void reviewResource(Long nodeId, String status, String rejectReason) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status))
            throw new BusinessException(400, "审核状态只能为 APPROVED 或 REJECTED");

        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) throw new BusinessException(404, "知识节点不存在");
        if (node.getLearningResources() == null || node.getLearningResources().isEmpty())
            throw new BusinessException(400, "该节点尚未生成学习资源");

        node.setResourceStatus(status);
        node.setResourceRejectReason("REJECTED".equals(status) ? rejectReason : null);
        node.setResourceGeneratedAt(LocalDateTime.now());
        nodeMapper.update(null, new LambdaUpdateWrapper<KnowledgeNode>()
            .eq(KnowledgeNode::getId, nodeId)
            .set(KnowledgeNode::getResourceStatus, status)
            .set(KnowledgeNode::getResourceRejectReason, "REJECTED".equals(status) ? rejectReason : null)
            .set(KnowledgeNode::getResourceGeneratedAt, LocalDateTime.now()));
    }

    public List<Map<String, Object>> checkVideoLinks() {
        List<Map<String, Object>> deadLinks = new ArrayList<>();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<KnowledgeNode> nodes = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getResourceStatus, "APPROVED")
                .and(w -> w.isNull(KnowledgeNode::getVideoCheckedAt)
                    .or().lt(KnowledgeNode::getVideoCheckedAt, sevenDaysAgo)));

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        for (KnowledgeNode node : nodes) {
            try {
                com.fasterxml.jackson.databind.JsonNode lr = objectMapper.readTree(node.getLearningResources());
                com.fasterxml.jackson.databind.JsonNode videos = lr.get("videoUrls");
                if (videos == null || !videos.isArray()) continue;

                for (com.fasterxml.jackson.databind.JsonNode v : videos) {
                    String url = v.get("url").asText();
                    String title = v.has("title") ? v.get("title").asText() : "";
                    String platform = v.has("platform") ? v.get("platform").asText() : "other";

                    boolean ok = checkSingleVideo(platform, url, httpClient);
                    if (!ok) {
                        deadLinks.add(Map.of(
                            "nodeId", node.getId(),
                            "nodeName", node.getName(),
                            "videoTitle", title,
                            "videoUrl", url,
                            "platform", platform,
                            "checkedAt", LocalDateTime.now().toString()
                        ));
                    }
                }
                nodeMapper.update(null,
                    new LambdaUpdateWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getId, node.getId())
                        .set(KnowledgeNode::getVideoCheckedAt, LocalDateTime.now()));
            } catch (Exception e) {
                log.warn("检查节点{}视频链接失败: {}", node.getId(), e.getMessage());
            }
        }
        return deadLinks;
    }

    private String buildLearningResourcePrompt(KnowledgeNode node, String subjectName) {
        String role;
        String contentFocus;
        String stageHint = "";
        if (subjectName.contains("[职高]")) stageHint = "职高";
        else if (subjectName.contains("[普高]")) stageHint = "普高";
        else stageHint = "中职";
        switch (subjectName) {
            case "数学": case "数学[职高]":
                role = stageHint + "数学辅导老师";
                contentFocus = "含LaTeX公式的例题(≥2道)+练习(≥3道)+常见误区+B站数学视频推荐(B站搜索关键词如'职高数学 集合运算 教学',≥2个)";
                break;
            case "语文": case "语文[职高]":
                role = stageHint + "语文辅导老师";
                contentFocus = "古文逐句解析+作文素材+阅读技巧+常考题型分析";
                break;
            case "英语": case "英语[职高]":
                role = stageHint + "英语辅导老师";
                contentFocus = "语法精讲+完形填空技巧+单词记忆口诀+真题示例";
                break;
            default:
                role = stageHint + subjectName + "辅导老师";
                contentFocus = "核心概念讲解+实操要点+常见考题+拓展阅读建议";
        }

        String nodePath = readService.getNodeFullPath(node.getId());
        String existingContent = node.getContent() != null ? node.getContent() : "暂无";

        return String.format(
            "你是一位%s。请为以下知识点生成学习资源，直接返回JSON格式（不要用```json包裹）：\n\n" +
            "知识点名称：%s\n知识点路径：%s\n所属学科：%s\n现有知识点内容：%s\n\n" +
            "严格按以下JSON结构输出：\n" +
            "{\n" +
            "  \"videoSearchKeywords\": [\"关键词1\", \"关键词2\"],\n" +
            "  \"examples\": [\n" +
            "    {\"question\": \"题目\", \"answer\": \"答案\", \"explanation\": \"解析\"}\n" +
            "  ],\n" +
            "  \"practices\": [\n" +
            "    {\"question\": \"题目\", \"answer\": \"答案\", \"difficulty\": 1}\n" +
            "  ],\n" +
            "  \"commonMistakes\": \"常见误区（Markdown文本）\",\n" +
            "  \"studyTips\": \"学习建议（Markdown文本）\",\n" +
            "  \"extendedReading\": \"拓展阅读建议\"\n" +
            "}\n\n要求:\n- 内容重点: %s\n" +
            "- videoSearchKeywords: 推荐2-4个B站搜索关键词(如\"职高数学 集合运算 教学\")，≥2个\n" +
            "- examples: ≥2道典型例题，数学用$...$包裹LaTeX公式\n" +
            "- practices: ≥3道练习题，difficulty: 1=基础 2=进阶 3=挑战(数字类型)\n" +
            "- commonMistakes: 3-5个常见误区\n- 纯JSON输出，不要任何包裹标记",
            role, node.getName(), nodePath, subjectName, existingContent, contentFocus
        );
    }

    private void validateLearningResourcesJson(String jsonStr) {
        com.fasterxml.jackson.databind.JsonNode root;
        try {
            root = objectMapper.readTree(jsonStr);
        } catch (Exception e) {
            throw new BusinessException(500, "AI返回格式异常，无法解析JSON: " + e.getMessage());
        }

        com.fasterxml.jackson.databind.JsonNode keywords = root.get("videoSearchKeywords");
        boolean hasVideoUrls = root.has("videoUrls") && root.get("videoUrls").isArray() && root.get("videoUrls").size() >= 2;
        if ((keywords == null || !keywords.isArray() || keywords.size() < 2) && !hasVideoUrls)
            throw new BusinessException(500, "AI返回缺少videoSearchKeywords字段或数量<2");

        com.fasterxml.jackson.databind.JsonNode examples = root.get("examples");
        if (examples == null || !examples.isArray() || examples.size() < 2)
            throw new BusinessException(500, "AI返回缺少examples字段或数量<2");
        for (com.fasterxml.jackson.databind.JsonNode e : examples) {
            if (!e.has("question") || !e.has("answer") || !e.has("explanation"))
                throw new BusinessException(500, "example元素缺少question/answer/explanation字段");
        }

        com.fasterxml.jackson.databind.JsonNode practices = root.get("practices");
        if (practices == null || !practices.isArray() || practices.size() < 3)
            throw new BusinessException(500, "AI返回缺少practices字段或数量<3");
        for (com.fasterxml.jackson.databind.JsonNode p : practices) {
            if (!p.has("question") || !p.has("answer") || !p.has("difficulty"))
                throw new BusinessException(500, "practice元素缺少question/answer/difficulty字段");
            if (!p.get("difficulty").isInt())
                throw new BusinessException(500, "practice.difficulty必须为整数");
            int d = p.get("difficulty").asInt();
            if (d < 1 || d > 3)
                throw new BusinessException(500, "practice.difficulty必须为1-3");
        }

        if (root.has("commonMistakes") && !root.get("commonMistakes").isTextual())
            throw new BusinessException(500, "commonMistakes必须为文本");
        if (root.has("studyTips") && !root.get("studyTips").isTextual())
            throw new BusinessException(500, "studyTips必须为文本");
    }

    private String callAiWithRetry(Map<String, Object> params, int maxRetries, int intervalSec) {
        Exception lastEx = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String result = aiGateway.generateContent(params);
                if (result != null && !result.isBlank()) return result;
                lastEx = new BusinessException(500, "AI 返回空内容");
            } catch (Exception e) {
                lastEx = e;
                log.warn("AI调用失败(第{}次/共{}次): {}", i + 1, maxRetries, e.getMessage());
            }
            if (i < maxRetries - 1) {
                try { Thread.sleep(intervalSec * 1000L); } catch (InterruptedException ignored) {}
            }
        }
        throw new BusinessException(500, "AI 生成失败(已重试" + maxRetries + "次): " +
            (lastEx != null ? lastEx.getMessage() : "未知错误"));
    }

    private boolean checkSingleVideo(String platform, String url, HttpClient client) {
        String checkUrl = url;
        if ("bilibili".equals(platform)) {
            String bv = extractBvId(url);
            if (bv != null) checkUrl = "https://api.bilibili.com/x/web-interface/view?bvid=" + bv;
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(checkUrl))
                    .method("bilibili".equals(platform) ? "GET" : "HEAD",
                        HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "TeachingSystem/1.0")
                    .build();
                HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
                int status = resp.statusCode();
                if ("bilibili".equals(platform)) {
                    if (status == 200) {
                        com.fasterxml.jackson.databind.JsonNode bResp = objectMapper.readTree(resp.body());
                        if (bResp.has("code") && bResp.get("code").asInt() == 0) return true;
                    }
                    if (attempt < 2) { try { Thread.sleep(2000); } catch (InterruptedException ignored) {} }
                    continue;
                }
                return status >= 200 && status < 400;
            } catch (Exception e) {
                if (attempt < 2) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }
        }
        return false;
    }
}
