package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.LessonPrepWorkbenchService;
import com.school.teaching.service.LessonPrepRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonPrepWorkbenchServiceImpl implements LessonPrepWorkbenchService {

    private final LessonPrepGroupMapper lessonPrepGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final LessonPrepRecordMapper recordMapper;
    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final LessonPrepRecordService recordService;

    @Override
    public LessonPrepGroup getMyLessonPrepGroup(Long teacherId) {
        GroupMember gm = groupMemberMapper.selectOne(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "LESSON_PREP")
                .eq(GroupMember::getTeacherId, teacherId)
                .eq(GroupMember::getRole, "LEADER")
                .last("LIMIT 1"));
        if (gm == null) return null;
        return lessonPrepGroupMapper.selectById(gm.getGroupId());
    }

    @Override
    public IPage<LessonPrepRecord> getRecords(Long groupId, String startDate, String endDate, int page, int size) {
        return recordService.getPage(groupId, startDate, endDate, page, size);
    }

    @Override
    @Transactional
    public LessonPrepRecord createRecord(LessonPrepRecord record, Long teacherId, Long groupId) {
        record.setLessonPrepGroupId(groupId);
        record.setRecordedBy(teacherId);
        return recordService.create(record);
    }

    @Override
    @Transactional
    public LessonPrepRecord updateRecord(Long id, LessonPrepRecord data, Long teacherId) {
        LessonPrepRecord existing = recordService.getById(id);
        if (!teacherId.equals(existing.getRecordedBy()) && !isGroupLeader(teacherId, existing.getLessonPrepGroupId())) {
            throw new BusinessException(403, "仅创建人或组长可编辑");
        }
        return recordService.update(id, data);
    }

    @Override
    @Transactional
    public void deleteRecord(Long id, Long teacherId) {
        LessonPrepRecord existing = recordService.getById(id);
        if (!isGroupLeader(teacherId, existing.getLessonPrepGroupId())) {
            throw new BusinessException(403, "仅组长可删除");
        }
        recordService.delete(id);
    }

    @Override
    public List<Map<String, Object>> getMembers(Long groupId) {
        List<GroupMember> members = groupMemberMapper.selectList(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "LESSON_PREP")
                .eq(GroupMember::getGroupId, groupId));
        if (members.isEmpty()) return List.of();

        List<Long> teacherIds = members.stream().map(GroupMember::getTeacherId).collect(Collectors.toList());
        List<Teacher> teachers = teacherMapper.selectList(
            new LambdaQueryWrapper<Teacher>().in(Teacher::getId, teacherIds));
        Map<Long, Teacher> teacherMap = teachers.stream().collect(Collectors.toMap(Teacher::getId, t -> t));

        List<Long> userIds = teachers.stream().map(Teacher::getUserId).collect(Collectors.toList());
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().in(User::getId, userIds));
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, Long> activityCounts = recordMapper.selectList(
            new LambdaQueryWrapper<LessonPrepRecord>()
                .in(LessonPrepRecord::getRecordedBy, teacherIds))
            .stream()
            .collect(Collectors.groupingBy(LessonPrepRecord::getRecordedBy, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupMember gm : members) {
            Teacher t = teacherMap.get(gm.getTeacherId());
            if (t == null) continue;
            User u = userMap.get(t.getUserId());
            result.add(Map.of(
                "teacherId", t.getId(),
                "teacherName", u != null ? u.getRealName() : "未知",
                "teacherNumber", t.getTeacherNumber() != null ? t.getTeacherNumber() : "",
                "role", gm.getRole(),
                "activityCount", activityCounts.getOrDefault(t.getId(), 0L)
            ));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPendingReviews(Long groupId) {
        List<GroupMember> members = groupMemberMapper.selectList(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "LESSON_PREP")
                .eq(GroupMember::getGroupId, groupId));
        if (members.isEmpty()) return List.of();

        List<Long> teacherIds = members.stream().map(GroupMember::getTeacherId).collect(Collectors.toList());

        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .in(Task::getTeacherId, teacherIds)
                .eq(Task::getReviewStatus, "PENDING_GROUP"));

        if (tasks.isEmpty()) return List.of();

        List<Long> taskTeacherIds = tasks.stream().map(Task::getTeacherId).distinct().collect(Collectors.toList());
        List<Teacher> teachers = teacherMapper.selectList(
            new LambdaQueryWrapper<Teacher>().in(Teacher::getId, taskTeacherIds));
        Map<Long, Long> teacherUserIdMap = teachers.stream().collect(Collectors.toMap(Teacher::getId, Teacher::getUserId));
        List<Long> userIds = teachers.stream().map(Teacher::getUserId).collect(Collectors.toList());
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().in(User::getId, userIds));
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            Long userId = teacherUserIdMap.get(task.getTeacherId());
            User u = userId != null ? userMap.get(userId) : null;
            result.add(Map.of(
                "taskId", task.getId(),
                "title", task.getTitle() != null ? task.getTitle() : "",
                "taskType", task.getTaskType() != null ? task.getTaskType() : "",
                "submitTime", task.getCreatedAt() != null ? task.getCreatedAt().toString() : "",
                "submitterName", u != null ? u.getRealName() : "未知"
            ));
        }
        return result;
    }

    private boolean isGroupLeader(Long teacherId, Long groupId) {
        return groupMemberMapper.selectCount(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "LESSON_PREP")
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getTeacherId, teacherId)
                .eq(GroupMember::getRole, "LEADER")) > 0;
    }
}
