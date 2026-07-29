package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    /** 单条通知 */
    void notify(Long userId, String type, String title, String content, Long relatedId);

    /** 批量通知多个用户 */
    void notifyBatch(List<Long> userIds, String type, String title, String content, Long relatedId);

    /** 通知某班级所有学生 */
    void notifyClassStudents(Long classId, String type, String title, String content, Long relatedId);

    /** 通知所有学生 */
    void notifyAllStudents(String type, String title, String content);
    Map<String, Object> adminListNotifications(Long userId, int page, int pageSize);

    /** 标记单条通知已读 */
    void markAsRead(Long id, Long userId);

    /** 未读通知计数 */
    long getUnreadCount(Long userId);
    /** 标记全部通知已读 */
    void markAllAsRead(Long userId);
}
