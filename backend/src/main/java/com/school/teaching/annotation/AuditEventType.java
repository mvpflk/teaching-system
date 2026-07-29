package com.school.teaching.annotation;

/**
 * 审计事件类型枚举
 */
public enum AuditEventType {

    // ── 认证 ──
    USER_LOGIN("用户登录"),
    USER_LOGOUT("用户登出"),
    PASSWORD_CHANGE("密码修改"),

    // ── 考试 ──
    EXAM_CREATE("创建试卷"),
    EXAM_PUBLISH("发布考试"),
    EXAM_SUBMIT("提交答卷"),
    EXAM_GRADE("批改试卷"),
    EXAM_DELETE("删除试卷"),

    // ── 作业 ──
    HOMEWORK_ASSIGN("布置作业"),
    HOMEWORK_SUBMIT("提交作业"),
    HOMEWORK_GRADE("批改作业"),
    HOMEWORK_DELETE("删除作业"),

    // ── 积分 ──
    CREDIT_ADJUST("积分调整"),
    CREDIT_REDEEM("积分兑换"),

    // ── 系统 ──
    PARAM_UPDATE("修改系统参数"),
    DATA_EXPORT("数据导出"),
    DATA_RESET("数据重置"),

    // ── 用户 ──
    USER_CREATE("创建用户"),
    USER_UPDATE("编辑用户"),
    USER_DELETE("删除用户"),

    // ── 其他 ──
    BBS_POST("发布帖子"),
    SHOWCASE_RECOMMEND("推荐作品"),
    MORAL_PRAISE("德育表扬"),

    // ── AI ──
    AI_QUESTIONS("AI出题/审核"),

    // ── 统一任务 ──
    TASK_CREATE("创建任务"),
    TASK_UPDATE("编辑任务"),
    TASK_PUBLISH("发布任务"),
    TASK_SUBMIT("提交任务"),
    TASK_GRADE("评分任务"),
    TASK_CLOSE("关闭任务"),
    TASK_DELETE("删除任务"),

    CLASS_TYPE_CONFIG("班级类型配置"),

    OTHER("其他操作");

    private final String label;

    AuditEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
