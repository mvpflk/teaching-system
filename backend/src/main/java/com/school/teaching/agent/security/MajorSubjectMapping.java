package com.school.teaching.agent.security;

import java.util.Map;
import java.util.Set;

/**
 * 专业 → 可访问学科映射表。
 * 学生可访问文化课（语数英）+ 所属专业课。
 * 管理员可在后续版本中将此映射移至数据库配置。
 */
public final class MajorSubjectMapping {

    private MajorSubjectMapping() {}

    /** 所有学生默认拥有的文化课 */
    private static final Set<String> CULTURE_SUBJECTS = Set.of(
            "语文[职高]", "数学[职高]", "英语[职高]");

    /** 专业 → 专业课 */
    private static final Map<String, Set<String>> MAJOR_MAP = Map.of(
            "计算机应用", Set.of("信息技术应用基础", "网络应用基础", "办公应用基础"),
            "计算机网络技术", Set.of("信息技术应用基础", "网络应用基础"),
            "数字媒体技术", Set.of("信息技术应用基础", "办公应用基础"),
            "电子技术应用", Set.of("信息技术应用基础"),
            "会计事务", Set.of("办公应用基础"),
            "旅游服务与管理", Set.of(),
            "汽车运用与维修", Set.of(),
            "建筑工程施工", Set.of(),
            "学前教育", Set.of(),
            "护理", Set.of()
    );

    /** 获取某专业可访问的全部学科（文化课 + 专业课） */
    public static Set<String> getSubjects(String major) {
        if (major == null || major.isBlank()) return getDefaultSubjects();
        Set<String> majorSubjects = MAJOR_MAP.getOrDefault(major, Set.of());
        Set<String> all = new java.util.LinkedHashSet<>(CULTURE_SUBJECTS);
        all.addAll(majorSubjects);
        return all;
    }

    /** 无专业信息时的默认学科（仅文化课） */
    public static Set<String> getDefaultSubjects() {
        return CULTURE_SUBJECTS;
    }
}
