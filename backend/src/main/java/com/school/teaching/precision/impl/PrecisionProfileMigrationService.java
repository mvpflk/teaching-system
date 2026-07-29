package com.school.teaching.precision.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.PrecisionProfile;
import com.school.teaching.entity.Student;
import com.school.teaching.mapper.PrecisionProfileMapper;
import com.school.teaching.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PrecisionProfileMigrationService {

    @Autowired private PrecisionProfileMapper profileMapper;
    @Autowired private StudentMapper studentMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 迁移所有学生的 precision_profile JSON 到新表
     * 用于一次性数据迁移
     */
    @Transactional
    public int migrateAllProfiles() {
        log.info("开始迁移 precision_profile 数据...");
        int migrated = 0;

        // 分页查询所有有 profile 的学生
        int page = 0;
        int pageSize = 100;
        while (true) {
            List<Student> students = studentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Student>()
                    .isNotNull(Student::getPrecisionProfile)
                    .last("LIMIT " + pageSize + " OFFSET " + (page * pageSize)));

            if (students.isEmpty()) break;

            for (Student student : students) {
                try {
                    migrated += migrateStudentProfile(student.getId(), student.getPrecisionProfile());
                } catch (Exception e) {
                    log.error("迁移学生 profile 失败 sid={}: {}", student.getId(), e.getMessage());
                }
            }
            page++;
        }

        log.info("迁移完成，共迁移 {} 条记录", migrated);
        return migrated;
    }

    /**
     * 迁移单个学生的 profile
     */
    @Transactional
    public int migrateStudentProfile(Long studentId, String profileJson) {
        if (profileJson == null || profileJson.isEmpty()) return 0;

        try {
            Map<String, Object> profile = objectMapper.readValue(profileJson, new TypeReference<>() {});
            int migrated = 0;

            for (Map.Entry<String, Object> entry : profile.entrySet()) {
                String subject = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> subjectProfile = (Map<String, Object>) value;
                    migrateSubjectProfile(studentId, subject, subjectProfile);
                    migrated++;
                }
            }

            return migrated;
        } catch (Exception e) {
            log.error("解析 profile JSON 失败 sid={}: {}", studentId, e.getMessage());
            return 0;
        }
    }

    private void migrateSubjectProfile(Long studentId, String subject, Map<String, Object> subjectProfile) {
        // 检查是否已存在
        PrecisionProfile existing = profileMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrecisionProfile>()
                .eq(PrecisionProfile::getStudentId, studentId)
                .eq(PrecisionProfile::getSubject, subject));

        if (existing != null) {
            log.debug("profile 已存在，跳过: sid={}, subject={}", studentId, subject);
            return;
        }

        PrecisionProfile profile = new PrecisionProfile();
        profile.setStudentId(studentId);
        profile.setSubject(subject);
        profile.setDiagnoseScore(getIntValue(subjectProfile, "diagnoseScore"));
        profile.setEstimatedScore(getIntValue(subjectProfile, "estimatedScore"));
        profile.setStreakWeeks(getIntValue(subjectProfile, "streakWeeks"));
        profile.setLastOnlineTestScore(getIntValue(subjectProfile, "lastOnlineTestScore"));

        // 解析日期字段
        String lastDiagnoseAt = getStringValue(subjectProfile, "lastDiagnoseAt");
        if (lastDiagnoseAt != null && !lastDiagnoseAt.isEmpty()) {
            try {
                profile.setLastDiagnoseAt(java.time.LocalDate.parse(lastDiagnoseAt));
            } catch (Exception e) {
                log.warn("解析 lastDiagnoseAt 失败: {}", lastDiagnoseAt);
            }
        }

        String lastSubmitDate = getStringValue(subjectProfile, "lastSubmitDate");
        if (lastSubmitDate != null && !lastSubmitDate.isEmpty()) {
            try {
                profile.setLastSubmitDate(java.time.LocalDate.parse(lastSubmitDate));
            } catch (Exception e) {
                log.warn("解析 lastSubmitDate 失败: {}", lastSubmitDate);
            }
        }

        // 学习包相关字段
        Object lastPackQuestionIds = subjectProfile.get("lastPackQuestionIds");
        if (lastPackQuestionIds != null) {
            try {
                profile.setLastPackQuestionIds(objectMapper.writeValueAsString(lastPackQuestionIds));
            } catch (Exception e) {
                log.warn("序列化 lastPackQuestionIds 失败");
            }
        }
        profile.setLastPackWeekNo(getIntValue(subjectProfile, "lastPackWeekNo"));

        // 其他字段放入 extraData
        Map<String, Object> extraData = new LinkedHashMap<>(subjectProfile);
        extraData.remove("diagnoseScore");
        extraData.remove("estimatedScore");
        extraData.remove("streakWeeks");
        extraData.remove("lastOnlineTestScore");
        extraData.remove("lastDiagnoseAt");
        extraData.remove("lastSubmitDate");
        extraData.remove("lastPackQuestionIds");
        extraData.remove("lastPackWeekNo");

        if (!extraData.isEmpty()) {
            try {
                profile.setExtraData(objectMapper.writeValueAsString(extraData));
            } catch (Exception e) {
                log.warn("序列化 extraData 失败");
            }
        }

        profileMapper.insert(profile);
        log.debug("迁移 profile 成功: sid={}, subject={}", studentId, subject);
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
