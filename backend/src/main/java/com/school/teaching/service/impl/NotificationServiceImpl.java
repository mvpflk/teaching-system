package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.school.teaching.entity.Notification;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.User;
import com.school.teaching.mapper.NotificationMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.service.NotificationService;
import com.school.teaching.sse.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final SseConnectionManager sseConnectionManager;

    @Override
    public void notify(Long userId, String type, String title, String content, Long relatedId) {
        Notification n = buildNotification(userId, type, title, content, relatedId);
        notificationMapper.insert(n);
        sseConnectionManager.push(userId, n);
    }

    @Override
    public void notifyBatch(List<Long> userIds, String type, String title, String content, Long relatedId) {
        if (userIds == null || userIds.isEmpty()) return;
        List<Notification> list = userIds.stream()
            .map(uid -> buildNotification(uid, type, title, content, relatedId))
            .collect(Collectors.toList());
        Db.saveBatch(list);
        list.forEach(n -> sseConnectionManager.push(n.getUserId(), n));
    }

    @Override
    public void notifyClassStudents(Long classId, String type, String title, String content, Long relatedId) {
        List<Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        if (students.isEmpty()) return;
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        List<Notification> list = new ArrayList<>();
        for (Student s : students) {
            User u = userMap.get(s.getUserId());
            if (u != null) list.add(buildNotification(u.getId(), type, title, content, relatedId));
        }
        if (!list.isEmpty()) {
            Db.saveBatch(list);
            list.forEach(n -> sseConnectionManager.push(n.getUserId(), n));
        }
    }

    @Override
    public void notifyAllStudents(String type, String title, String content) {
        List<Student> students = studentMapper.selectList(null);
        if (students.isEmpty()) return;
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        List<Notification> list = new ArrayList<>();
        for (Student s : students) {
            User u = userMap.get(s.getUserId());
            if (u != null) list.add(buildNotification(u.getId(), type, title, content, null));
        }
        if (!list.isEmpty()) {
            Db.saveBatch(list);
            list.forEach(n -> sseConnectionManager.push(n.getUserId(), n));
        }
    }

    private Notification buildNotification(Long userId, String type, String title, String content, Long relatedId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        return n;
    }

    @Override
    public Map<String, Object> adminListNotifications(Long userId, int page, int pageSize) {
        Page<Notification> pg = new Page<>(page, pageSize);
        LambdaQueryWrapper<Notification> w = new LambdaQueryWrapper<>();
        w.eq(Notification::getUserId, userId).orderByDesc(Notification::getCreateTime);
        IPage<Notification> result = notificationMapper.selectPage(pg, w);
        List<Map<String, Object>> records = new ArrayList<>();
        for (Notification n : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId()); item.put("type", n.getType()); item.put("title", n.getTitle());
            item.put("content", n.getContent()); item.put("relatedId", n.getRelatedId());
            item.put("isRead", n.getIsRead()); item.put("createTime", n.getCreateTime());
            records.add(item);
        }
        Map<String, Object> r = new HashMap<>();
        r.put("records", records); r.put("total", result.getTotal());
        r.put("page", page); r.put("pageSize", pageSize);
        Long unread = notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        r.put("unreadCount", unread);
        return r;
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
            .eq(Notification::getId, id)
            .eq(Notification::getUserId, userId)
            .set(Notification::getIsRead, 1)
            .set(Notification::getReadTime, LocalDateTime.now()));
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
            .eq(Notification::getUserId, userId)
            .eq(Notification::getIsRead, 0)
            .set(Notification::getIsRead, 1)
            .set(Notification::getReadTime, LocalDateTime.now()));
    }

    /** 每月1日凌晨3:07清理180天前的已读通知 */
    @Scheduled(cron = "0 7 3 1 * *")
    public void cleanOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(180);
        long deleted = notificationMapper.delete(new LambdaQueryWrapper<Notification>()
            .eq(Notification::getIsRead, 1)
            .lt(Notification::getCreateTime, cutoff));
        if (deleted > 0) log.info("清理过期通知: {} 条 (早于 {})", deleted, cutoff.toLocalDate());
    }
}
