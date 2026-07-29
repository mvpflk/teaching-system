package com.school.teaching.common;

import java.util.Set;

/**
 * 统一题型枚举 — 全系统单点定义，禁止硬编码字符串判断题型。
 *
 * 分类：
 *   客观题(可自动评分): SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE, FILL_IN
 *   主观题(需人工评分): SHORT_ANSWER, PROGRAMMING, FILE_UPLOAD, AUDIO_VIDEO, ESSAY, DRAG_SORT, MATCHING, CLOZE
 *   综合题(含子题): COMPOSITE
 *   特殊: CLASSROOM_MANUAL(课堂手动题目，同步到 question_bank 时使用)
 */
public enum QuestionTypeEnum {

    SINGLE_CHOICE   ("单选",         true,   false),
    MULTI_CHOICE    ("多选",         true,   false),
    TRUE_FALSE      ("判断",         true,   false),
    FILL_IN         ("填空",         true,   false),
    DRAG_SORT       ("拖拽排序",      true,   false),
    MATCHING        ("连线匹配",      true,   false),
    CLOZE           ("完形填空",      true,   false),
    SHORT_ANSWER    ("简答",         false,  false),
    PROGRAMMING     ("编程题",       false,  false),
    FILE_UPLOAD     ("文件上传",      false,  false),
    AUDIO_VIDEO     ("音视频作答",    false,  false),
    ESSAY           ("论述/作文",     false,  false),
    COMPOSITE       ("综合题",       false,  true),  // 含多个子题
    CLASSROOM_MANUAL("课堂手动题目",  false,  false); // 课堂手动创建，不在 question_bank 中

    private final String label;
    private final boolean objective;   // 可自动评分
    private final boolean composite;   // 是综合题

    QuestionTypeEnum(String label, boolean objective, boolean composite) {
        this.label = label;
        this.objective = objective;
        this.composite = composite;
    }

    public String getLabel()          { return label; }
    public boolean isObjective()      { return objective; }
    public boolean isComposite()      { return composite; }

    /** 所有客观题类型（DRAG_SORT/MATCHING/CLOZE 无法自动判分，已移至 SUBJECTIVE_TYPES） */
    public static final Set<QuestionTypeEnum> OBJECTIVE_TYPES = Set.of(
        SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE, FILL_IN);

    /** 所有主观题类型 */
    public static final Set<QuestionTypeEnum> SUBJECTIVE_TYPES = Set.of(
        SHORT_ANSWER, PROGRAMMING, FILE_UPLOAD, AUDIO_VIDEO, ESSAY, DRAG_SORT, MATCHING, CLOZE);

    /** 需要选项的题型 */
    public static final Set<QuestionTypeEnum> OPTION_TYPES = Set.of(
        SINGLE_CHOICE, MULTI_CHOICE);

    /** 固定1分的题型 */
    public static final Set<QuestionTypeEnum> FIXED_ONE_POINT_TYPES = Set.of(
        TRUE_FALSE, FILL_IN);

    /** 从字符串安全转换 */
    public static QuestionTypeEnum fromString(String s) {
        if (s == null) return null;
        try { return valueOf(s); }
        catch (IllegalArgumentException e) { return null; }
    }
}
