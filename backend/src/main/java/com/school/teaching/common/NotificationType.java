package com.school.teaching.common;

/** 通知事件类型统一枚举 — 所有通知类型集中管理，禁止散落字符串常量 */
public final class NotificationType {

    private NotificationType() {}

    // ── 任务系统 ──
    public static final String TASK_PUBLISHED  = "TASK_PUBLISHED";
    public static final String TASK_SUBMITTED  = "TASK_SUBMITTED";
    public static final String TASK_GRADED     = "TASK_GRADED";
    public static final String TASK_CLOSED     = "TASK_CLOSED";
    public static final String TASK_DEADLINE   = "TASK_DEADLINE";
    /** 重测通知 */
    public static final String RETEST_REQUIRED = "RETEST_REQUIRED";

    // ── BBS ──
    public static final String BBS_REPLY       = "bbs_reply";
    public static final String BBS_LIKE        = "bbs_like";
    public static final String BBS_STICKY      = "bbs_sticky";
    public static final String BBS_HIGHLIGHT   = "bbs_highlight";
    public static final String BBS_MUTED       = "bbs_muted";
    public static final String BBS_MENTION     = "bbs_mention";

    // ── 积分 ──
    public static final String CREDIT_SIGN     = "credit_sign";
    public static final String CREDIT_ADJUST   = "credit_adjust";
    public static final String CREDIT_LEVEL_UP = "credit_level_up";

    // ── 展示墙 ──
    public static final String SHOWCASE_RECOMMEND = "SHOWCASE_RECOMMEND";
    public static final String SHOWCASE_NEW       = "SHOWCASE_NEW";
    public static final String PRACTICE_SHOWCASED = "PRACTICE_SHOWCASED";

    // ── 班级/学生 ──
    public static final String CLASS_CHANGED   = "class_changed";
    public static final String WELCOME         = "welcome";
    public static final String ANNOUNCEMENT    = "announcement";
    public static final String MORAL_PRAISE    = "MORAL_PRAISE";

    // ── 学业预警 ──
    public static final String ACADEMIC_ALERT = "ACADEMIC_ALERT";

    // ── 审核 ──
    public static final String TASK_SUBMITTED_FOR_REVIEW = "TASK_SUBMITTED_FOR_REVIEW";
    public static final String TASK_REVIEW_APPROVED = "TASK_REVIEW_APPROVED";
    public static final String TASK_REVIEW_REJECTED = "TASK_REVIEW_REJECTED";
}
