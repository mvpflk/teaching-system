package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.GroupMember;
import com.school.teaching.entity.LessonPrepGroup;
import com.school.teaching.entity.LessonPrepRecord;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.GroupMemberMapper;
import com.school.teaching.mapper.LessonPrepGroupMapper;
import com.school.teaching.mapper.LessonPrepRecordMapper;
import com.school.teaching.service.LessonPrepRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonPrepRecordServiceImpl implements LessonPrepRecordService {

    private final LessonPrepRecordMapper mapper;
    private final LessonPrepGroupMapper lessonPrepGroupMapper;
    private final GroupMemberMapper groupMemberMapper;

    private int countGroupMembers(Long groupId) {
        return groupMemberMapper.selectCount(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "LESSON_PREP")
                .eq(GroupMember::getGroupId, groupId)).intValue();
    }

    @Override
    public LessonPrepRecord getById(Long id) {
        LessonPrepRecord r = mapper.selectById(id);
        if (r == null) throw new BusinessException(404, "备课活动记录不存在");
        LessonPrepGroup g = lessonPrepGroupMapper.selectById(r.getLessonPrepGroupId());
        if (g != null) r.setGroupName(g.getName());
        int total = countGroupMembers(r.getLessonPrepGroupId());
        r.setTotalMembers(total);
        int pCount = r.getParticipantCount() != null ? r.getParticipantCount() : 0;
        r.setParticipationRate(total > 0 ? (double) pCount / total : 0.0);
        return r;
    }

    @Override
    @Transactional
    public LessonPrepRecord create(LessonPrepRecord record) {
        LessonPrepGroup group = lessonPrepGroupMapper.selectById(record.getLessonPrepGroupId());
        if (group == null) throw new BusinessException(400, "备课组不存在");
        int total = countGroupMembers(record.getLessonPrepGroupId());
        if (record.getParticipantCount() == null || record.getParticipantCount() == 0) {
            record.setParticipantCount(total);
        }
        mapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public LessonPrepRecord update(Long id, LessonPrepRecord data) {
        LessonPrepRecord existing = getById(id);
        if (data.getLessonPrepGroupId() != null) {
            LessonPrepGroup group = lessonPrepGroupMapper.selectById(data.getLessonPrepGroupId());
            if (group == null) throw new BusinessException(400, "备课组不存在");
            existing.setLessonPrepGroupId(data.getLessonPrepGroupId());
        }
        if (data.getTitle() != null) existing.setTitle(data.getTitle());
        if (data.getRecordDate() != null) existing.setRecordDate(data.getRecordDate());
        if (data.getParticipantIds() != null) existing.setParticipantIds(data.getParticipantIds());
        if (data.getParticipantCount() != null) existing.setParticipantCount(data.getParticipantCount());
        if (data.getContent() != null) existing.setContent(data.getContent());
        if (data.getOutputUrls() != null) existing.setOutputUrls(data.getOutputUrls());
        if (data.getRecordedBy() != null) existing.setRecordedBy(data.getRecordedBy());
        if (existing.getParticipantCount() == null || existing.getParticipantCount() == 0) {
            existing.setParticipantCount(countGroupMembers(existing.getLessonPrepGroupId()));
        }
        mapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }

    @Override
    public IPage<LessonPrepRecord> getPage(Long groupId, String startDate, String endDate, int page, int size) {
        LambdaQueryWrapper<LessonPrepRecord> q = new LambdaQueryWrapper<>();
        if (groupId != null) q.eq(LessonPrepRecord::getLessonPrepGroupId, groupId);
        if (startDate != null && !startDate.isEmpty()) q.ge(LessonPrepRecord::getRecordDate, LocalDate.parse(startDate));
        if (endDate != null && !endDate.isEmpty()) q.le(LessonPrepRecord::getRecordDate, LocalDate.parse(endDate));
        q.orderByDesc(LessonPrepRecord::getRecordDate);
        IPage<LessonPrepRecord> result = mapper.selectPage(new Page<>(page, size), q);
        for (LessonPrepRecord r : result.getRecords()) {
            LessonPrepGroup g = lessonPrepGroupMapper.selectById(r.getLessonPrepGroupId());
            if (g != null) r.setGroupName(g.getName());
            int total = countGroupMembers(r.getLessonPrepGroupId());
            r.setTotalMembers(total);
            int pCount = r.getParticipantCount() != null ? r.getParticipantCount() : 0;
            r.setParticipationRate(total > 0 ? (double) pCount / total : 0.0);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getGroupStats(Long groupId) {
        List<LessonPrepRecord> records = mapper.selectList(
            new LambdaQueryWrapper<LessonPrepRecord>()
                .eq(LessonPrepRecord::getLessonPrepGroupId, groupId));
        Map<String, Long> grouped = records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getRecordDate() != null ? r.getRecordDate().toString() : "未知",
                Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((date, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", count);
            result.add(item);
        });
        result.sort((a, b) -> b.get("date").toString().compareTo(a.get("date").toString()));
        return result;
    }
}
