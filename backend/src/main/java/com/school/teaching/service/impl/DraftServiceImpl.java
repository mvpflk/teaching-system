package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.Draft;
import com.school.teaching.mapper.DraftMapper;
import com.school.teaching.service.DraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DraftServiceImpl implements DraftService {

    private final DraftMapper draftMapper;

    @Override
    public void save(Long studentId, Long taskId, String content) {
        Draft existing = draftMapper.selectOne(
                new LambdaQueryWrapper<Draft>().eq(Draft::getStudentId, studentId).eq(Draft::getTaskId, taskId));
        if (existing != null) {
            existing.setContent(content);
            draftMapper.updateById(existing);
        } else {
            Draft d = new Draft();
            d.setStudentId(studentId);
            d.setTaskId(taskId);
            d.setContent(content);
            draftMapper.insert(d);
        }
    }

    @Override
    public String load(Long studentId, Long taskId) {
        Draft d = draftMapper.selectOne(
                new LambdaQueryWrapper<Draft>().eq(Draft::getStudentId, studentId).eq(Draft::getTaskId, taskId));
        return d != null ? d.getContent() : null;
    }

    @Override
    public void delete(Long studentId, Long taskId) {
        draftMapper.delete(new LambdaQueryWrapper<Draft>()
                .eq(Draft::getStudentId, studentId).eq(Draft::getTaskId, taskId));
    }
}
