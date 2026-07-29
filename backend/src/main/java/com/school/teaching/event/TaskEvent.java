package com.school.teaching.event;

import com.school.teaching.common.TaskCategory;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 统一任务事件基类 — 任务生命周期各阶段发布。
 *
 * 扩展方式：新增事件类型时，继承此类或直接使用本类的工厂方法。
 * 监听方式：@EventListener 注解即可，无需修改发布者代码。
 */
@Getter
public class TaskEvent extends ApplicationEvent {
    private final String eventType;
    private final Long taskId;
    private final TaskCategory category;
    private final Long studentId;
    private final Long teacherId;
    private final Map<String, Object> data;

    public TaskEvent(Object source, String eventType, Long taskId, TaskCategory category,
                     Long studentId, Long teacherId, Map<String, Object> data) {
        super(source);
        this.eventType = eventType;
        this.taskId = taskId;
        this.category = category;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.data = data;
    }

    // 事件类型常量
    public static final String TYPE_PUBLISHED = "task_published";
    public static final String TYPE_SUBMITTED = "task_submitted";
    public static final String TYPE_GRADED    = "task_graded";
    public static final String TYPE_CLOSED    = "task_closed";

    // 工厂方法
    public static TaskEvent published(Object src, Long taskId, TaskCategory cat,
                                       Long teacherId, Map<String, Object> data) {
        return new TaskEvent(src, TYPE_PUBLISHED, taskId, cat, null, teacherId, data);
    }

    public static TaskEvent submitted(Object src, Long taskId, TaskCategory cat,
                                       Long studentId, Long teacherId, Map<String, Object> data) {
        return new TaskEvent(src, TYPE_SUBMITTED, taskId, cat, studentId, teacherId, data);
    }

    public static TaskEvent graded(Object src, Long taskId, TaskCategory cat,
                                    Long studentId, Long teacherId, Map<String, Object> data) {
        return new TaskEvent(src, TYPE_GRADED, taskId, cat, studentId, teacherId, data);
    }

    public static TaskEvent closed(Object src, Long taskId, TaskCategory cat,
                                    Long teacherId, Map<String, Object> data) {
        return new TaskEvent(src, "task_closed", taskId, cat, null, teacherId, data);
    }

    public static TaskEvent competitionStarted(Object src, Long taskId, TaskCategory cat,
                                                Long studentId, Map<String, Object> data) {
        return new TaskEvent(src, "task_competition_started", taskId, cat, studentId, null, data);
    }

    public static TaskEvent submittedForReview(Object src, Long taskId, TaskCategory cat,
                                                Long teacherId, Map<String, Object> data) {
        return new TaskEvent(src, "task_submitted_for_review", taskId, cat, null, teacherId, data);
    }

    public static TaskEvent reviewApproved(Object src, Long taskId, TaskCategory cat,
                                            Long teacherId, Long reviewerId, Map<String, Object> data) {
        return new TaskEvent(src, "task_review_approved", taskId, cat, null, reviewerId, data);
    }

    public static TaskEvent reviewRejected(Object src, Long taskId, TaskCategory cat,
                                            Long teacherId, Long reviewerId, Map<String, Object> data) {
        return new TaskEvent(src, "task_review_rejected", taskId, cat, null, reviewerId, data);
    }
}
