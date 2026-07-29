package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.SystemService;
import com.school.teaching.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SystemServiceImpl implements SystemService {

    @Autowired private SystemSettingMapper settingMapper;
    @Autowired private DictGradeMapper dictGradeMapper;
    @Autowired private DictSubjectMapper dictSubjectMapper;
    @Autowired private DictMajorMapper dictMajorMapper;
    @Autowired private DictMajorSubjectMapper dictMajorSubjectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogMapper auditLogMapper;

    // Mappers for system status / reset / clear (bulk operations)
    @Autowired(required = false) private BbsPostMapper bbsPostMapper;
    @Autowired(required = false) private BbsReplyMapper bbsReplyMapper;
    @Autowired(required = false) private CreditTransactionMapper creditTransactionMapper;
    @Autowired(required = false) private SignRecordMapper signRecordMapper;
    @Autowired(required = false) private NotificationMapper notificationMapper;
    @Autowired(required = false) private ClassesMapper classesMapper;
    @Autowired(required = false) private StudentMapper studentMapper;
    @Autowired(required = false) private TeacherMapper teacherMapper;
    @Autowired(required = false) private com.school.teaching.mapper.TaskMapper taskMapper;
    @Autowired(required = false) private BbsCategoryMapper bbsCategoryMapper;
    @Autowired(required = false) private BbsLikeMapper bbsLikeMapper;
    @Autowired(required = false) private BbsBookmarkMapper bbsBookmarkMapper;
    @Autowired(required = false) private BbsMutedUserMapper bbsMutedUserMapper;
    @Autowired private SchoolTermMapper schoolTermMapper;
    @Autowired private com.school.teaching.service.TeacherService teacherService;
    @Autowired private com.school.teaching.mapper.TeacherClassMapper teacherClassMapper;

    // ==================== Settings CRUD ====================

    @Override
    public Map<String, String> getAllSettings() {
        return settingMapper.selectList(null).stream()
            .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    @Override
    public void updateAllSettings(Map<String, String> settings) {
        Map<String, SystemSetting> existingMap = settingMapper.selectList(
            new LambdaQueryWrapper<SystemSetting>().in(SystemSetting::getSettingKey, settings.keySet()))
            .stream().collect(Collectors.toMap(SystemSetting::getSettingKey, s -> s));
        for (String key : settings.keySet()) {
            if (!existingMap.containsKey(key)) throw new BusinessException(400, "配置项不存在: " + key);
        }
        for (var entry : settings.entrySet()) {
            SystemSetting s = existingMap.get(entry.getKey());
            if (s != null) { s.setSettingValue(entry.getValue()); settingMapper.updateById(s); }
        }
    }

    @Override
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        SystemSetting setting = settingMapper.selectOne(
            new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, key));
        if (setting == null || setting.getSettingValue() == null) return defaultValue;
        return "true".equalsIgnoreCase(setting.getSettingValue().trim());
    }

    @Override
    public int getIntConfig(String key, int defaultValue) {
        SystemSetting setting = settingMapper.selectOne(
            new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, key));
        if (setting == null || setting.getSettingValue() == null) return defaultValue;
        try { return Integer.parseInt(setting.getSettingValue().trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    @Override
    public Map<String, Boolean> getFeatureFlags() {
        List<SystemSetting> list = settingMapper.selectList(
            new LambdaQueryWrapper<SystemSetting>().likeRight(SystemSetting::getSettingKey, "feature."));
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (SystemSetting s : list) {
            result.put(s.getSettingKey(), "true".equalsIgnoreCase(
                s.getSettingValue() != null ? s.getSettingValue().trim() : "false"));
        }
        return result;
    }

    // ==================== Dictionary ====================

    @Override
    public List<Map<String, Object>> getGrades(Long stageId) {
        LambdaQueryWrapper<DictGrade> w = new LambdaQueryWrapper<DictGrade>()
            .eq(DictGrade::getStatus, 1).orderByAsc(DictGrade::getSortOrder);
        List<DictGrade> grades = dictGradeMapper.selectList(w).stream()
            .filter(g -> stageId == null || stageId.equals(g.getStageId())).toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (DictGrade g : grades) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", g.getId()); m.put("gradeName", g.getGradeName()); m.put("sortOrder", g.getSortOrder()); m.put("status", g.getStatus());
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getSubjects() {
        List<DictSubject> subs = dictSubjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getStatus, 1).orderByAsc(DictSubject::getSortOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DictSubject s : subs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId()); m.put("subjectName", s.getSubjectName()); m.put("sortOrder", s.getSortOrder()); m.put("status", s.getStatus());
            result.add(m);
        }
        return result;
    }

    @Override public List<Map<String, Object>> getDictGrades() { return getGrades(null); }
    @Override public List<Map<String, Object>> getDictSubjects() { return getSubjects(); }

    @Override public Map<String, Object> addDictGrade(String name) {
        DictGrade g = new DictGrade(); g.setGradeName(name); dictGradeMapper.insert(g);
        return Map.of("id", g.getId(), "gradeName", name);
    }
    @Override public int batchAddDictGrades(List<Map<String, Object>> list) {
        int count = 0;
        for (Map<String, Object> item : list) {
            String name = String.valueOf(item.getOrDefault("name", item.getOrDefault("gradeName", "")));
            if (name.isBlank()) continue;
            DictGrade g = new DictGrade(); g.setGradeName(name);
            if (item.get("sortOrder") instanceof Number) g.setSortOrder(((Number) item.get("sortOrder")).intValue());
            dictGradeMapper.insert(g); count++;
        }
        return count;
    }
    @Override @CacheEvict(value = "gradeList", allEntries = true)
    public void updateDictGrade(Long id, String name) {
        DictGrade g = dictGradeMapper.selectById(id);
        if (g != null) { g.setGradeName(name); dictGradeMapper.updateById(g); }
    }
    @Override @CacheEvict(value = "gradeList", allEntries = true)
    public void deleteDictGrade(Long id) { dictGradeMapper.deleteById(id); }

    @Override public Map<String, Object> addDictSubject(String name) {
        DictSubject s = new DictSubject(); s.setSubjectName(name); dictSubjectMapper.insert(s);
        return Map.of("id", s.getId(), "subjectName", name);
    }
    @Override public int batchAddDictSubjects(List<Map<String, Object>> list) {
        int count = 0;
        for (Map<String, Object> item : list) {
            String name = String.valueOf(item.getOrDefault("name", item.getOrDefault("subjectName", "")));
            if (name.isBlank()) continue;
            DictSubject s = new DictSubject(); s.setSubjectName(name);
            if (item.get("sortOrder") instanceof Number) s.setSortOrder(((Number) item.get("sortOrder")).intValue());
            dictSubjectMapper.insert(s); count++;
        }
        return count;
    }
    @Override @CacheEvict(value = "subjectTree", allEntries = true)
    public void updateDictSubject(Long id, String name) {
        DictSubject s = dictSubjectMapper.selectById(id);
        if (s != null) { s.setSubjectName(name); dictSubjectMapper.updateById(s); }
    }
    @Override @CacheEvict(value = "subjectTree", allEntries = true) @Transactional
    public void deleteDictSubject(Long id) {
        // 先删除该学科与所有专业的关联
        dictMajorSubjectMapper.delete(new LambdaQueryWrapper<DictMajorSubject>().eq(DictMajorSubject::getSubjectId, id));
        dictSubjectMapper.deleteById(id);
    }

    // ── 专业字典 ──
    @Override
    public List<Map<String, Object>> getDictMajors() {
        List<DictMajor> majors = dictMajorMapper.selectList(
            new LambdaQueryWrapper<DictMajor>().eq(DictMajor::getStatus, 1).orderByAsc(DictMajor::getSortOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DictMajor m : majors) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId()); map.put("majorName", m.getMajorName());
            map.put("sortOrder", m.getSortOrder()); map.put("status", m.getStatus());
            result.add(map);
        }
        return result;
    }
    @Override public Map<String, Object> addDictMajor(String name) {
        DictMajor m = new DictMajor(); m.setMajorName(name); dictMajorMapper.insert(m);
        return Map.of("id", m.getId(), "majorName", name);
    }
    @Override public int batchAddDictMajors(List<Map<String, Object>> list) {
        int count = 0;
        for (Map<String, Object> item : list) {
            String name = String.valueOf(item.getOrDefault("name", item.getOrDefault("majorName", "")));
            if (name.isBlank()) continue;
            DictMajor m = new DictMajor(); m.setMajorName(name);
            if (item.get("sortOrder") instanceof Number) m.setSortOrder(((Number) item.get("sortOrder")).intValue());
            dictMajorMapper.insert(m); count++;
        }
        return count;
    }
    @Override public void updateDictMajor(Long id, String name) {
        DictMajor m = dictMajorMapper.selectById(id);
        if (m != null) { m.setMajorName(name); dictMajorMapper.updateById(m); }
    }
    @Override @Transactional
    public void deleteDictMajor(Long id) {
        // 先删除该专业与所有学科的关联
        dictMajorSubjectMapper.delete(new LambdaQueryWrapper<DictMajorSubject>().eq(DictMajorSubject::getMajorId, id));
        dictMajorMapper.deleteById(id);
    }

    // ── 专业-学科关联 ──
    @Override
    public List<Map<String, Object>> getMajorSubjects(Long majorId) {
        List<DictMajorSubject> links = dictMajorSubjectMapper.selectList(
            new LambdaQueryWrapper<DictMajorSubject>().eq(DictMajorSubject::getMajorId, majorId));
        if (links.isEmpty()) return List.of();
        List<Long> subjectIds = links.stream().map(DictMajorSubject::getSubjectId).toList();
        List<DictSubject> subjects = dictSubjectMapper.selectBatchIds(subjectIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DictSubject s : subjects) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId()); m.put("subjectName", s.getSubjectName());
            m.put("sortOrder", s.getSortOrder()); m.put("status", s.getStatus());
            result.add(m);
        }
        return result;
    }

    @Override @Transactional
    public void setMajorSubjects(Long majorId, List<Long> subjectIds) {
        // 先删后插，避免唯一约束冲突
        dictMajorSubjectMapper.delete(new LambdaQueryWrapper<DictMajorSubject>().eq(DictMajorSubject::getMajorId, majorId));
        if (subjectIds != null && !subjectIds.isEmpty()) {
            for (int i = 0; i < subjectIds.size(); i++) {
                DictMajorSubject link = new DictMajorSubject();
                link.setMajorId(majorId);
                link.setSubjectId(subjectIds.get(i));
                link.setSortOrder(i + 1);
                dictMajorSubjectMapper.insert(link);
            }
        }
    }

    // ==================== System Status ====================

    @Override
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> data = new HashMap<>();
        Runtime rt = Runtime.getRuntime();
        data.put("totalMemory", rt.totalMemory() / 1024 / 1024 + " MB");
        data.put("freeMemory", rt.freeMemory() / 1024 / 1024 + " MB");
        data.put("processors", rt.availableProcessors());
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("osName", System.getProperty("os.name"));

        data.put("userCount", userMapper != null ? userMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("studentCount", studentMapper != null ? studentMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("teacherCount", teacherMapper != null ? teacherMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("classCount", classesMapper != null ? classesMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("bbsPostCount", bbsPostMapper != null ? bbsPostMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("todaySignCount", signRecordMapper != null ? signRecordMapper.selectCount(
            new LambdaQueryWrapper<SignRecord>().likeRight(SignRecord::getSignDate, java.time.LocalDate.now().toString())) : 0);
        return data;
    }

    // ==================== Reset / Clear ====================

    @Override
    @Transactional
    public Map<String, Object> resetData(String target) {
        Map<String, Object> result = new HashMap<>();
        result.put("target", target);
        int count = 0;
        switch (target) {
            case "bbs" -> { count += deleteAll(bbsBookmarkMapper); count += deleteAll(bbsLikeMapper); count += deleteAll(bbsReplyMapper); count += deleteAll(bbsPostMapper); count += deleteAll(bbsMutedUserMapper); }
            case "signs" -> count += deleteAll(signRecordMapper);
            case "notifications" -> count += deleteAll(notificationMapper);
            case "credits" -> { count += deleteAll(creditTransactionMapper); if (studentMapper != null) studentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Student>().set(Student::getTotalCredits, 0)); }
            case "all" -> {
                for (var m : List.of(bbsBookmarkMapper, bbsLikeMapper, bbsReplyMapper, bbsPostMapper, bbsMutedUserMapper, signRecordMapper, notificationMapper, creditTransactionMapper)) count += deleteAll(m);
                if (studentMapper != null) studentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Student>().set(Student::getTotalCredits, 0));
            }
            default -> throw new IllegalArgumentException("未知的清理目标: " + target);
        }
        result.put("deletedCount", count);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> clearAllData() {
        int count = 0;
        for (var m : List.of(bbsBookmarkMapper, bbsLikeMapper, bbsReplyMapper, bbsPostMapper,
            bbsMutedUserMapper, signRecordMapper, notificationMapper, creditTransactionMapper,
            auditLogMapper)) if (m != null) count += deleteAll(m);
        if (studentMapper != null) studentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Student>().set(Student::getTotalCredits, 0));
        return Map.of("deletedCount", count);
    }

    private int deleteAll(com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper) {
        return mapper != null ? mapper.delete(new LambdaQueryWrapper<>()) : 0;
    }

    // ==================== Dynamic Params ====================

    @Override
    public List<Map<String, Object>> getDynamicParams(String category) {
        LambdaQueryWrapper<SystemSetting> w = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) w.eq(SystemSetting::getCategory, category);
        return settingMapper.selectList(w).stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId()); m.put("key", s.getSettingKey()); m.put("value", s.getSettingValue());
            m.put("category", s.getCategory()); m.put("description", com.school.teaching.common.EncodingUtils.fix(s.getDescription()));
            m.put("valueType", s.getValueType()); m.put("options", s.getOptions());
            m.put("defaultValue", s.getDefaultValue()); m.put("validationRule", s.getValidationRule());
            m.put("isEditable", s.getIsEditable());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getParamDetail(String key) {
        SystemSetting s = settingMapper.selectOne(
            new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, key));
        if (s == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("key", s.getSettingKey()); m.put("value", s.getSettingValue());
        m.put("category", s.getCategory()); m.put("description", com.school.teaching.common.EncodingUtils.fix(s.getDescription()));
        m.put("valueType", s.getValueType()); m.put("options", s.getOptions());
        return m;
    }

    @Override
    public void updateParam(String key, String value) {
        SystemSetting s = settingMapper.selectOne(
            new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, key));
        if (s != null) { s.setSettingValue(value); settingMapper.updateById(s); }
    }

    @Override
    public void updateParamsBatch(List<Map<String, String>> params) {
        Set<String> keys = params.stream().map(p -> p.get("key")).collect(Collectors.toSet());
        Map<String, SystemSetting> existingMap = keys.isEmpty() ? Map.of() :
            settingMapper.selectList(new LambdaQueryWrapper<SystemSetting>().in(SystemSetting::getSettingKey, keys))
                .stream().collect(Collectors.toMap(SystemSetting::getSettingKey, s -> s));
        for (var p : params) {
            SystemSetting s = existingMap.get(p.get("key"));
            if (s != null) { s.setSettingValue(p.get("value")); settingMapper.updateById(s); }
        }
    }

    // ==================== Announcement ====================

    @Override
    public int sendAnnouncement(String scope, Long targetId, String title, String content) {
        List<User> recipients;
        switch (scope != null ? scope : "all") {
            case "teachers" -> recipients = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getRoleId, List.of(1L, 2L, 8L)));
            case "students" -> {
                if (targetId != null && targetId > 0) {
                    List<Long> sids = studentMapper.selectList(
                        new LambdaQueryWrapper<Student>().eq(Student::getClassId, targetId))
                        .stream().map(Student::getUserId).toList();
                    recipients = sids.isEmpty() ? List.of() : userMapper.selectBatchIds(sids);
                } else {
                    recipients = userMapper.selectList(
                        new LambdaQueryWrapper<User>().eq(User::getRoleId, 4L));
                }
            }
            default -> recipients = userMapper.selectList(null);
        }
        for (User u : recipients) {
            notificationService.notify(u.getId(), "announcement", title, content, null);
        }
        return recipients.size();
    }

    // ==================== Backup / Import ====================

    @Override
    public byte[] exportBackup() {
        // Placeholder — actual implementation via Runtime.exec mysqldump
        return ("-- Teaching System Backup\n-- Generated at " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n").getBytes();
    }

    @Override
    @Transactional
    public int importBackup(byte[] sqlBytes) {
        return 0;
    }

    // ==================== Dashboard ====================

    @Override
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> data = new HashMap<>();
        data.put("studentCount", studentMapper != null ? studentMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("teacherCount", teacherMapper != null ? teacherMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("classCount", classesMapper != null ? classesMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("taskCount", taskMapper != null ? taskMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("bbsPostCount", bbsPostMapper != null ? bbsPostMapper.selectCount(new LambdaQueryWrapper<>()) : 0);
        data.put("todaySignCount", signRecordMapper != null ? signRecordMapper.selectCount(
            new LambdaQueryWrapper<SignRecord>().likeRight(SignRecord::getSignDate, java.time.LocalDate.now().toString())) : 0);
        data.put("todayPosts", bbsPostMapper != null ? bbsPostMapper.selectCount(
            new LambdaQueryWrapper<BbsPost>().ge(BbsPost::getCreateTime, java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))) : 0);
        return data;
    }

    @Override
    public List<Map<String, Object>> getSystemParams() {
        return getDynamicParams(null);
    }

    @Override
    public List<Map<String, Object>> getSystemParams(String category) {
        return getDynamicParams(category);
    }

    @Override
    public void updateSystemParams(Map<String, Object> params) {
        if (params == null) return;
        Map<String, SystemSetting> existingMap = settingMapper.selectList(
            new LambdaQueryWrapper<SystemSetting>().in(SystemSetting::getSettingKey, params.keySet()))
            .stream().collect(Collectors.toMap(SystemSetting::getSettingKey, s -> s));
        for (String key : params.keySet()) {
            if (!existingMap.containsKey(key)) throw new BusinessException(400, "配置项不存在: " + key);
        }
        params.forEach((k, v) -> {
            String sv = v != null ? v.toString() : "";
            SystemSetting setting = existingMap.get(k);
            setting.setSettingValue(sv);
            settingMapper.updateById(setting);
        });
    }

    // ==================== 学期管理 ====================

    @Override
    public List<Map<String, Object>> getTerms() {
        return schoolTermMapper.selectList(
            new LambdaQueryWrapper<SchoolTerm>().orderByDesc(SchoolTerm::getStartDate))
            .stream().map(t -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", t.getId()); m.put("name", t.getName());
                m.put("startDate", t.getStartDate()); m.put("endDate", t.getEndDate());
                m.put("isCurrent", t.getIsCurrent()); m.put("schoolId", t.getSchoolId());
                return m;
            }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> addTerm(Map<String, Object> body) {
        SchoolTerm t = new SchoolTerm();
        t.setName((String) body.get("name"));
        t.setStartDate(java.time.LocalDate.parse((String) body.get("startDate")));
        t.setEndDate(java.time.LocalDate.parse((String) body.get("endDate")));
        t.setIsCurrent(body.get("isCurrent") != null ? Integer.valueOf(body.get("isCurrent").toString()) : 0);
        t.setSchoolId(body.get("schoolId") != null ? Long.valueOf(body.get("schoolId").toString()) : 1L);
        // 若设为当前学期，取消其他当前标记
        if (t.getIsCurrent() == 1) {
            schoolTermMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SchoolTerm>()
                    .set(SchoolTerm::getIsCurrent, 0));
        }
        schoolTermMapper.insert(t);
        Map<String, Object> r = new HashMap<>();
        r.put("id", t.getId()); r.put("name", t.getName());
        r.put("startDate", t.getStartDate()); r.put("endDate", t.getEndDate());
        r.put("isCurrent", t.getIsCurrent());
        return r;
    }

    @Override
    public void updateTerm(Long id, Map<String, Object> body) {
        SchoolTerm t = schoolTermMapper.selectById(id);
        if (t == null) return;
        if (body.containsKey("name")) t.setName((String) body.get("name"));
        if (body.containsKey("startDate")) t.setStartDate(java.time.LocalDate.parse((String) body.get("startDate")));
        if (body.containsKey("endDate")) t.setEndDate(java.time.LocalDate.parse((String) body.get("endDate")));
        if (body.containsKey("isCurrent")) {
            int cur = Integer.parseInt(body.get("isCurrent").toString());
            t.setIsCurrent(cur);
            if (cur == 1) {
                schoolTermMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SchoolTerm>()
                        .set(SchoolTerm::getIsCurrent, 0).ne(SchoolTerm::getId, id));
            }
        }
        schoolTermMapper.updateById(t);
    }

    @Override
    public void deleteTerm(Long id) {
        schoolTermMapper.deleteById(id);
    }

    // ==================== 偏科提分班级权限 ====================

    @Override
    public String getRemedialClassIds() {
        SystemSetting s = settingMapper.selectOne(
            new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, "feature.remedial_class_ids"));
        return s != null && s.getSettingValue() != null ? s.getSettingValue() : "";
    }

    @Override
    public void setRemedialClassIds(String classIds) {
        SystemSetting s = settingMapper.selectOne(
            new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, "feature.remedial_class_ids"));
        if (s == null) {
            s = new SystemSetting();
            s.setSettingKey("feature.remedial_class_ids");
            s.setSettingValue(classIds);
            s.setCategory("feature");
            s.setValueType("string");
            s.setDescription("偏科提分模块可用班级ID，逗号分隔，空=全部可用");
            s.setIsEditable(1);
            settingMapper.insert(s);
        } else {
            s.setSettingValue(classIds);
            settingMapper.updateById(s);
        }
    }

    @Override
    public List<Map<String, Object>> getAllClassesWithRemedialStatus() {
        Set<Long> allowedIds = new HashSet<>();
        String idsStr = getRemedialClassIds();
        if (idsStr != null && !idsStr.isBlank()) {
            for (String part : idsStr.split(",")) {
                try { allowedIds.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) { log.debug("班级ID解析失败: {}", part); }
            }
        }
        boolean noRestriction = allowedIds.isEmpty();

        List<Classes> allClasses = classesMapper != null ? classesMapper.selectList(null) : List.of();
        return allClasses.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("className", c.getClassName());
            m.put("grade", c.getGrade());
            m.put("remedialEnabled", noRestriction || allowedIds.contains(c.getId()));
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isRemedialEnabledForCurrentUser() {
        // 1. 先检查总开关
        if (!getBooleanConfig("feature.remedial_enabled", true)) return false;

        // 2. 读班级白名单
        String idsStr = getRemedialClassIds();
        if (idsStr == null || idsStr.isBlank()) return true; // 空=全部可用

        Set<Long> allowedIds = new HashSet<>();
        for (String part : idsStr.split(",")) {
            try { allowedIds.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) {}
        }
        if (allowedIds.isEmpty()) return true;

        // 3. 按角色查班级
        Long userId = com.school.teaching.security.SecurityUtils.getCurrentUserId();
        if (userId == null) return false;
        String role = com.school.teaching.security.SecurityUtils.getCurrentRole();

        // 管理员/超管/巡视员始终可用（巡视员只读访问）
        if ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "INSPECTOR".equals(role)) return true;

        // 学生：查所在班级
        if ("STUDENT".equals(role) && studentMapper != null) {
            Student stu = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
            return stu != null && allowedIds.contains(stu.getClassId());
        }

        // 教师/班主任：查任教班级
        if (("TEACHER".equals(role) || "HEAD_TEACHER".equals(role)) && teacherService != null) {
            try {
                List<Long> teachingClassIds = teacherService.getTeachingClassIds(userId);
                return teachingClassIds.stream().anyMatch(allowedIds::contains);
            } catch (Exception e) { return false; }
        }

        return false;
    }

    // ==================== 系统 Logo ====================

    private static final String LOGO_KEY = "system.logo_url";

    @Override
    public String getLogoUrl() {
        SystemSetting s = settingMapper.selectOne(
            new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, LOGO_KEY));
        return (s != null && s.getSettingValue() != null && !s.getSettingValue().isBlank())
            ? s.getSettingValue().trim() : null;
    }

    @Override
    public void setLogoUrl(String url) {
        SystemSetting s = settingMapper.selectOne(
            new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, LOGO_KEY));
        if (s == null) {
            s = new SystemSetting();
            s.setSettingKey(LOGO_KEY);
            s.setSettingValue(url);
            s.setCategory("system");
            s.setValueType("string");
            s.setDescription("学校 Logo 图片路径");
            s.setIsEditable(1);
            settingMapper.insert(s);
        } else {
            s.setSettingValue(url);
            settingMapper.updateById(s);
        }
    }

    @Override
    public List<String> getVocationalSubjects() {
        return dictSubjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>()
                .like(DictSubject::getSubjectName, "[职高]")
                .eq(DictSubject::getStatus, 1))
            .stream().map(DictSubject::getSubjectName)
            .collect(java.util.stream.Collectors.toList());
    }
}
