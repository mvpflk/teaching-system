package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.TypingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TypingServiceImpl implements TypingService {

    @Autowired private TypingTextMapper textMapper;
    @Autowired private TypingCompetitionMapper competitionMapper;
    @Autowired private TypingCompetitionResultMapper resultMapper;
    @Autowired private TypingRecordMapper recordMapper;
    @Autowired private TypingLevelMapper levelMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private com.school.teaching.service.SystemService systemService;
    @Autowired private com.school.teaching.sse.SseConnectionManager sseConnectionManager;
    @Autowired private com.school.teaching.mapper.DictMajorMapper dictMajorMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // #17 修复：经验值公式常量集中定义，避免散落在各方法中
    private static final int PRACTICE_EXP_DIVISOR = 20;  // 练习模式：correctChars / 20
    private static final int PRACTICE_EXP_DEFAULT = 5;   // 无正确字符时的默认经验值
    private static final int COMPETE_EXP_DIVISOR = 10;   // 竞赛模式：correctChars / 10
    private static final int COMPETE_EXP_DEFAULT = 10;   // 竞赛无正确字符时的默认经验值

    private String getStudentName(Long studentId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) return "未知";
        User u = userMapper.selectById(s.getUserId());
        return u != null && u.getRealName() != null ? u.getRealName() : (u != null ? u.getUsername() : "未知");
    }

    private String getStudentMajor(Long studentId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null || s.getClassId() == null) return null;
        Classes c = classesMapper.selectById(s.getClassId());
        return c != null ? c.getMajor() : null;
    }

    // ── 进度缓存: competitionId → (studentId → progress) ──
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Map<String, Object>>> progressCache = new ConcurrentHashMap<>();

    // ── 提交结果缓存: competitionId → List<TypingCompetitionResult>（避免每次 getRanking/getDashboard 查DB）──
    private final ConcurrentHashMap<Long, List<TypingCompetitionResult>> resultsCache = new ConcurrentHashMap<>();

    // ── 学生姓名/班级缓存: competitionId → Map<studentId, name/class>（批量加载后缓存，竞赛期间不变）──
    private final ConcurrentHashMap<Long, Map<Long, String>> studentNameCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Map<Long, String>> studentClassCache = new ConcurrentHashMap<>();

    // ── #11 修复：文本总字数缓存: competitionId → totalChars（竞赛开始时缓存，避免广播时反复查DB）──
    private final ConcurrentHashMap<Long, Integer> totalCharsCache = new ConcurrentHashMap<>();

    /** 定时清理非进行中竞赛的进度缓存（每30分钟） */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1_800_000)
    public void cleanStaleProgressCache() {
        List<Long> ongoing = competitionMapper.selectList(
            new LambdaQueryWrapper<TypingCompetition>().eq(TypingCompetition::getStatus, "ongoing"))
            .stream().map(TypingCompetition::getId).toList();
        progressCache.keySet().removeIf(id -> !ongoing.contains(id));
        resultsCache.keySet().removeIf(id -> !ongoing.contains(id));
        studentNameCache.keySet().removeIf(id -> !ongoing.contains(id));
        studentClassCache.keySet().removeIf(id -> !ongoing.contains(id));
        totalCharsCache.keySet().removeIf(id -> !ongoing.contains(id));
    }

    /** 定时自动结束已到期的竞赛（每30秒检查一次） */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30_000)
    public void autoFinishExpiredCompetitions() {
        List<TypingCompetition> expired = competitionMapper.selectList(
            new LambdaQueryWrapper<TypingCompetition>()
                .eq(TypingCompetition::getStatus, "ongoing")
                .isNotNull(TypingCompetition::getEndTime)
                .lt(TypingCompetition::getEndTime, LocalDateTime.now()));
        for (TypingCompetition comp : expired) {
            try {
                comp.setStatus("finished");
                competitionMapper.updateById(comp);
                // #8 修复：清理所有相关缓存
                progressCache.remove(comp.getId());
                resultsCache.remove(comp.getId());
                studentNameCache.remove(comp.getId());
                studentClassCache.remove(comp.getId());
                totalCharsCache.remove(comp.getId());
                log.info("竞赛自动结束: id={}, title={}", comp.getId(), comp.getTitle());
            } catch (Exception e) {
                log.error("自动结束竞赛失败: id={}", comp.getId(), e);
            }
        }
    }

    /** 定时广播排名和仪表盘（每 1.5 秒），从内存缓存读取，不查 DB */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1_500)
    public void broadcastProgress() {
        for (Map.Entry<Long, ConcurrentHashMap<Long, Map<String, Object>>> entry : progressCache.entrySet()) {
            Long compId = entry.getKey();
            if (entry.getValue().isEmpty()) continue;
            try {
                sseConnectionManager.broadcastCompetition(compId, "ranking", getRankingFromCache(compId));
                sseConnectionManager.broadcastCompetition(compId, "dashboard", getDashboardFromCache(compId));
            } catch (Exception ignored) { /* SSE push failure must not break scheduler */ }
        }
    }

    // ── 权限 ──
    @Override
    public boolean checkStudentPermission(Long studentId) {
        Student s = studentMapper.selectById(studentId);
        String majorName = getStudentMajor(studentId); // classes.major 存的是专业名称
        if (s == null || majorName == null) return false;
        List<Integer> allowed = getTypingAllowedMajors();
        // 通过 dict_major 表查找专业名称对应的ID
        DictMajor dm = dictMajorMapper.selectOne(
            new LambdaQueryWrapper<DictMajor>().eq(DictMajor::getMajorName, majorName));
        if (dm == null) return false;
        return allowed.contains(dm.getId().intValue());
    }

    private List<Integer> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return List.of(1);
        }
    }

    @Override
    public List<Integer> getTypingAllowedMajors() {
        Map<String, String> all = systemService.getAllSettings();
        return parseJsonArray(all.getOrDefault("typing_allowed_majors", "[1]"));
    }

    @Override
    public void setTypingAllowedMajors(List<Integer> majorIds) {
        try {
            String json = objectMapper.writeValueAsString(majorIds);
            systemService.updateAllSettings(Map.of("typing_allowed_majors", json));
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "保存失败");
        }
    }

    // ── 文本管理 ──
    @Override
    public Map<String, Object> getTexts(int page, int size, String type, String keyword,
                                        String language, Integer difficulty, String category) {
        LambdaQueryWrapper<TypingText> qw = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) qw.eq(TypingText::getType, type);
        if (keyword != null && !keyword.isEmpty()) qw.like(TypingText::getTitle, keyword);
        if (language != null && !language.isEmpty()) qw.eq(TypingText::getLanguage, language);
        if (difficulty != null) qw.eq(TypingText::getDifficulty, difficulty);
        if (category != null && !category.isEmpty()) qw.eq(TypingText::getCategory, category);
        qw.orderByDesc(TypingText::getCreatedAt);
        Page<TypingText> p = textMapper.selectPage(new Page<>(page, size), qw);
        return Map.of("records", p.getRecords(), "total", p.getTotal(), "pages", p.getPages());
    }

    @Override
    public TypingText addText(TypingText text) {
        text.setCreatedAt(LocalDateTime.now());
        text.setUpdatedAt(LocalDateTime.now());
        textMapper.insert(text);
        return text;
    }

    @Override
    public TypingText updateText(Long id, TypingText text) {
        TypingText exist = textMapper.selectById(id);
        if (exist == null) throw new BusinessException(404, "文本不存在");
        text.setId(id);
        text.setUpdatedAt(LocalDateTime.now());
        textMapper.updateById(text);
        return textMapper.selectById(id);
    }

    @Override
    public void deleteText(Long id) {
        Long count = competitionMapper.selectCount(
            new LambdaQueryWrapper<TypingCompetition>().eq(TypingCompetition::getTextId, id));
        if (count > 0) throw new BusinessException(400, "该文本已被竞赛引用，无法删除");
        // #5 修复：检查是否有练习记录引用此文本
        Long recordCount = recordMapper.selectCount(
            new LambdaQueryWrapper<TypingRecord>().eq(TypingRecord::getTextId, id));
        if (recordCount > 0) throw new BusinessException(400, "该文本已有练习记录，无法删除");
        textMapper.deleteById(id);
    }

    // ── 竞赛管理 ──
    @Override
    public Map<String, Object> getCompetitions(int page, int size, String status) {
        LambdaQueryWrapper<TypingCompetition> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq(TypingCompetition::getStatus, status);
        qw.orderByDesc(TypingCompetition::getCreatedAt);
        Page<TypingCompetition> p = competitionMapper.selectPage(new Page<>(page, size), qw);
        // 批量加载文本避免 N+1
        if (!p.getRecords().isEmpty()) {
            Set<Long> textIds = p.getRecords().stream().map(TypingCompetition::getTextId).collect(Collectors.toSet());
            Map<Long, TypingText> textMap = textMapper.selectBatchIds(textIds).stream()
                .collect(Collectors.toMap(TypingText::getId, t -> t));
            for (TypingCompetition c : p.getRecords()) {
                TypingText t = textMap.get(c.getTextId());
                if (t != null) {
                    c.setTextTitle(t.getTitle());
                    c.setTextContent(t.getContent());
                }
            }
        }
        return Map.of("records", p.getRecords(), "total", p.getTotal(), "pages", p.getPages());
    }

    @Override
    @Transactional
    public TypingCompetition createCompetition(TypingCompetition comp) {
        TypingText text = textMapper.selectById(comp.getTextId());
        if (text == null) throw new BusinessException(404, "文本不存在");
        comp.setStatus("pending");
        comp.setCreatedAt(LocalDateTime.now());
        competitionMapper.insert(comp);
        return comp;
    }

    @Override
    @Transactional
    public void startCompetition(Long id) {
        TypingCompetition comp = competitionMapper.selectById(id);
        if (comp == null) throw new BusinessException(404, "竞赛不存在");
        if (!"pending".equals(comp.getStatus())) throw new BusinessException(400, "竞赛状态不允许开始");
        comp.setStatus("ongoing");
        comp.setStartTime(LocalDateTime.now());
        // 预设时长：startTime + durationMinutes → endTime
        if (comp.getDurationMinutes() != null && comp.getDurationMinutes() > 0) {
            comp.setEndTime(comp.getStartTime().plusMinutes(comp.getDurationMinutes()));
        }
        competitionMapper.updateById(comp);
        progressCache.put(id, new ConcurrentHashMap<>());

        // #11 修复：缓存文本总字数，避免广播时反复查DB
        TypingText textForChars = textMapper.selectById(comp.getTextId());
        if (textForChars != null && textForChars.getContent() != null) {
            totalCharsCache.put(id, textForChars.getContent().length());
        }

        // 通知所有在线学生竞赛已开始（复用上方已加载的 textForChars，避免重复查DB）
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("competitionId", id);
        announcement.put("title", comp.getTitle());
        announcement.put("textPreview", textForChars != null && textForChars.getContent() != null
            ? textForChars.getContent().substring(0, Math.min(50, textForChars.getContent().length())) : "");
        announcement.put("durationMinutes", comp.getDurationMinutes());
        announcement.put("endTime", comp.getEndTime() != null ? comp.getEndTime().toString() : null);
        sseConnectionManager.broadcastTypingAnnouncement("competition_started", announcement);
    }

    @Override
    @Transactional
    public void finishCompetition(Long id) {
        TypingCompetition comp = competitionMapper.selectById(id);
        if (comp == null) throw new BusinessException(404, "竞赛不存在");
        if (!"ongoing".equals(comp.getStatus())) throw new BusinessException(400, "竞赛不是进行中状态");
        comp.setStatus("finished");
        comp.setEndTime(LocalDateTime.now());
        competitionMapper.updateById(comp);
        // #8 修复：清理所有相关缓存
        progressCache.remove(id);
        resultsCache.remove(id);
        studentNameCache.remove(id);
        studentClassCache.remove(id);
        totalCharsCache.remove(id);
    }

    @Override
    @Transactional
    public void deleteCompetition(Long id) {
        TypingCompetition comp = competitionMapper.selectById(id);
        if (comp == null) throw new BusinessException(404, "竞赛不存在");
        if ("ongoing".equals(comp.getStatus())) throw new BusinessException(400, "进行中的竞赛不能删除，请先结束");
        // 删除关联成绩和进度缓存
        resultMapper.delete(new LambdaQueryWrapper<TypingCompetitionResult>()
            .eq(TypingCompetitionResult::getCompetitionId, id));
        progressCache.remove(id);
        resultsCache.remove(id);
        studentNameCache.remove(id);
        studentClassCache.remove(id);
        totalCharsCache.remove(id);
        competitionMapper.deleteById(id);
    }

    @Override
    public TypingCompetition getCurrentCompetition(Long studentId) {
        LambdaQueryWrapper<TypingCompetition> qw = new LambdaQueryWrapper<>();
        qw.eq(TypingCompetition::getStatus, "ongoing");
        qw.orderByDesc(TypingCompetition::getCreatedAt);
        List<TypingCompetition> list = competitionMapper.selectList(qw);
        if (list.isEmpty()) return null;

        // #12 修复：批量加载学生信息，避免循环内 N+1 查询
        Student student = studentId != null ? studentMapper.selectById(studentId) : null;

        // 批量加载文本避免循环内逐条查询
        Set<Long> textIds = list.stream().map(TypingCompetition::getTextId).collect(Collectors.toSet());
        Map<Long, TypingText> textMap = textIds.isEmpty() ? Map.of()
            : textMapper.selectBatchIds(textIds).stream()
                .collect(Collectors.toMap(TypingText::getId, t -> t));

        for (TypingCompetition comp : list) {
            // 班级权限检查
            if (student != null && student.getClassId() != null) {
                try {
                    List<Integer> allowedClassIds = parseJsonArray(comp.getAllowedClassIds());
                    if (!allowedClassIds.isEmpty() && !allowedClassIds.contains(student.getClassId().intValue())) {
                        continue;
                    }
                } catch (Exception ignored) { continue; }
            }
            // 从批量缓存中加载文本
            TypingText text = textMap.get(comp.getTextId());
            if (text != null) {
                comp.setTextTitle(text.getTitle());
                comp.setTextContent(text.getContent());
            }
            return comp;
        }
        return null;
    }

    // ── 排名/驾驶舱 ──
    @Override
    public List<Map<String, Object>> getRanking(Long competitionId) {
        return getRankingFromCache(competitionId);
    }

    /** 从内存缓存构建排名（不查 DB），用于实时广播 */
    private List<Map<String, Object>> getRankingFromCache(Long competitionId) {
        ConcurrentHashMap<Long, Map<String, Object>> cache = progressCache.get(competitionId);
        List<Map<String, Object>> ranking = new ArrayList<>();
        Set<Long> allStudentIds = new HashSet<>();

        if (cache != null) allStudentIds.addAll(cache.keySet());
        List<TypingCompetitionResult> results = getCachedResults(competitionId);
        for (TypingCompetitionResult r : results) allStudentIds.add(r.getStudentId());

        Map<Long, String> nameMap = getCachedStudentNames(competitionId, allStudentIds);
        Map<Long, String> classMap = getCachedStudentClasses(competitionId, allStudentIds);

        if (cache != null) {
            for (Map.Entry<Long, Map<String, Object>> e : cache.entrySet()) {
                Map<String, Object> p = new HashMap<>(e.getValue());
                p.put("studentId", e.getKey());
                p.put("nickname", maskName(nameMap.getOrDefault(e.getKey(), "未知")));
                p.put("className", classMap.getOrDefault(e.getKey(), ""));
                p.put("source", "live");
                if (!p.containsKey("progressPercent") || p.get("progressPercent") == null) {
                    int total = toInt(p.get("totalCount"));
                    int correct = toInt(p.get("correctCount"));
                    p.put("progressPercent", total > 0 ? Math.round(correct * 100f / total) : 0);
                }
                ranking.add(p);
            }
        }

        Set<Long> cachedIds = cache != null ? cache.keySet() : Set.of();
        for (TypingCompetitionResult r : results) {
            if (cachedIds.contains(r.getStudentId())) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("studentId", r.getStudentId());
            m.put("nickname", maskName(nameMap.getOrDefault(r.getStudentId(), "未知")));
            m.put("speedWpm", r.getSpeedWpm());
            m.put("accuracy", r.getAccuracy());
            m.put("progressPercent", 100);
            m.put("source", "db");
            ranking.add(m);
        }

        ranking.sort((a, b) -> {
            BigDecimal sa = toBigDecimal(a.get("speedWpm"));
            BigDecimal sb = toBigDecimal(b.get("speedWpm"));
            int cmp = sb.compareTo(sa);
            if (cmp != 0) return cmp;
            BigDecimal aa = toBigDecimal(a.get("accuracy"));
            BigDecimal ab = toBigDecimal(b.get("accuracy"));
            return ab.compareTo(aa);
        });

        return ranking.size() > 200 ? ranking.subList(0, 200) : ranking;
    }

    @Override
    public Map<String, Object> getDashboard(Long competitionId) {
        return getDashboardFromCache(competitionId);
    }

    /** 从内存缓存构建仪表盘（不查 DB），用于实时广播 */
    private Map<String, Object> getDashboardFromCache(Long competitionId) {
        // #11 修复：优先从缓存获取 totalChars，避免每 1.5 秒查 DB
        Integer totalChars = totalCharsCache.get(competitionId);
        TypingCompetition comp = null;
        if (totalChars == null) {
            comp = competitionMapper.selectById(competitionId);
            if (comp == null) throw new BusinessException(404, "竞赛不存在");
            TypingText text = textMapper.selectById(comp.getTextId());
            totalChars = text != null && text.getContent() != null ? text.getContent().length() : 0;
            totalCharsCache.put(competitionId, totalChars);
        }

        ConcurrentHashMap<Long, Map<String, Object>> cache = progressCache.get(competitionId);
        List<Map<String, Object>> students = new ArrayList<>();
        Set<Long> allStudentIds = cache != null ? new HashSet<>(cache.keySet()) : new HashSet<>();

        // 批量加载学生姓名（从缓存或 DB）
        Map<Long, String> nameMap = getCachedStudentNames(competitionId, allStudentIds);

        if (cache != null) {
            for (Map.Entry<Long, Map<String, Object>> e : cache.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                row.put("studentId", e.getKey());
                row.put("name", nameMap.getOrDefault(e.getKey(), "未知"));
                row.put("totalChars", totalChars);
                row.put("typedChars", e.getValue().getOrDefault("correctCount", 0));
                row.put("speedWpm", e.getValue().getOrDefault("speedWpm", 0));
                row.put("accuracy", e.getValue().getOrDefault("accuracy", 0));
                row.put("finished", false);
                row.put("source", "live");
                students.add(row);
            }
        }

        List<TypingCompetitionResult> results = getCachedResults(competitionId);
        Set<Long> resultStudentIds = results.stream().map(TypingCompetitionResult::getStudentId).collect(Collectors.toSet());
        resultStudentIds.removeAll(allStudentIds);
        if (!resultStudentIds.isEmpty()) {
            allStudentIds.addAll(resultStudentIds);
            nameMap = getCachedStudentNames(competitionId, allStudentIds);
        }

        Map<Long, Map<String, Object>> studentRowMap = new HashMap<>();
        for (Map<String, Object> row : students) {
            studentRowMap.put((Long) row.get("studentId"), row);
        }

        for (TypingCompetitionResult r : results) {
            Map<String, Object> existingRow = studentRowMap.get(r.getStudentId());
            if (existingRow != null) {
                existingRow.put("typedChars", r.getCorrectChars());
                existingRow.put("speedWpm", r.getSpeedWpm());
                existingRow.put("accuracy", r.getAccuracy());
                existingRow.put("finished", true);
                existingRow.put("source", "db");
            } else {
                Map<String, Object> row = new HashMap<>();
                row.put("studentId", r.getStudentId());
                row.put("name", nameMap.getOrDefault(r.getStudentId(), "未知"));
                row.put("totalChars", totalChars);
                row.put("typedChars", r.getCorrectChars());
                row.put("speedWpm", r.getSpeedWpm());
                row.put("accuracy", r.getAccuracy());
                row.put("finished", true);
                row.put("source", "db");
                students.add(row);
            }
        }

        return Map.of("competition", comp, "totalChars", totalChars, "students", students);
    }

    // ── 学生打字过程 ──
    @Override
    public void reportProgress(Long competitionId, Long studentId, Map<String, Object> progress) {
        ConcurrentHashMap<Long, Map<String, Object>> cache = progressCache.computeIfAbsent(competitionId, k -> new ConcurrentHashMap<>());
        progress.put("updatedAt", System.currentTimeMillis());
        cache.put(studentId, progress);
        // 不再同步广播 — 由 @Scheduled broadcastProgress() 定时推送，避免阻塞请求线程
    }

    @Override
    public TypingText getRandomPracticeText(Long textId, Integer difficulty, String language) {
        if (textId != null) {
            TypingText t = textMapper.selectById(textId);
            if (t != null) return t;
        }
        LambdaQueryWrapper<TypingText> wrapper = new LambdaQueryWrapper<TypingText>()
            .eq(TypingText::getType, "practice");
        if (difficulty != null) wrapper.eq(TypingText::getDifficulty, difficulty);
        if (language != null && !language.isEmpty()) wrapper.eq(TypingText::getLanguage, language);
        List<TypingText> list = textMapper.selectList(wrapper);
        if (list.isEmpty()) {
            list = textMapper.selectList(new LambdaQueryWrapper<TypingText>().eq(TypingText::getType, "practice"));
        }
        if (list.isEmpty()) throw new BusinessException(404, "没有可用的打字文本");
        return list.get(new Random().nextInt(list.size()));
    }

    @Override
    public List<String> getPracticeCategories() {
        return textMapper.selectList(
            new LambdaQueryWrapper<TypingText>()
                .eq(TypingText::getType, "practice")
                .isNotNull(TypingText::getCategory)
                .ne(TypingText::getCategory, "")
                .select(TypingText::getCategory))
            .stream().map(TypingText::getCategory).distinct().sorted().toList();
    }

    @Override
    public void saveRecord(TypingRecord record) {
        // #6 修复：校验 textId 合法性
        if (record.getTextId() != null) {
            TypingText text = textMapper.selectById(record.getTextId());
            if (text == null) throw new BusinessException(400, "文本不存在");
            if ("practice".equals(record.getMode()) && !"practice".equals(text.getType())) {
                throw new BusinessException(400, "文本类型不匹配");
            }
        }
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }

    /** #15 修复：统一练习保存入口，避免 /records 和 /practice/finish 重复逻辑 */
    @Override
    public int savePracticeRecord(TypingRecord record) {
        // textId 合法性校验
        if (record.getTextId() != null) {
            TypingText text = textMapper.selectById(record.getTextId());
            if (text == null) throw new BusinessException(400, "文本不存在");
            if (!"practice".equals(text.getType())) throw new BusinessException(400, "文本类型不匹配");
        }
        // 负数校验
        if (record.getDurationSeconds() != null && record.getDurationSeconds() <= 0) {
            throw new BusinessException(400, "时长数据异常");
        }
        if (record.getCorrectChars() != null && record.getCorrectChars() < 0) {
            throw new BusinessException(400, "数据异常：正确字符数为负数");
        }
        if (record.getWrongChars() != null && record.getWrongChars() < 0) {
            throw new BusinessException(400, "数据异常：错误字符数为负数");
        }
        if (record.getBackspaceCount() != null && record.getBackspaceCount() < 0) {
            throw new BusinessException(400, "数据异常：退格数为负数");
        }
        // 防篡改校验
        if (record.getCorrectChars() != null && record.getTotalChars() != null
            && record.getCorrectChars() > record.getTotalChars()) {
            throw new BusinessException(400, "数据异常：正确字符数超过总字符数");
        }
        if (record.getSpeedWpm() != null && record.getSpeedWpm().compareTo(new BigDecimal("300")) > 0) {
            throw new BusinessException(400, "数据异常：速度超过合理上限");
        }
        if (record.getAccuracy() != null && record.getAccuracy().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(400, "数据异常：正确率超过100%");
        }
        record.setMode("practice");
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        // #17 修复：统一经验值公式，使用常量
        int exp = record.getCorrectChars() != null ? Math.max(1, record.getCorrectChars() / PRACTICE_EXP_DIVISOR) : PRACTICE_EXP_DEFAULT;
        addExp(record.getStudentId(), exp);
        return exp;
    }

    // ── 提交成绩 ──
    @Override
    @Transactional
    public TypingCompetitionResult submitResult(Long competitionId, Long studentId, Map<String, Object> data) {
        TypingCompetition comp = competitionMapper.selectById(competitionId);
        if (comp == null) throw new BusinessException(404, "竞赛不存在");

        // #2 修复：竞赛结束后不允许提交成绩
        if (!"ongoing".equals(comp.getStatus())) {
            throw new BusinessException(400, "竞赛已结束，无法提交成绩");
        }

        // 检查是否已提交过
        Long existCount = resultMapper.selectCount(
            new LambdaQueryWrapper<TypingCompetitionResult>()
                .eq(TypingCompetitionResult::getCompetitionId, competitionId)
                .eq(TypingCompetitionResult::getStudentId, studentId));
        if (existCount > 0) throw new BusinessException(409, "你已提交过本次竞赛成绩");

        TypingCompetitionResult r = new TypingCompetitionResult();
        r.setCompetitionId(competitionId);
        r.setStudentId(studentId);
        r.setTotalChars(toInt(data.get("totalChars")));
        r.setCorrectChars(toInt(data.get("correctChars")));
        r.setWrongChars(toInt(data.get("wrongChars")));
        r.setBackspaceCount(toInt(data.get("backspaceCount")));
        r.setDurationSeconds(toInt(data.get("durationSeconds")));
        r.setSpeedWpm(toBigDecimal(data.get("speedWpm")));
        r.setAccuracy(toBigDecimal(data.get("accuracy")));

        // #20 修复：负数/异常值校验
        if (r.getDurationSeconds() != null && r.getDurationSeconds() <= 0) {
            throw new BusinessException(400, "时长数据异常");
        }
        if (r.getCorrectChars() != null && r.getCorrectChars() < 0) {
            throw new BusinessException(400, "数据异常：正确字符数为负数");
        }
        if (r.getWrongChars() != null && r.getWrongChars() < 0) {
            throw new BusinessException(400, "数据异常：错误字符数为负数");
        }
        if (r.getBackspaceCount() != null && r.getBackspaceCount() < 0) {
            throw new BusinessException(400, "数据异常：退格数为负数");
        }

        // #1 修复：duration <= 0 直接拒绝，不再跳过校验
        int duration = r.getDurationSeconds() != null ? r.getDurationSeconds() : 0;
        int correct = r.getCorrectChars() != null ? r.getCorrectChars() : 0;
        if (duration <= 0) {
            throw new BusinessException(400, "时长数据异常，请重新提交");
        }
        {
            BigDecimal serverWpm = BigDecimal.valueOf(correct).divide(BigDecimal.valueOf(duration), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60));
            BigDecimal clientWpm = r.getSpeedWpm() != null ? r.getSpeedWpm() : BigDecimal.ZERO;
            if (clientWpm.compareTo(BigDecimal.valueOf(300)) > 0) {
                throw new BusinessException(400, "数据异常，请重新提交");
            }
            if (r.getAccuracy() != null && r.getAccuracy().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException(400, "数据异常，请重新提交");
            }
            if (serverWpm.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal deviation = serverWpm.subtract(clientWpm).abs()
                    .divide(serverWpm, 2, RoundingMode.HALF_UP);
                if (deviation.compareTo(BigDecimal.valueOf(0.15)) > 0) {
                    throw new BusinessException(400, "数据校验失败，请重新提交");
                }
            }
        }

        r.setScore(calcScore(r.getSpeedWpm(), r.getAccuracy()));
        r.setFinishedAt(LocalDateTime.now());
        try {
            if (data.get("errorDetails") != null) {
                r.setErrorDetails(objectMapper.writeValueAsString(data.get("errorDetails")));
            }
        } catch (JsonProcessingException ignored) { log.warn("errorDetails序列化失败", ignored); }
        try {
            if (data.get("keystrokeData") != null) {
                Object kd = data.get("keystrokeData");
                String json = kd instanceof String ? (String) kd : objectMapper.writeValueAsString(kd);
                r.setKeystrokeData(json);
            }
        } catch (JsonProcessingException ignored) { log.warn("keystrokeData序列化失败", ignored); }

        resultMapper.insert(r);
        // 更新缓存：将新提交的结果加入缓存，避免下次广播重新查 DB
        resultsCache.computeIfAbsent(competitionId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(r);
        progressCache.getOrDefault(competitionId, new ConcurrentHashMap<>()).remove(studentId);

        // 自动记录到 typing_records
        TypingText text = textMapper.selectById(comp.getTextId());
        TypingRecord rec = new TypingRecord();
        rec.setStudentId(studentId);
        rec.setTextId(comp.getTextId());
        rec.setMode("competition");
        rec.setTotalChars(r.getTotalChars());
        rec.setCorrectChars(r.getCorrectChars());
        rec.setWrongChars(r.getWrongChars());
        rec.setBackspaceCount(r.getBackspaceCount());
        rec.setDurationSeconds(r.getDurationSeconds());
        rec.setSpeedWpm(r.getSpeedWpm());
        rec.setAccuracy(r.getAccuracy());
        rec.setErrorDetails(r.getErrorDetails());
        rec.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(rec);

        // #17 修复：竞赛经验值公式，使用常量
        int exp = r.getCorrectChars() != null ? Math.max(1, r.getCorrectChars() / COMPETE_EXP_DIVISOR) : COMPETE_EXP_DEFAULT;
        addExpInternal(studentId, exp);

        return r;
    }

    // ── 竞赛回放 ──
    @Override
    public Map<String, Object> getCompetitionReplay(Long competitionId, Long studentId) {
        TypingCompetition comp = competitionMapper.selectById(competitionId);
        if (comp == null) throw new BusinessException(404, "竞赛不存在");
        TypingText text = textMapper.selectById(comp.getTextId());
        TypingCompetitionResult result = resultMapper.selectOne(
            new LambdaQueryWrapper<TypingCompetitionResult>()
                .eq(TypingCompetitionResult::getCompetitionId, competitionId)
                .eq(TypingCompetitionResult::getStudentId, studentId));
        if (result == null) throw new BusinessException(404, "该学生未提交成绩");
        Map<String, Object> r = new HashMap<>();
        r.put("studentName", getStudentName(studentId));
        r.put("textContent", text != null ? text.getContent() : "");
        r.put("speedWpm", result.getSpeedWpm());
        r.put("accuracy", result.getAccuracy());
        r.put("durationSeconds", result.getDurationSeconds());
        try {
            if (result.getKeystrokeData() != null) {
                r.put("keystrokeData", objectMapper.readValue(result.getKeystrokeData(),
                    new TypeReference<List<Map<String, Object>>>() {}));
            } else {
                r.put("keystrokeData", List.of());
            }
        } catch (Exception e) {
            r.put("keystrokeData", List.of());
        }
        return r;
    }

    // ── 导出 ──
    @Override
    public List<Map<String, Object>> exportResults(Long competitionId) {
        List<TypingCompetitionResult> results = resultMapper.selectList(
            new LambdaQueryWrapper<TypingCompetitionResult>().eq(TypingCompetitionResult::getCompetitionId, competitionId));
        Set<Long> studentIds = results.stream().map(TypingCompetitionResult::getStudentId).collect(Collectors.toSet());
        Map<Long, String> nameMap = batchLoadStudentNames(studentIds);
        // 批量加载 username
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, String> usernameMap = userIds.isEmpty() ? Map.of()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        Map<Long, Long> studentToUserId = students.stream()
            .collect(Collectors.toMap(Student::getId, Student::getUserId));
        return results.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("studentName", nameMap.getOrDefault(r.getStudentId(), "未知"));
            Long uid = studentToUserId.get(r.getStudentId());
            m.put("username", uid != null ? usernameMap.getOrDefault(uid, "") : "");
            m.put("speedWpm", r.getSpeedWpm());
            m.put("accuracy", r.getAccuracy());
            m.put("durationSeconds", r.getDurationSeconds());
            m.put("correctChars", r.getCorrectChars());
            m.put("wrongChars", r.getWrongChars());
            m.put("backspaceCount", r.getBackspaceCount());
            m.put("score", r.getScore());
            m.put("errorDetails", r.getErrorDetails());
            m.put("finishedAt", r.getFinishedAt() != null ? r.getFinishedAt().toString() : "");
            return m;
        }).collect(Collectors.toList());
    }

    // ── 学生历史/游戏化 ──
    @Override
    public List<TypingRecord> getStudentHistory(Long studentId) {
        return recordMapper.selectList(
            new LambdaQueryWrapper<TypingRecord>()
                .eq(TypingRecord::getStudentId, studentId)
                .orderByDesc(TypingRecord::getCreatedAt));
    }

    @Override
    public List<Map<String, Object>> getWrongWords(Long studentId) {
        List<TypingRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<TypingRecord>()
                .eq(TypingRecord::getStudentId, studentId)
                .isNotNull(TypingRecord::getErrorDetails));

        Map<String, Integer> freq = new LinkedHashMap<>();
        for (TypingRecord r : records) {
            if (r.getErrorDetails() == null) continue;
            try {
                List<Map<String, Object>> errors = objectMapper.readValue(r.getErrorDetails(),
                    new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> err : errors) {
                    String ch = String.valueOf(err.getOrDefault("expected", "?"));
                    freq.merge(ch, 1, Integer::sum);
                }
            } catch (Exception ignored) { log.warn("错误详情解析失败", ignored); }
        }
        return freq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(50)
            .map(e -> Map.<String, Object>of("char", e.getKey(), "count", e.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getStudentLevels(Long studentId) {
        TypingLevel level = levelMapper.selectOne(
            new LambdaQueryWrapper<TypingLevel>().eq(TypingLevel::getStudentId, studentId));
        if (level == null) {
            level = new TypingLevel();
            level.setStudentId(studentId);
            level.setLevelId(1);
            level.setExp(0);
            level.setUnlockedMaps("[1]");
            level.setUpdatedAt(LocalDateTime.now());
            levelMapper.insert(level);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("levelId", level.getLevelId());
        result.put("exp", level.getExp());
        result.put("expToNext", level.getLevelId() * 100);
        try {
            result.put("unlockedMaps", objectMapper.readValue(
                level.getUnlockedMaps() != null ? level.getUnlockedMaps() : "[1]",
                new TypeReference<List<Integer>>() {}));
        } catch (Exception e) {
            result.put("unlockedMaps", List.of(1));
        }
        return result;
    }

    @Override
    @Transactional
    public void addExp(Long studentId, int exp) {
        addExpInternal(studentId, exp);
    }

    private void addExpInternal(Long studentId, int exp) {
        // #3 修复：使用 SELECT FOR UPDATE 行级锁，防止并发升级竞态
        TypingLevel level = levelMapper.selectForUpdate(studentId);
        if (level == null) {
            level = new TypingLevel();
            level.setStudentId(studentId);
            level.setLevelId(1);
            level.setExp(0);
            level.setUnlockedMaps("[1]");
            level.setUpdatedAt(LocalDateTime.now());
            try {
                levelMapper.insert(level);
                // 插入后重新加锁读取
                level = levelMapper.selectForUpdate(studentId);
            } catch (org.springframework.dao.DuplicateKeyException ignored) {
                level = levelMapper.selectForUpdate(studentId);
            }
        }
        // 在行锁保护下增加 exp 并检查升级
        levelMapper.addExpAtomic(studentId, exp);
        // 重新读取已更新的 exp（仍在事务/锁内）
        level = levelMapper.selectForUpdate(studentId);
        int newExp = level.getExp();
        int lv = level.getLevelId();
        int maxIter = 100;
        while (newExp >= lv * 100 && maxIter-- > 0) {
            newExp -= lv * 100;
            lv++;
        }
        if (lv != level.getLevelId() || newExp != level.getExp()) {
            level.setExp(newExp);
            level.setLevelId(lv);
            level.setUpdatedAt(LocalDateTime.now());
            levelMapper.updateById(level);
        }
    }

    // ── 工具方法 ──
    /** 获取缓存的提交结果，缓存未命中时从 DB 加载 */
    private List<TypingCompetitionResult> getCachedResults(Long competitionId) {
        return resultsCache.computeIfAbsent(competitionId, id ->
            resultMapper.selectList(new LambdaQueryWrapper<TypingCompetitionResult>()
                .eq(TypingCompetitionResult::getCompetitionId, id)));
    }

    /** 获取缓存的学生姓名映射，自动补全缺失的 studentId */
    private Map<Long, String> getCachedStudentNames(Long competitionId, Set<Long> studentIds) {
        Map<Long, String> cache = studentNameCache.computeIfAbsent(competitionId, k -> new ConcurrentHashMap<>());
        Set<Long> missing = studentIds.stream().filter(id -> !cache.containsKey(id)).collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            cache.putAll(batchLoadStudentNames(missing));
        }
        return cache;
    }

    /** 获取缓存的学生班级映射 */
    private Map<Long, String> getCachedStudentClasses(Long competitionId, Set<Long> studentIds) {
        Map<Long, String> cache = studentClassCache.computeIfAbsent(competitionId, k -> new ConcurrentHashMap<>());
        Set<Long> missing = studentIds.stream().filter(id -> !cache.containsKey(id)).collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            cache.putAll(batchLoadStudentClasses(missing));
        }
        return cache;
    }
    private Map<Long, String> batchLoadStudentNames(Set<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return Map.of();
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return students.stream()
            .collect(Collectors.toMap(Student::getId, s -> {
                User u = userMap.get(s.getUserId());
                return u != null && u.getRealName() != null ? u.getRealName()
                    : u != null ? u.getUsername() : "未知";
            }));
    }

    /** 批量加载学生班级名 */
    private Map<Long, String> batchLoadStudentClasses(Set<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return Map.of();
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Set<Long> classIds = students.stream().map(Student::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Classes> classMap = classIds.isEmpty() ? Map.of()
            : classesMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(Classes::getId, c -> c));
        return students.stream()
            .collect(Collectors.toMap(Student::getId, s -> {
                Classes c = classMap.get(s.getClassId());
                return c != null ? c.getClassName() : "";
            }));
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }

    private int toInt(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        if (v instanceof String) {
            try { return new BigDecimal((String) v); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
        }
        return BigDecimal.ZERO;
    }

    @Override
    public List<Map<String, Object>> getStudentSpeedTrend(Long studentId, int limit) {
        List<TypingRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<TypingRecord>()
                .eq(TypingRecord::getStudentId, studentId)
                .orderByDesc(TypingRecord::getCreatedAt)
                .last("LIMIT " + Math.min(Math.max(limit, 5), 100)));
        // 按日期升序返回
        Collections.reverse(records);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TypingRecord r : records) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : null);
            m.put("speedWpm", r.getSpeedWpm() != null ? r.getSpeedWpm().intValue() : 0);
            m.put("accuracy", r.getAccuracy() != null ? r.getAccuracy().setScale(1, RoundingMode.HALF_UP).doubleValue() : 0);
            m.put("totalChars", r.getTotalChars() != null ? r.getTotalChars() : 0);
            m.put("durationSeconds", r.getDurationSeconds() != null ? r.getDurationSeconds() : 0);
            result.add(m);
        }
        return result;
    }

    private BigDecimal calcScore(BigDecimal speed, BigDecimal accuracy) {
        if (speed == null) speed = BigDecimal.ZERO;
        if (accuracy == null) accuracy = BigDecimal.ZERO;
        return speed.multiply(BigDecimal.valueOf(0.6))
            .add(accuracy.multiply(BigDecimal.valueOf(0.4)))
            .setScale(2, RoundingMode.HALF_UP);
    }
}